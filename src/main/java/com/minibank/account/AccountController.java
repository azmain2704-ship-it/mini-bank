package com.minibank.account;

import java.net.URI;
import java.util.List;

import com.minibank.account.AccountDtos.AccountResponse;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> open(@Valid @RequestBody AccountDtos.CreateAccountRequest request) {
        AccountResponse created = service.open(request);
        return ResponseEntity.created(URI.create("/api/accounts/" + created.id())).body(created);
    }

    @GetMapping
    public List<AccountResponse> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable long id) {
        return service.get(id);
    }

    @PostMapping("/{id}/deposits")
    public AccountResponse deposit(@PathVariable long id, @Valid @RequestBody AccountDtos.DepositRequest request) {
        return service.deposit(id, request);
    }
}
