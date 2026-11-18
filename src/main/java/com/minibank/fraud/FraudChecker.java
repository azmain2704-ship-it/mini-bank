package com.minibank.fraud;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.minibank.common.Money;
import com.minibank.transfer.Transfer;
import com.minibank.transfer.TransferRepository;

import org.springframework.stereotype.Component;

/**
 * Looks at a completed transfer and records a flag for anything suspicious. Runs inside the transfer's transaction,
 * so a transfer and its flags are saved together.
 */
@Component
public class FraudChecker {

    private final FraudProperties rules;
    private final FraudFlagRepository flags;
    private final TransferRepository transfers;

    public FraudChecker(FraudProperties rules, FraudFlagRepository flags, TransferRepository transfers) {
        this.rules = rules;
        this.flags = flags;
        this.transfers = transfers;
    }

    public List<FraudFlag> check(Transfer transfer) {
        List<FraudFlag> raised = new ArrayList<>();
        Instant now = transfer.getCreatedAt();

        long largePence = Money.toPence(rules.largeAmount());
        if (transfer.getAmountPence() >= largePence) {
            raised.add(new FraudFlag(transfer, FraudReason.LARGE_AMOUNT,
                    "£" + Money.toPounds(transfer.getAmountPence()) + " is at or above the £" + rules.largeAmount()
                            + " limit", now));
        }

        // Includes this transfer, which has already been saved
        long recent = transfers.countCompletedFromSince(transfer.getFromAccount().getId(), now.minus(rules.rapidWindow()));
        if (recent >= rules.rapidCount()) {
            raised.add(new FraudFlag(transfer, FraudReason.RAPID_TRANSFERS,
                    recent + " transfers out of account " + transfer.getFromAccount().getId() + " in the last "
                            + rules.rapidWindow().toSeconds() + " seconds", now));
        }

        flags.saveAll(raised);
        transfer.getFraudFlags().addAll(raised);
        return raised;
    }
}
