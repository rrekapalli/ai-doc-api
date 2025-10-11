package com.hidoc.api.ai.model;

import com.hidoc.api.web.dto.ChatMessage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Model for managing chat sessions and conversation context.
 * Stores conversation history, session data, and metadata for Spring AI chat memory.
 */
public class ConversationContext {
    private String conversationId;
    private String userId;
    private List<ChatMessage> messageHistory;
    private Map<String, Object> sessionData;
    private LocalDateTime lastActivity;
    private LocalDateTime createdAt;
    private Integer messageCount;

    public ConversationContext() {
        this.messageHistory = new ArrayList<>();
        this.sessionData = new HashMap<>();
        this.createdAt = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
        this.messageCount = 0;
    }

    public ConversationContext(String conversationId, String userId) {
        this();
        this.conversationId = conversationId;
        this.userId = userId;
    }

    /**
     * Add a message to the conversation history
     */
    public void addMessage(ChatMessage message) {
        this.messageHistory.add(message);
        this.messageCount = this.messageHistory.size();
        this.lastActivity = LocalDateTime.now();
    }

    /**
     * Add multiple messages to the conversation history
     */
    public void addMessages(List<ChatMessage> messages) {
        this.messageHistory.addAll(messages);
        this.messageCount = this.messageHistory.size();
        this.lastActivity = LocalDateTime.now();
    }

    /**
     * Get the last N messages from the conversation history
     */
    public List<ChatMessage> getLastMessages(int limit) {
        if (messageHistory.size() <= limit) {
            return new ArrayList<>(messageHistory);
        }
        return new ArrayList<>(messageHistory.subList(messageHistory.size() - limit, messageHistory.size()));
    }

    /**
     * Clear old messages to maintain the configured limit
     */
    public void trimToLimit(int maxMessages) {
        if (messageHistory.size() > maxMessages) {
            int toRemove = messageHistory.size() - maxMessages;
            messageHistory.subList(0, toRemove).clear();
            this.messageCount = this.messageHistory.size();
        }
    }

    /**
     * Update session data
     */
    public void setSessionValue(String key, Object value) {
        this.sessionData.put(key, value);
        this.lastActivity = LocalDateTime.now();
    }

    /**
     * Get session data value
     */
    public Object getSessionValue(String key) {
        return this.sessionData.get(key);
    }

    /**
     * Check if conversation is expired based on last activity
     */
    public boolean isExpired(long maxInactiveMinutes) {
        return lastActivity.isBefore(LocalDateTime.now().minusMinutes(maxInactiveMinutes));
    }

    // Getters and setters
    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<ChatMessage> getMessageHistory() {
        return messageHistory;
    }

    public void setMessageHistory(List<ChatMessage> messageHistory) {
        this.messageHistory = messageHistory != null ? messageHistory : new ArrayList<>();
        this.messageCount = this.messageHistory.size();
    }

    public Map<String, Object> getSessionData() {
        return sessionData;
    }

    public void setSessionData(Map<String, Object> sessionData) {
        this.sessionData = sessionData != null ? sessionData : new HashMap<>();
    }

    public LocalDateTime getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(Integer messageCount) {
        this.messageCount = messageCount;
    }
}