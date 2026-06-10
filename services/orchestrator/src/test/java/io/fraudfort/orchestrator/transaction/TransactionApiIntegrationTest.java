package io.fraudfort.orchestrator.transaction;

import io.fraudfort.orchestrator.scoring.ScoreResult;
import io.fraudfort.orchestrator.scoring.ScoringClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TransactionApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransactionRepository repository;

    @MockitoBean
    private ScoringClient scoringClient;

    @BeforeEach
    void cleanDb() {
        repository.deleteAll();
    }

    @Test
    void postCreatesTransactionAndReturns201WithScore() throws Exception {
        when(scoringClient.score(any()))
                .thenReturn(ScoreResult.scored(0.73, "heuristic-0.1.0", List.of("high_amount")));

        String json = """
                {
                  "cardId": "4111111111111111",
                  "customerId": "c-001",
                  "amount": 129.90,
                  "currency": "EUR",
                  "merchantCategory": "5411",
                  "country": "PL",
                  "deviceId": "dev-abc",
                  "occurredAt": "2026-06-10T12:34:56Z"
                }
                """;

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.cardId").value("4111111111111111"))
                .andExpect(jsonPath("$.amount").value(129.90))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.fraudScore").value(0.73))
                .andExpect(jsonPath("$.scoringStatus").value("SCORED"))
                .andExpect(jsonPath("$.scoreReasons[0]").value("high_amount"));
    }

    @Test
    void postWithInvalidPayloadReturns400ProblemDetail() throws Exception {
        String badJson = """
                {
                  "cardId": "",
                  "customerId": "c-001",
                  "amount": -1.0,
                  "currency": "eur",
                  "merchantCategory": "12345",
                  "country": "POL",
                  "occurredAt": "2026-06-10T12:34:56Z"
                }
                """;

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void getByIdReturnsTransaction() throws Exception {
        Transaction saved = repository.save(new Transaction(
                UUID.randomUUID(), "card-1", "cust-1",
                new BigDecimal("42.00"), "USD", "5411", "US",
                "dev-1", Instant.parse("2026-06-10T08:00:00Z")
        ));

        mockMvc.perform(get("/api/v1/transactions/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.amount").value(42.00))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void getByUnknownIdReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void listReturnsPagedResults() throws Exception {
        for (int i = 0; i < 3; i++) {
            repository.save(new Transaction(
                    UUID.randomUUID(), "card-" + i, "cust-" + i,
                    new BigDecimal("10.00"), "EUR", "5411", "PL",
                    null, Instant.parse("2026-06-10T08:00:00Z")
            ));
        }

        mockMvc.perform(get("/api/v1/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.totalElements").value(3));
    }
}
