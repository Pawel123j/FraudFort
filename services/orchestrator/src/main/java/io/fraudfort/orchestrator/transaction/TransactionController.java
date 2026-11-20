package io.fraudfort.orchestrator.transaction;

import io.fraudfort.orchestrator.transaction.api.CreateTransactionRequest;
import io.fraudfort.orchestrator.transaction.api.TransactionResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> create(@Valid @RequestBody CreateTransactionRequest request,
                                                      Authentication authentication) {
        ScoredTransaction result = service.create(request, ClientPrincipal.from(authentication));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(result.transaction().getId())
                .toUri();
        return ResponseEntity.created(location)
                .body(TransactionResponse.from(result.transaction(), result.score()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getById(@PathVariable UUID id, Authentication authentication) {
        return service.findById(id, ClientPrincipal.from(authentication))
                .map(TransactionResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public Page<TransactionResponse> list(Pageable pageable, Authentication authentication) {
        return service.list(pageable, ClientPrincipal.from(authentication)).map(TransactionResponse::from);
    }
}
