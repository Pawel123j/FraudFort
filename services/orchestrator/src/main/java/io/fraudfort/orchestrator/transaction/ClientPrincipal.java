package io.fraudfort.orchestrator.transaction;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * The authenticated API client derived from a validated JWT.
 *
 * @param clientId stable client identifier ({@code sub}); owner of the data it submits
 * @param analyst  holds {@code SCOPE_transactions:analyst} and may read across clients
 */
public record ClientPrincipal(String clientId, boolean analyst) {

    public static final String ANALYST_AUTHORITY = "SCOPE_transactions:analyst";

    public ClientPrincipal {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("JWT subject is required");
        }
    }

    public static ClientPrincipal from(Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("Expected a JWT-authenticated request");
        }
        boolean analyst = authentication.getAuthorities().stream()
                .anyMatch(a -> ANALYST_AUTHORITY.equals(a.getAuthority()));
        return new ClientPrincipal(jwt.getSubject(), analyst);
    }
}
