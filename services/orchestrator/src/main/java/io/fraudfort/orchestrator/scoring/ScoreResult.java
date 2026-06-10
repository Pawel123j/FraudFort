package io.fraudfort.orchestrator.scoring;

import java.util.List;

/**
 * Outcome of a scoring attempt. Decoupled from the wire {@link ScoreResponse} so
 * the rest of the application has a stable type that also models the
 * "scoring unavailable" fallback explicitly, rather than via null/exceptions.
 */
public record ScoreResult(
        boolean available,
        Double fraudScore,
        String modelVersion,
        List<String> reasons
) {
    public static ScoreResult scored(double fraudScore, String modelVersion, List<String> reasons) {
        return new ScoreResult(true, fraudScore, modelVersion, reasons == null ? List.of() : List.copyOf(reasons));
    }

    public static ScoreResult unavailable() {
        return new ScoreResult(false, null, null, List.of());
    }

    /** Stable string surfaced in the API and (from stage 3) persisted on the decision. */
    public String status() {
        return available ? "SCORED" : "UNAVAILABLE";
    }
}
