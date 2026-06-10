package io.fraudfort.orchestrator.scoring;

import io.fraudfort.orchestrator.transaction.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Pure unit test of the HTTP layer — no Spring context, no Docker. Uses
 * {@link MockRestServiceServer} bound to the same {@link RestClient.Builder} the
 * client is built from, so we assert on the exact request and drive responses.
 */
class ScoringClientTest {

    private MockRestServiceServer server;
    private ScoringClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://scoring.test");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ScoringClient(builder.build());
    }

    @Test
    void returnsScoredResultOnSuccess() {
        server.expect(once(), requestTo("http://scoring.test/score"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.card_id").value("card-9"))
                .andExpect(jsonPath("$.merchant_category").value("7995"))
                .andRespond(withSuccess("""
                        {
                          "fraud_score": 0.82,
                          "model_version": "heuristic-0.1.0",
                          "reasons": ["high_amount", "off_hours_utc"],
                          "scored_at": "2026-06-10T02:30:00Z"
                        }
                        """, MediaType.APPLICATION_JSON));

        ScoreResult result = client.score(sampleTransaction());

        assertThat(result.available()).isTrue();
        assertThat(result.status()).isEqualTo("SCORED");
        assertThat(result.fraudScore()).isEqualTo(0.82);
        assertThat(result.modelVersion()).isEqualTo("heuristic-0.1.0");
        assertThat(result.reasons()).containsExactly("high_amount", "off_hours_utc");
        server.verify();
    }

    @Test
    void fallsBackToUnavailableOnServerError() {
        server.expect(requestTo("http://scoring.test/score"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        ScoreResult result = client.score(sampleTransaction());

        assertThat(result.available()).isFalse();
        assertThat(result.status()).isEqualTo("UNAVAILABLE");
        assertThat(result.fraudScore()).isNull();
        assertThat(result.reasons()).isEmpty();
        server.verify();
    }

    private Transaction sampleTransaction() {
        return new Transaction(
                UUID.randomUUID(), "card-9", "cust-9",
                new BigDecimal("7500.00"), "EUR", "7995", "PL",
                null, Instant.parse("2026-06-10T02:30:00Z"));
    }
}
