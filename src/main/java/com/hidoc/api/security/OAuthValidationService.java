package com.hidoc.api.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OAuthValidationService {
    private static final Logger log = LoggerFactory.getLogger(OAuthValidationService.class);

    private static final String GOOGLE_JWKS = "https://www.googleapis.com/oauth2/v3/certs";
    private static final String MSFT_JWKS = "https://login.microsoftonline.com/common/discovery/v2.0/keys";

    private final Map<String, JWKSet> jwkCache = new ConcurrentHashMap<>();
    private final Map<String, Long> jwkCacheExpiry = new ConcurrentHashMap<>();
    private static final long JWK_CACHE_TTL_MS = 10 * 60 * 1000; // 10 minutes

    public boolean isTokenValid(String token) {
        try {
            return validateToken(token) != null;
        } catch (Exception e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public UserInfo validateToken(String token) throws ParseException, JOSEException {
        if (token == null || token.isBlank()) return null;
        SignedJWT jwt = SignedJWT.parse(token);
        String issuer = Optional.ofNullable(jwt.getJWTClaimsSet().getIssuer()).orElse("");
        OAuthProvider provider = detectProvider(issuer);

        if (provider == OAuthProvider.UNKNOWN) {
            log.warn("Unknown token issuer: {}", issuer);
            return null;
        }

        if (!verifySignature(jwt, provider)) {
            return null;
        }

        Date exp = jwt.getJWTClaimsSet().getExpirationTime();
        if (exp == null || exp.toInstant().isBefore(Instant.now())) {
            log.debug("Token expired or missing exp claim");
            return null;
        }

        String sub = jwt.getJWTClaimsSet().getSubject();
        String email = (String) jwt.getJWTClaimsSet().getClaim("email");
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) jwt.getJWTClaimsSet().getClaim("roles");
        Set<String> rolesSet = roles != null ? new HashSet<>(roles) : Set.of("USER");

        return new UserInfo(sub, email, provider, rolesSet, exp.toInstant());
    }

    public UserInfo extractUserInfo(String token) {
        try {
            return validateToken(token);
        } catch (Exception e) {
            log.debug("Failed to extract user info: {}", e.getMessage());
            return null;
        }
    }

    private OAuthProvider detectProvider(String issuer) {
        String iss = issuer.toLowerCase(Locale.ROOT);
        if (iss.contains("accounts.google.com")) return OAuthProvider.GOOGLE;
        if (iss.contains("login.microsoftonline.com")) return OAuthProvider.MICROSOFT;
        return OAuthProvider.UNKNOWN;
    }

    private boolean verifySignature(SignedJWT jwt, OAuthProvider provider) {
        try {
            String kid = jwt.getHeader().getKeyID();
            JWKSet jwkSet = getJwkSet(provider);
            if (jwkSet == null) return false;

            List<JWK> matches = new JWKSelector(new JWKMatcher.Builder()
                    .keyID(kid)
                    .keyUse(KeyUse.SIGNATURE)
                    .algorithm(JWSAlgorithm.RS256)
                    .build()).select(jwkSet);

            for (JWK jwk : matches) {
                if (jwk.toRSAKey() != null) {
                    RSASSAVerifier verifier = new RSASSAVerifier(jwk.toRSAKey());
                    if (jwt.verify(verifier)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Signature verification failed: {}", e.getMessage());
        }
        return false;
    }

    private JWKSet getJwkSet(OAuthProvider provider) throws MalformedURLException, ParseException, BadJOSEException, JOSEException {
        String url = switch (provider) {
            case GOOGLE -> GOOGLE_JWKS;
            case MICROSOFT -> MSFT_JWKS;
            default -> null;
        };
        if (url == null) return null;

        long now = System.currentTimeMillis();
        Long exp = jwkCacheExpiry.get(url);
        if (exp == null || now > exp) {
            // refresh
            log.debug("Refreshing JWKs from {}", url);
            try {
                JWKSet fresh = JWKSet.load(new URL(url));
                jwkCache.put(url, fresh);
                jwkCacheExpiry.put(url, now + JWK_CACHE_TTL_MS);
            } catch (Exception e) {
                log.warn("Failed to load JWK set from {}: {}", url, e.getMessage());
                return null;
            }
        }
        return jwkCache.get(url);
    }
}
