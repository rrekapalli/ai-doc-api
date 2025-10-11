package com.hidoc.api.ai.service;

import com.hidoc.api.ai.model.AIRequest;
import com.hidoc.api.ai.model.AIResponse;
import com.hidoc.api.service.RateLimitingService;
import com.hidoc.api.web.dto.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Central chat orchestrator for health-related conversations using Spring AI
 * Manages conversation flow, context, and routing to appropriate tools
 */
@Service
public class HealthChatOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(HealthChatOrchestrator.class);
    private static final int DEFAULT_MESSAGE_HISTORY_LIMIT = 100;

    private final ChatClient chatClient;
    private final ChatMemoryService chatMemoryService;
    private final PromptService promptService;
    private final RateLimitingService rateLimitingService;

    public HealthChatOrchestrator(
            ChatClient chatClient,
            ChatMemoryService chatMemoryService,
            PromptService promptService,
            RateLimitingService rateLimitingService) {
        this.chatClient = chatClient;
        this.chatMemoryService = chatMemoryService;
        this.promptService = promptService;
        this.rateLimitingService = rateLimitingService;
    }

    /**
     * Process a health-related message through the Spring AI orchestration system
     */
    public AIResponse processHealthMessage(AIRequest request) {
        logger.info("Processing health message for user: {}", request.getUserId());
        
        try {
            // Generate conversation and message IDs
            String conversationId = generateConversationId(request);
            String messageId = UUID.randomUUID().toString();
            
            // Prepare conversation context
            List<Message> conversationMessages = prepareConversationContext(request, conversationId);
            
            // Get system prompt for Spring AI orchestration
            String systemPrompt = promptService.getSpringAiMasterPrompt();
            
            // Create the chat client call with conversation context
            ChatClient.ChatClientRequestSpec chatRequest = chatClient.prompt()
                .system(systemPrompt)
                .messages(conversationMessages)
                .user(request.getMessage());
            
            // Execute the chat request
            String aiResponse = chatRequest.call().content();
            
            // Create and populate the response
            AIResponse response = createResponse(request, aiResponse, conversationId, messageId);
            
            // Update conversation memory
            updateConversationMemory(request, response, conversationId);
            
            logger.info("Successfully processed health message for user: {}", request.getUserId());
            return response;
            
        } catch (Exception e) {
            logger.error("Error processing health message for user: {}", request.getUserId(), e);
            return createErrorResponse(request, e);
        }
    }

    /**
     * Generate or retrieve conversation ID for the user session
     */
    private String generateConversationId(AIRequest request) {
        // For now, generate a new conversation ID for each request
        // TODO: Implement conversation persistence and retrieval logic
        return UUID.randomUUID().toString();
    }

    /**
     * Prepare conversation context from message history and chat memory
     */
    private List<Message> prepareConversationContext(AIRequest request, String conversationId) {
        List<Message> messages = new ArrayList<>();
        
        // Add message history if provided in the request
        if (request.getMessageHistory() != null && !request.getMessageHistory().isEmpty()) {
            List<ChatMessage> history = limitMessageHistory(request.getMessageHistory());
            
            for (ChatMessage chatMessage : history) {
                switch (chatMessage.getRole().toLowerCase()) {
                    case "user" -> messages.add(new UserMessage(chatMessage.getContent()));
                    case "assistant" -> messages.add(new SystemMessage("Previous assistant response: " + chatMessage.getContent()));
                    case "system" -> messages.add(new SystemMessage(chatMessage.getContent()));
                    default -> logger.warn("Unknown message role: {}", chatMessage.getRole());
                }
            }
        }
        
        // Retrieve additional context from chat memory service
        try {
            List<ChatMessage> memoryMessages = chatMemoryService.getConversationHistory(conversationId);
            for (ChatMessage chatMessage : memoryMessages) {
                switch (chatMessage.getRole().toLowerCase()) {
                    case "user" -> messages.add(new UserMessage(chatMessage.getContent()));
                    case "assistant" -> messages.add(new SystemMessage("Previous assistant response: " + chatMessage.getContent()));
                    case "system" -> messages.add(new SystemMessage(chatMessage.getContent()));
                    default -> logger.warn("Unknown message role in memory: {}", chatMessage.getRole());
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to retrieve conversation history for ID: {}", conversationId, e);
        }
        
        return messages;
    }

    /**
     * Limit message history to prevent context overflow
     */
    private List<ChatMessage> limitMessageHistory(List<ChatMessage> messageHistory) {
        if (messageHistory.size() <= DEFAULT_MESSAGE_HISTORY_LIMIT) {
            return messageHistory;
        }
        
        // Keep the most recent messages
        int startIndex = messageHistory.size() - DEFAULT_MESSAGE_HISTORY_LIMIT;
        return messageHistory.subList(startIndex, messageHistory.size());
    }

    /**
     * Create AIResponse from the chat result
     */
    private AIResponse createResponse(AIRequest request, String aiResponse, String conversationId, String messageId) {
        AIResponse response = new AIResponse();
        
        // Set basic response data
        response.setResponse(aiResponse);
        response.setDateTime(LocalDateTime.now());
        response.setMessageId(messageId);
        response.setConversationId(conversationId);
        response.setUserId(request.getUserId());
        
        // Calculate available requests after rate limiting
        response.setAvailableRequests(calculateAvailableRequests(request));
        
        // Set default values for Spring AI fields
        response.setClassification("PROCESSING"); // Will be updated by tools
        response.setInference("Initial processing by health chat orchestrator");
        response.setIsFollowUp(false);
        
        // Set model information
        response.setModel("spring-ai-orchestrator");
        
        return response;
    }

    /**
     * Calculate available requests for the user after rate limiting
     */
    private Integer calculateAvailableRequests(AIRequest request) {
        try {
            if (request.getEmail() != null && !request.getEmail().isBlank()) {
                RateLimitingService.UsageStats stats = rateLimitingService.getUsageStatsByEmail(request.getEmail());
                return (int) stats.remaining();
            } else if (request.getUserId() != null && !request.getUserId().isBlank()) {
                RateLimitingService.UsageStats stats = rateLimitingService.getUserUsageStats(request.getUserId());
                return (int) stats.remaining();
            }
        } catch (Exception e) {
            logger.warn("Failed to calculate available requests for user: {}", request.getUserId(), e);
        }
        
        return null; // Unknown remaining requests
    }

    /**
     * Update conversation memory with the new interaction
     */
    private void updateConversationMemory(AIRequest request, AIResponse response, String conversationId) {
        try {
            // Store the user message
            ChatMessage userMessage = new ChatMessage();
            userMessage.setRole("user");
            userMessage.setContent(request.getMessage());
            chatMemoryService.addMessage(conversationId, userMessage);
            
            // Store the assistant response
            ChatMessage assistantMessage = new ChatMessage();
            assistantMessage.setRole("assistant");
            assistantMessage.setContent(response.getResponse());
            chatMemoryService.addMessage(conversationId, assistantMessage);
            
        } catch (Exception e) {
            logger.error("Failed to update conversation memory for conversation: {}", conversationId, e);
        }
    }

    /**
     * Create error response for failed processing
     */
    private AIResponse createErrorResponse(AIRequest request, Exception error) {
        AIResponse response = new AIResponse();
        
        response.setResponse("I apologize, but I encountered an error while processing your health-related request. Please try again or contact support if the issue persists.");
        response.setDateTime(LocalDateTime.now());
        response.setMessageId(UUID.randomUUID().toString());
        response.setConversationId(UUID.randomUUID().toString());
        response.setUserId(request.getUserId());
        response.setClassification("ERROR");
        response.setInference("Error occurred during message processing: " + error.getMessage());
        response.setIsFollowUp(false);
        response.setModel("spring-ai-orchestrator");
        
        // Don't deduct from rate limit for error responses
        response.setAvailableRequests(calculateAvailableRequests(request));
        
        return response;
    }

    /**
     * Check if the orchestrator is ready to process requests
     */
    public boolean isReady() {
        try {
            return chatClient != null && 
                   chatMemoryService != null && 
                   promptService != null &&
                   promptService.getSpringAiMasterPrompt() != null;
        } catch (Exception e) {
            logger.error("Health chat orchestrator readiness check failed", e);
            return false;
        }
    }

    /**
     * Get orchestrator status information
     */
    public String getStatus() {
        if (isReady()) {
            return "HealthChatOrchestrator is ready and operational";
        } else {
            return "HealthChatOrchestrator is not ready - check configuration and dependencies";
        }
    }
}