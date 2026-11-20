package io.fraudfort.orchestrator.transaction;

import io.fraudfort.orchestrator.scoring.ScoreResult;
import io.fraudfort.orchestrator.scoring.ScoringClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static io.fraudfort.orchestrator.transaction.TransactionApiIntegrationTest.client;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FF-01 / FF-02 regressions: authentication, scopes, data ownership, actuator exposure. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TransactionSecurityIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransactionRepository repository;

    @MockitoBean
    private ScoringClient scoringClient;

    private Transaction ownedByB;

    @BeforeEach
    void seed() {
        repository.deleteAll();
        repository.save(tx("client-a"));
        ownedByB = repository.save(tx("client-b"));
        when(scoringClient.score(any())).thenReturn(ScoreResult.scored(0.1, "test", List.of()));
    }

    private static Transaction tx(String owner) {
        return new Transaction(UUID.randomUUID(), owner, "card-1", "cust-1", new BigDecimal("1.00"),
                "EUR", "5411", "PL", null, Instant.parse("2026-06-10T08:00:00Z"));
    }

    @Test
    void anonymousRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/v1/transactions")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/transactions/" + ownedByB.getId())).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/transactions").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingScopeIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/transactions").with(client("client-a", "transactions:write")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/transactions").with(client("client-a", "transactions:read"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void clientCannotReadAnotherClientsTransaction() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/" + ownedByB.getId()).with(client("client-a", "transactions:read")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/transactions").with(client("client-a", "transactions:read")))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(org.hamcrest.Matchers.not(ownedByB.getId().toString())));
    }

    @Test
    void analystReadsAcrossClients() throws Exception {
        mockMvc.perform(get("/api/v1/transactions").with(client("soc-1", "transactions:read", "transactions:analyst")))
                .andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(get("/api/v1/transactions/" + ownedByB.getId())
                        .with(client("soc-1", "transactions:read", "transactions:analyst")))
                .andExpect(status().isOk());
    }

    @Test
    void createdTransactionIsOwnedByTheCaller() throws Exception {
        String body = """
                {"cardId":"card-9","customerId":"cust-9","amount":10.00,"currency":"EUR",
                 "merchantCategory":"5411","country":"PL","occurredAt":"2026-06-10T08:00:00Z"}""";
        mockMvc.perform(post("/api/v1/transactions").with(client("client-c", "transactions:write"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        assert repository.findAll().stream().anyMatch(t -> "client-c".equals(t.getClientId()));
    }

    @Test
    void metricsRequireScopeButHealthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/metrics").with(client("client-a", "transactions:read")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/actuator/metrics").with(client("prometheus", "metrics:read")))
                .andExpect(status().isOk());
    }

    @Test
    void unknownEndpointsAreDenied() throws Exception {
        mockMvc.perform(get("/actuator/env").with(client("client-a", "transactions:read")))
                .andExpect(status().isForbidden());
    }
}
