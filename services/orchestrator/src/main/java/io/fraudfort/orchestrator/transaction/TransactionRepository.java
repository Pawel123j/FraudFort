package io.fraudfort.orchestrator.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByIdAndClientId(UUID id, String clientId);

    Page<Transaction> findAllByClientId(String clientId, Pageable pageable);
}
