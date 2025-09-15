package com.hidoc.api.ai.model;

import com.hidoc.api.ai.AIProvider;

import java.util.Map;

public class AIRequest {
    private String message;
    private AIProvider provider;
    private String userId;
    private Map<String, Object> metadata;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public AIProvider getProvider() { return provider; }
    public void setProvider(AIProvider provider) { this.provider = provider; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}