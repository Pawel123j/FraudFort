package io.fraudfort.orchestrator.transaction;

import io.fraudfort.orchestrator.transaction.api.CreateTransactionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository repository;

    public TransactionService(TransactionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Transaction create(CreateTransactionRequest request) {
        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                request.cardId(),
                request.customerId(),
                request.amount(),
                request.currency(),
                request.merchantCategory(),
                request.country(),
                request.deviceId(),
                request.occurredAt()
        );
        return repository.save(transaction);
    }

    @Transactional(readOnly = true)
    public Optional<Transaction> findById(UUID id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public Page<Transaction> list(Pageable pageable) {
        return repository.findAll(pageable);
    }
}
