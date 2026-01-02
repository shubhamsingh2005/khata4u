package com.khata4u.backend.controller;

import com.khata4u.backend.model.Ledger;
import com.khata4u.backend.service.LedgerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ledgers")
public class LedgerController {

    private final LedgerService ledgerService;
    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','CUSTOMER')")
    public ResponseEntity<List<Ledger>> list() {
        return ResponseEntity.ok(ledgerService.findAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Ledger> create(@RequestBody Ledger ledger) {
        return ResponseEntity.ok(ledgerService.save(ledger));
    }
}

