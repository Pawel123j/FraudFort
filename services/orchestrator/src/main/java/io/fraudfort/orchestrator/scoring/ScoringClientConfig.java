package io.fraudfort.orchestrator.scoring;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

/**
 * Builds the {@link RestClient} used to call the scoring service, with the base URL
 * and connect/read timeouts from {@link ScoringProperties}. Tight timeouts matter:
 * a slow scorer must not stall transaction intake — {@link ScoringClient} falls back.
 *
 * <p>Backed by the JDK {@link HttpClient} (no extra HTTP-client dependency, and a
 * good fit with virtual threads, which the app enables).
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ScoringProperties.class)
class ScoringClientConfig {

    @Bean
    RestClient scoringRestClient(RestClient.Builder builder, ScoringProperties props) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(props.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(props.readTimeout());
        return builder
                .baseUrl(props.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
