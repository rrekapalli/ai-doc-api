package com.hidoc.api.security;

import java.time.Instant;
import java.util.Set;

public class UserInfo {
    private final String userId;
    private final String email;
    private final OAuthProvider provider;
    private final Set<String> roles;
    private final Instant expiresAt;

    public UserInfo(String userId, String email, OAuthProvider provider, Set<String> roles, Instant expiresAt) {
        this.userId = userId;
        this.email = email;
        this.provider = provider;
        this.roles = roles;
        this.expiresAt = expiresAt;
    }

    public String getUserId() { return userId; }
    public String getEmail() { return email; }
    public OAuthProvider getProvider() { return provider; }
    public Set<String> getRoles() { return roles; }
    public Instant getExpiresAt() { return expiresAt; }
}
