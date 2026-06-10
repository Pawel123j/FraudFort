package io.fraudfort.orchestrator.scoring;

import io.fraudfort.orchestrator.transaction.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Calls the scoring service over HTTP.
 *
 * <p>Never throws on scoring failure: if the service is unreachable, times out, or
 * returns an error, this returns {@link ScoreResult#unavailable()}. Transaction
 * intake must not be coupled to scoring availability — the decision engine
 * (stage 3) decides what to do when a score is missing.
 */
@Component
public class ScoringClient {

    private static final Logger log = LoggerFactory.getLogger(ScoringClient.class);

    private final RestClient restClient;

    public ScoringClient(@Qualifier("scoringRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public ScoreResult score(Transaction tx) {
        try {
            ScoreResponse response = restClient.post()
                    .uri("/score")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ScoreRequest.from(tx))
                    .retrieve()
                    .body(ScoreResponse.class);

            if (response == null) {
                log.warn("Scoring service returned an empty body for tx {}", tx.getId());
                return ScoreResult.unavailable();
            }
            return ScoreResult.scored(response.fraudScore(), response.modelVersion(), response.reasons());
        } catch (RestClientException ex) {
            log.warn("Scoring service unavailable for tx {} — falling back. Cause: {}", tx.getId(), ex.getMessage());
            return ScoreResult.unavailable();
        }
    }
}
