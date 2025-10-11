package com.hidoc.api.ai.service;

import com.hidoc.api.ai.model.ConversationContext;
import com.hidoc.api.ai.util.ConversationUtils;
import com.hidoc.api.web.dto.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for high-level conversation management operations.
 * Provides convenient methods for managing conversations and integrating with chat memory.
 */
@Service
public class ConversationManagementService {

    private static final Logger logger = LoggerFactory.getLogger(ConversationManagementService.class);

    private final ChatMemoryService chatMemoryService;

    public ConversationManagementService(ChatMemoryService chatMemoryService) {
        this.chatMemoryService = chatMemoryService;
    }

    /**
     * Start a new conversation for a user
     */
    public String startConversation(String userId) {
        logger.debug("Starting new conversation for user: {}", userId);
        String conversationId = chatMemoryService.createConversation(userId);
        logger.info("Created new conversation {} for user {}", conversationId, userId);
        return conversationId;
    }

    /**
     * Get or create a conversation ID for a user
     * If no active conversation exists, creates a new one
     */
    public String getOrCreateConversationId(String userId, String existingConversationId) {
        // If a conversation ID is provided and valid, use it
        if (existingConversationId != null && ConversationUtils.isValidConversationId(existingConversationId)) {
            ConversationContext context = chatMemoryService.getContext(existingConversationId);
            if (context != null && userId.equals(context.getUserId())) {
                logger.debug("Using existing conversation {} for user {}", existingConversationId, userId);
                return existingConversationId;
            }
        }

        // Create a new conversation
        return startConversation(userId);
    }

    /**
     * Add a user message to the conversation
     */
    public void addUserMessage(String conversationId, String messageContent) {
        ChatMessage message = new ChatMessage();
        message.setRole("user");
        message.setContent(messageContent);
        
        chatMemoryService.addMessage(conversationId, message);
        logger.debug("Added user message to conversation {}", conversationId);
    }

    /**
     * Add an assistant response to the conversation
     */
    public void addAssistantMessage(String conversationId, String responseContent) {
        ChatMessage message = new ChatMessage();
        message.setRole("assistant");
        message.setContent(responseContent);
        
        chatMemoryService.addMessage(conversationId, message);
        logger.debug("Added assistant message to conversation {}", conversationId);
    }

    /**
     * Get conversation history for context
     */
    public List<ChatMessage> getConversationHistory(String conversationId, int limit) {
        return chatMemoryService.getConversationHistory(conversationId, limit);
    }

    /**
     * Get full conversation history
     */
    public List<ChatMessage> getConversationHistory(String conversationId) {
        return chatMemoryService.getConversationHistory(conversationId);
    }

    /**
     * Check if a conversation exists and is active
     */
    public boolean isConversationActive(String conversationId) {
        return chatMemoryService.getContext(conversationId) != null;
    }

    /**
     * End a conversation (clear from memory)
     */
    public void endConversation(String conversationId) {
        logger.debug("Ending conversation: {}", conversationId);
        chatMemoryService.clear(conversationId);
        logger.info("Ended conversation: {}", conversationId);
    }

    /**
     * Store conversation-specific data (e.g., user preferences, context)
     */
    public void storeConversationData(String conversationId, String key, Object value) {
        chatMemoryService.setSessionData(conversationId, key, value);
        logger.debug("Stored session data for conversation {}: {} = {}", conversationId, key, value);
    }

    /**
     * Retrieve conversation-specific data
     */
    public Object getConversationData(String conversationId, String key) {
        return chatMemoryService.getSessionData(conversationId, key);
    }

    /**
     * Generate a new message ID for tracking
     */
    public String generateMessageId() {
        return ConversationUtils.generateMessageId();
    }

    /**
     * Validate conversation ownership
     */
    public boolean validateConversationOwnership(String conversationId, String userId) {
        ConversationContext context = chatMemoryService.getContext(conversationId);
        return context != null && userId.equals(context.getUserId());
    }

    /**
     * Get conversation statistics
     */
    public ConversationStats getConversationStats(String conversationId) {
        ConversationContext context = chatMemoryService.getContext(conversationId);
        if (context == null) {
            return null;
        }

        return new ConversationStats(
            conversationId,
            context.getUserId(),
            context.getMessageCount(),
            context.getCreatedAt(),
            context.getLastActivity()
        );
    }

    /**
     * Inner class for conversation statistics
     */
    public static class ConversationStats {
        private final String conversationId;
        private final String userId;
        private final int messageCount;
        private final java.time.LocalDateTime createdAt;
        private final java.time.LocalDateTime lastActivity;

        public ConversationStats(String conversationId, String userId, int messageCount, 
                               java.time.LocalDateTime createdAt, java.time.LocalDateTime lastActivity) {
            this.conversationId = conversationId;
            this.userId = userId;
            this.messageCount = messageCount;
            this.createdAt = createdAt;
            this.lastActivity = lastActivity;
        }

        // Getters
        public String getConversationId() { return conversationId; }
        public String getUserId() { return userId; }
        public int getMessageCount() { return messageCount; }
        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
        public java.time.LocalDateTime getLastActivity() { return lastActivity; }
    }
}