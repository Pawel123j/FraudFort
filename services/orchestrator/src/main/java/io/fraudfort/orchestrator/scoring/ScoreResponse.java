package io.fraudfort.orchestrator.scoring;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

/**
 * Raw response from the scoring service. Snake_case wire fields are mapped
 * explicitly with {@code @JsonProperty} (Jackson-version agnostic). Mapped into
 * the richer {@link ScoreResult} before the rest of the app sees it.
 */
public record ScoreResponse(
        @JsonProperty("fraud_score") double fraudScore,
        @JsonProperty("model_version") String modelVersion,
        @JsonProperty("reasons") List<String> reasons,
        @JsonProperty("scored_at") Instant scoredAt
) {
}
