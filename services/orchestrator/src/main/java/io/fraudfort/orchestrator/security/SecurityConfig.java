package io.fraudfort.orchestrator.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

import java.util.Collection;

/**
 * FF-01: the transaction API is an OAuth2 resource server.
 *
 * <ul>
 *   <li>Every {@code /api/**} request needs a valid JWT from the configured issuer
 *       ({@code FRAUDFORT_JWT_ISSUER_URI}) with the configured audience
 *       ({@code FRAUDFORT_JWT_AUDIENCE}). There are no defaults and no shared secret: the
 *       service does not start without an issuer (fail-closed).</li>
 *   <li>Function-level authorization by scope: {@code transactions:write} for POST,
 *       {@code transactions:read} for GET. Object-level authorization (a client sees only
 *       its own transactions unless it has {@code transactions:analyst}) is enforced in
 *       {@code TransactionService}.</li>
 *   <li>Actuator: health/info are public probes; metrics/prometheus need
 *       {@code SCOPE_metrics:read}. Anything else is denied.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // stateless bearer-token API, no cookies
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/transactions")
                        .hasAuthority("SCOPE_transactions:write")
                        .requestMatchers(HttpMethod.GET, "/api/v1/transactions", "/api/v1/transactions/*")
                        .hasAuthority("SCOPE_transactions:read")
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/metrics", "/actuator/metrics/**", "/actuator/prometheus")
                        .hasAuthority("SCOPE_metrics:read")
                        .requestMatchers("/error").permitAll()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> { }));
        return http.build();
    }

    /**
     * Decoder bound to the configured issuer and audience. Keys come from the issuer's
     * JWKS (resolved lazily on first use), never from a value in this repository.
     */
    @Bean
    JwtDecoder jwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}") String issuer,
                          @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}") String jwkSetUri,
                          @Value("${fraudfort.security.jwt.audience:}") String audience) {
        if (!StringUtils.hasText(issuer)) {
            throw new IllegalStateException("FRAUDFORT_JWT_ISSUER_URI is required");
        }
        if (!StringUtils.hasText(audience)) {
            throw new IllegalStateException("FRAUDFORT_JWT_AUDIENCE is required");
        }
        NimbusJwtDecoder decoder = StringUtils.hasText(jwkSetUri)
                ? NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build()
                : NimbusJwtDecoder.withIssuerLocation(issuer).build();
        decoder.setJwtValidator(tokenValidator(issuer, audience));
        return decoder;
    }

    /** Issuer, expiry/not-before (default validators) and audience must all match. */
    static OAuth2TokenValidator<Jwt> tokenValidator(String issuer, String audience) {
        OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<Collection<String>>(
                "aud", aud -> aud != null && aud.contains(audience));
        return new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer), audienceValidator);
    }
}
