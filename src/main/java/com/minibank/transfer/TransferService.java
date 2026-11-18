package com.minibank.transfer;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.minibank.account.Account;
import com.minibank.account.AccountRepository;
import com.minibank.common.IdempotencyKeyReusedException;
import com.minibank.common.InvalidRequestException;
import com.minibank.common.Money;
import com.minibank.common.NotFoundException;
import com.minibank.fraud.FraudChecker;
import com.minibank.transfer.TransferDtos.TransferOutcome;
import com.minibank.transfer.TransferDtos.TransferRequest;
import com.minibank.transfer.TransferDtos.TransferResponse;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {

    private final AccountRepository accounts;
    private final TransferRepository transfers;
    private final FraudChecker fraudChecker;
    private final Clock clock;

    public TransferService(AccountRepository accounts, TransferRepository transfers, FraudChecker fraudChecker, Clock clock) {
        this.accounts = accounts;
        this.transfers = transfers;
        this.fraudChecker = fraudChecker;
        this.clock = clock;
    }

    /**
     * Moves money between two accounts. Everything happens in one database transaction: both balances change and the
     * history row is written, or none of it happens.
     */
    @Transactional
    public TransferOutcome transfer(TransferRequest request, String idempotencyKey) {
        // Same key as an earlier request? Return what happened then instead of moving the money twice.
        if (idempotencyKey != null) {
            Optional<Transfer> earlier = transfers.findByIdempotencyKey(idempotencyKey);
            if (earlier.isPresent()) {
                Transfer t = earlier.get();
                // Same key but different details means the client has a bug (or reused a key). Replaying the old
                // transfer would hide that, so refuse instead and leave the money alone.
                boolean sameRequest = t.getFromAccount().getId().equals(request.fromAccountId())
                        && t.getToAccount().getId().equals(request.toAccountId())
                        && t.getAmountPence() == Money.toPence(request.amount());
                if (!sameRequest) {
                    throw new IdempotencyKeyReusedException(
                            "Idempotency-Key '" + idempotencyKey + "' was already used for a different transfer");
                }
                return new TransferOutcome(TransferResponse.from(t), true);
            }
        }

        long fromId = request.fromAccountId();
        long toId = request.toAccountId();
        if (fromId == toId) {
            throw new InvalidRequestException("Can't transfer money to the same account");
        }
        long amount = Money.toPence(request.amount());
        if (amount <= 0) {
            throw new InvalidRequestException("Amount must be more than zero");
        }

        // Lock both rows, always lowest id first. If two transfers go A->B and B->A at the same moment and each
        // locked its own "from" account first, they could wait for each other forever (a deadlock).
        Account first = lock(Math.min(fromId, toId));
        Account second = lock(Math.max(fromId, toId));
        Account from = first.getId() == fromId ? first : second;
        Account to = from == first ? second : first;

        Instant now = clock.instant();
        if (!from.canAfford(amount)) {
            Transfer refused = transfers.save(Transfer.rejected(from, to, amount,
                    "Insufficient funds: balance is " + Money.toPounds(from.getBalancePence()), idempotencyKey, now));
            return new TransferOutcome(TransferResponse.from(refused), false);
        }

        from.withdraw(amount);
        to.deposit(amount);
        Transfer done = transfers.save(Transfer.completed(from, to, amount, idempotencyKey, now));
        fraudChecker.check(done);
        return new TransferOutcome(TransferResponse.from(done), false);
    }

    @Transactional(readOnly = true)
    public List<TransferResponse> history(Long accountId) {
        if (accountId == null) {
            return transfers.findAllByOrderByCreatedAtDescIdDesc().stream().map(TransferResponse::from).toList();
        }
        if (!accounts.existsById(accountId)) {
            throw new NotFoundException("No account with id " + accountId);
        }
        return transfers.findHistoryForAccount(accountId).stream().map(TransferResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TransferResponse get(long id) {
        return transfers.findById(id).map(TransferResponse::from)
                .orElseThrow(() -> new NotFoundException("No transfer with id " + id));
    }

    private Account lock(long id) {
        return accounts.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("No account with id " + id));
    }
}
