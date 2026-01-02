package com.khata4u.backend.service;

import com.khata4u.backend.model.Ledger;
import com.khata4u.backend.repository.LedgerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LedgerService {
    private final LedgerRepository repo;

    public LedgerService(LedgerRepository repo) {
        this.repo = repo;
    }

    public List<Ledger> findAll() {
        return repo.findAll();
    }

    public Ledger save(Ledger ledger) {
        return repo.save(ledger);
    }
}

