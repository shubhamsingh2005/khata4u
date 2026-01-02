package com.khata4u.backend.repository;

import com.khata4u.backend.model.Ledger;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerRepository extends JpaRepository<Ledger, Long> {
}

