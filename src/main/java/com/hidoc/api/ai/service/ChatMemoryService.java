package com.hidoc.api.ai.service;

import com.hidoc.api.ai.model.ConversationContext;
import com.hidoc.api.ai.util.ConversationUtils;
import com.hidoc.api.web.dto.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Implementation of Spring AI's ChatMemory interface for managing conversation history.
 * Provides in-memory storage with configurable message limits and conversation management.
 */
@Service
public class ChatMemoryService implements ChatMemory {

    private static final Logger logger = LoggerFactory.getLogger(ChatMemoryService.class);

    @Value("${app.chat.memory.max-messages:100}")
    private int maxMessages;

    @Value("${app.chat.memory.max-inactive-minutes:1440}") // 24 hours default
    private long maxInactiveMinutes;

    // In-memory storage for conversation contexts
    private final ConcurrentMap<String, ConversationContext> conversations = new ConcurrentHashMap<>();

    /**
     * Add messages to the conversation memory
     */
    @Override
    public void add(String conversationId, List<Message> messages) {
        logger.debug("Adding {} messages to conversation {}", messages.size(), conversationId);
        
        ConversationContext context = getOrCreateContext(conversationId);
        
        // Convert Spring AI Messages to ChatMessage DTOs
        List<ChatMessage> chatMessages = messages.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        
        context.addMessages(chatMessages);
        context.trimToLimit(maxMessages);
        
        logger.debug("Conversation {} now has {} messages", conversationId, context.getMessageCount());
    }

    /**
     * Get all messages from conversation memory
     */
    @Override
    public List<Message> get(String conversationId, int lastN) {
        logger.debug("Retrieving last {} messages from conversation {}", lastN, conversationId);
        
        ConversationContext context = conversations.get(conversationId);
        if (context == null) {
            logger.debug("No conversation found for ID: {}", conversationId);
            return new ArrayList<>();
        }

        // Clean up expired conversations
        if (context.isExpired(maxInactiveMinutes)) {
            logger.debug("Conversation {} has expired, removing from memory", conversationId);
            conversations.remove(conversationId);
            return new ArrayList<>();
        }

        List<ChatMessage> recentMessages = context.getLastMessages(lastN);
        List<Message> springAiMessages = recentMessages.stream()
                .map(this::convertToSpringAiMessage)
                .collect(Collectors.toList());
        
        logger.debug("Retrieved {} messages from conversation {}", springAiMessages.size(), conversationId);
        return springAiMessages;
    }

    /**
     * Clear all messages from a conversation
     */
    @Override
    public void clear(String conversationId) {
        logger.debug("Clearing conversation {}", conversationId);
        conversations.remove(conversationId);
    }

    /**
     * Get or create a conversation context
     */
    public ConversationContext getOrCreateContext(String conversationId) {
        return conversations.computeIfAbsent(conversationId, id -> {
            logger.debug("Creating new conversation context for ID: {}", id);
            String userId = ConversationUtils.extractUserIdFromConversationId(id);
            return new ConversationContext(id, userId);
        });
    }

    /**
     * Get conversation context by ID
     */
    public ConversationContext getContext(String conversationId) {
        ConversationContext context = conversations.get(conversationId);
        if (context != null && context.isExpired(maxInactiveMinutes)) {
            conversations.remove(conversationId);
            return null;
        }
        return context;
    }

    /**
     * Create a new conversation for a user
     */
    public String createConversation(String userId) {
        String conversationId = ConversationUtils.generateUserConversationId(userId);
        ConversationContext context = new ConversationContext(conversationId, userId);
        conversations.put(conversationId, context);
        logger.debug("Created new conversation {} for user {}", conversationId, userId);
        return conversationId;
    }

    /**
     * Add a single message to conversation
     */
    public void addMessage(String conversationId, ChatMessage message) {
        ConversationContext context = getOrCreateContext(conversationId);
        context.addMessage(message);
        context.trimToLimit(maxMessages);
        logger.debug("Added message to conversation {}", conversationId);
    }

    /**
     * Get conversation history as ChatMessage DTOs
     */
    public List<ChatMessage> getConversationHistory(String conversationId) {
        ConversationContext context = getContext(conversationId);
        return context != null ? new ArrayList<>(context.getMessageHistory()) : new ArrayList<>();
    }

    /**
     * Get conversation history with limit
     */
    public List<ChatMessage> getConversationHistory(String conversationId, int limit) {
        ConversationContext context = getContext(conversationId);
        return context != null ? context.getLastMessages(limit) : new ArrayList<>();
    }

    /**
     * Set session data for a conversation
     */
    public void setSessionData(String conversationId, String key, Object value) {
        ConversationContext context = getOrCreateContext(conversationId);
        context.setSessionValue(key, value);
    }

    /**
     * Get session data from a conversation
     */
    public Object getSessionData(String conversationId, String key) {
        ConversationContext context = getContext(conversationId);
        return context != null ? context.getSessionValue(key) : null;
    }

    /**
     * Clean up expired conversations
     */
    public void cleanupExpiredConversations() {
        logger.debug("Cleaning up expired conversations");
        conversations.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue().isExpired(maxInactiveMinutes);
            if (expired) {
                logger.debug("Removing expired conversation: {}", entry.getKey());
            }
            return expired;
        });
    }

    /**
     * Get active conversation count
     */
    public int getActiveConversationCount() {
        return conversations.size();
    }

    /**
     * Convert Spring AI Message to ChatMessage DTO
     */
    private ChatMessage convertToDto(Message message) {
        ChatMessage chatMessage = new ChatMessage();
        
        if (message instanceof UserMessage) {
            chatMessage.setRole("user");
        } else if (message instanceof AssistantMessage) {
            chatMessage.setRole("assistant");
        } else {
            chatMessage.setRole("system");
        }
        
        chatMessage.setContent(message.getContent());
        return chatMessage;
    }

    /**
     * Convert ChatMessage DTO to Spring AI Message
     */
    private Message convertToSpringAiMessage(ChatMessage chatMessage) {
        switch (chatMessage.getRole().toLowerCase()) {
            case "user":
                return new UserMessage(chatMessage.getContent());
            case "assistant":
                return new AssistantMessage(chatMessage.getContent());
            default:
                // For system messages or unknown roles, use UserMessage as fallback
                return new UserMessage(chatMessage.getContent());
        }
    }

    // Getters for configuration values (useful for testing and monitoring)
    public int getMaxMessages() {
        return maxMessages;
    }

    public long getMaxInactiveMinutes() {
        return maxInactiveMinutes;
    }
}