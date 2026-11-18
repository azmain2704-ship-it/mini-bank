package com.minibank.transfer;

import java.net.URI;
import java.util.List;

import com.minibank.transfer.TransferDtos.TransferOutcome;
import com.minibank.transfer.TransferDtos.TransferRequest;
import com.minibank.transfer.TransferDtos.TransferResponse;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TransferController {

    private final TransferService service;

    public TransferController(TransferService service) {
        this.service = service;
    }

    /**
     * 201 = money moved. 422 = refused by a business rule (e.g. not enough money), still recorded in the history.
     * 200 with Idempotent-Replayed: true = same Idempotency-Key as before, nothing new happened.
     */
    @PostMapping("/api/transfers")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        String key = idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey.trim();
        TransferOutcome outcome = service.transfer(request, key);
        TransferResponse body = outcome.transfer();
        if (outcome.replayed()) {
            return ResponseEntity.ok().header("Idempotent-Replayed", "true").body(body);
        }
        if (body.status() == TransferStatus.REJECTED) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
        }
        return ResponseEntity.created(URI.create("/api/transfers/" + body.id())).body(body);
    }

    @GetMapping("/api/transfers")
    public List<TransferResponse> history(@RequestParam(required = false) Long accountId) {
        return service.history(accountId);
    }

    @GetMapping("/api/transfers/{id}")
    public TransferResponse get(@PathVariable long id) {
        return service.get(id);
    }

    @GetMapping("/api/accounts/{id}/transfers")
    public List<TransferResponse> accountHistory(@PathVariable long id) {
        return service.history(id);
    }
}
