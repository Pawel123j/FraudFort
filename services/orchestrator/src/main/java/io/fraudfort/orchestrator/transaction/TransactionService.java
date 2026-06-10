package io.fraudfort.orchestrator.transaction;

import io.fraudfort.orchestrator.scoring.ScoreResult;
import io.fraudfort.orchestrator.scoring.ScoringClient;
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
    private final ScoringClient scoringClient;

    public TransactionService(TransactionRepository repository, ScoringClient scoringClient) {
        this.repository = repository;
        this.scoringClient = scoringClient;
    }

    /**
     * Creates and persists a transaction, scoring it at intake.
     *
     * <p>Deliberately not {@code @Transactional}: scoring is an external HTTP call,
     * and we don't want it holding a pooled DB connection. We score first (the call
     * falls back gracefully if the scorer is down), then persist via the repository's
     * own transaction. The score itself is not persisted yet — that arrives with the
     * decision record in stage 3.
     */
    public ScoredTransaction create(CreateTransactionRequest request) {
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
        ScoreResult score = scoringClient.score(transaction);
        Transaction saved = repository.save(transaction);
        return new ScoredTransaction(saved, score);
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
