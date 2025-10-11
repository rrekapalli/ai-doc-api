package com.hidoc.api.ai.model;

import com.hidoc.api.ai.AIProvider;
import com.hidoc.api.web.dto.ChatMessage;

import java.util.List;
import java.util.Map;

public class AIRequest {
    private String message;
    private AIProvider provider;
    private String userId;
    private String email; // optional: prefer for usage tracking
    private Map<String, Object> metadata;
    private List<ChatMessage> messageHistory; // New field for conversation context

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public AIProvider getProvider() { return provider; }
    public void setProvider(AIProvider provider) { this.provider = provider; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public List<ChatMessage> getMessageHistory() { return messageHistory; }
    public void setMessageHistory(List<ChatMessage> messageHistory) { this.messageHistory = messageHistory; }
}