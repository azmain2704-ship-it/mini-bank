package com.minibank.account;

import java.time.Clock;
import java.util.List;

import com.minibank.account.AccountDtos.AccountResponse;
import com.minibank.common.Money;
import com.minibank.common.NotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accounts;
    private final Clock clock;

    public AccountService(AccountRepository accounts, Clock clock) {
        this.accounts = accounts;
        this.clock = clock;
    }

    @Transactional
    public AccountResponse open(AccountDtos.CreateAccountRequest request) {
        Account account = new Account(request.ownerName().trim(), Money.toPence(request.openingBalance()), clock.instant());
        return AccountResponse.from(accounts.save(account));
    }

    @Transactional
    public AccountResponse deposit(long accountId, AccountDtos.DepositRequest request) {
        Account account = accounts.findByIdForUpdate(accountId)
                .orElseThrow(() -> new NotFoundException("No account with id " + accountId));
        account.deposit(Money.toPence(request.amount()));
        return AccountResponse.from(account);
    }

    @Transactional(readOnly = true)
    public AccountResponse get(long accountId) {
        return accounts.findById(accountId).map(AccountResponse::from)
                .orElseThrow(() -> new NotFoundException("No account with id " + accountId));
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> list() {
        return accounts.findAll().stream().map(AccountResponse::from).toList();
    }
}
