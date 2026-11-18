package com.minibank.fraud;

import java.time.Instant;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FraudController {

    public record FraudFlagResponse(Long id, Long transferId, FraudReason reason, String detail, Instant createdAt) {
    }

    private final FraudFlagRepository flags;

    public FraudController(FraudFlagRepository flags) {
        this.flags = flags;
    }

    /** Everything that's been flagged, newest first: the list a fraud team would work through. */
    @GetMapping("/api/fraud-flags")
    @Transactional(readOnly = true)
    public List<FraudFlagResponse> list() {
        return flags.findAllByOrderByCreatedAtDescIdDesc().stream()
                .map(f -> new FraudFlagResponse(f.getId(), f.getTransfer().getId(), f.getReason(), f.getDetail(),
                        f.getCreatedAt()))
                .toList();
    }
}
