package io.fraudfort.orchestrator.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** FF-01: decoder configuration is fail-closed and tokens must match issuer and audience. */
class SecurityConfigTest {

    private static final String ISSUER = "https://issuer.fraudfort.test/";

    private static Jwt token(String issuer, List<String> audience) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("t").header("alg", "RS256")
                .issuer(issuer).audience(audience).subject("client-a")
                .issuedAt(now.minusSeconds(5)).expiresAt(now.plusSeconds(300)).build();
    }

    @Test
    void decoderRefusesToStartWithoutIssuerOrAudience() {
        SecurityConfig config = new SecurityConfig();
        assertThatThrownBy(() -> config.jwtDecoder("", "", "fraudfort-api"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("ISSUER");
        assertThatThrownBy(() -> config.jwtDecoder(ISSUER, "https://issuer.fraudfort.test/jwks", ""))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("AUDIENCE");
    }

    @Test
    void validatorAcceptsOnlyMatchingIssuerAndAudience() {
        var validator = SecurityConfig.tokenValidator(ISSUER, "fraudfort-api");
        assertThat(validator.validate(token(ISSUER, List.of("fraudfort-api"))).hasErrors()).isFalse();
        assertThat(validator.validate(token(ISSUER, List.of("other-api"))).hasErrors()).isTrue();
        assertThat(validator.validate(token("https://evil.test/", List.of("fraudfort-api"))).hasErrors()).isTrue();
    }
}
