package io.fraudfort.orchestrator.scoring;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Configuration for reaching the scoring service. Bound from {@code fraudfort.scoring.*}.
 */
@ConfigurationProperties(prefix = "fraudfort.scoring")
public record ScoringProperties(
        @DefaultValue("http://localhost:8000") String baseUrl,
        @DefaultValue("200ms") Duration connectTimeout,
        @DefaultValue("1s") Duration readTimeout
) {
}
