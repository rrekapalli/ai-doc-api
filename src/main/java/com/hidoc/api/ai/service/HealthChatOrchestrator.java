package com.hidoc.api.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final MessageClassifierTool messageClassifierTool;

    public HealthChatOrchestrator(
            ChatClient chatClient,
            ChatMemoryService chatMemoryService,
            PromptService promptService,
            RateLimitingService rateLimitingService,
            MessageClassifierTool messageClassifierTool) {
        this.chatClient = chatClient;
        this.chatMemoryService = chatMemoryService;
        this.promptService = promptService;
        this.rateLimitingService = rateLimitingService;
        this.messageClassifierTool = messageClassifierTool;
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
            
            // Step 1: Classify the message using MessageClassifierTool
            AIResponse classificationResult = classifyMessage(request, conversationId);
            
            // Step 2: Route to appropriate tool based on classification
            AIResponse response = routeToSpecializedTool(request, classificationResult, conversationId, messageId);
            
            // Step 3: Update conversation memory
            updateConversationMemory(request, response, conversationId);
            
            logger.info("Successfully processed health message for user: {} with classification: {}", 
                request.getUserId(), classificationResult.getClassification());
            return response;
            
        } catch (Exception e) {
            logger.error("Error processing health message for user: {}", request.getUserId(), e);
            return createErrorResponse(request, e);
        }
    }

    /**
     * Classify the incoming message using MessageClassifierTool directly
     */
    private AIResponse classifyMessage(AIRequest request, String conversationId) {
        logger.debug("Classifying message for user: {}", request.getUserId());
        
        try {
            // Format message history
            String messageHistory = formatMessageHistory(request.getMessageHistory());
            
            // Call the classification tool directly
            String classificationJson = messageClassifierTool.classifyMessage(
                request.getMessage(),
                request.getUserId(),
                request.getEmail(),
                messageHistory
            );
            
            // Parse the JSON response into AIResponse
            AIResponse response = parseClassificationJson(classificationJson, request);
            
            logger.debug("Message classified as: {} for user: {}", 
                response.getClassification(), request.getUserId());
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error classifying message for user: {}", request.getUserId(), e);
            return createClassificationErrorResponse(request, e);
        }
    }

    /**
     * Route to specialized tool based on classification result
     */
    private AIResponse routeToSpecializedTool(AIRequest request, AIResponse classificationResult, 
                                            String conversationId, String messageId) {
        Map<String, Object> data = classificationResult.getData();
        String routeTo = data != null ? (String) data.get("routeTo") : "MedicalQueryService";
        Boolean shouldDeductFromRateLimit = data != null ? (Boolean) data.get("shouldDeductFromRateLimit") : true;
        
        logger.debug("Routing to tool: {} for user: {}", routeTo, request.getUserId());
        
        // For now, use the classification result as the response
        // TODO: Implement actual routing to specialized tools in future tasks
        AIResponse response = classificationResult;
        
        // Update response with conversation and message IDs
        response.setConversationId(conversationId);
        response.setMessageId(messageId);
        
        // Handle rate limiting based on classification
        if (Boolean.TRUE.equals(shouldDeductFromRateLimit)) {
            // Calculate available requests after deduction
            response.setAvailableRequests(calculateAvailableRequests(request));
        } else {
            // Don't deduct from rate limit (e.g., for non-health queries)
            response.setAvailableRequests(calculateAvailableRequestsWithoutDeduction(request));
        }
        
        return response;
    }

    /**
     * Format message history for classification
     */
    private String formatMessageHistory(List<ChatMessage> messageHistory) {
        if (messageHistory == null || messageHistory.isEmpty()) {
            return "No previous conversation history.";
        }
        
        StringBuilder history = new StringBuilder();
        for (ChatMessage message : messageHistory) {
            history.append(String.format("%s: %s\n", 
                message.getRole().toUpperCase(), 
                message.getContent()));
        }
        
        return history.toString();
    }

    /**
     * Parse classification JSON response into AIResponse
     */
    private AIResponse parseClassificationJson(String classificationJson, AIRequest request) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = objectMapper.readValue(classificationJson, Map.class);
            
            AIResponse response = new AIResponse();
            response.setClassification((String) responseMap.get("classification"));
            response.setResponse((String) responseMap.get("response"));
            response.setInference((String) responseMap.get("inference"));
            response.setIsFollowUp((Boolean) responseMap.get("isFollowUp"));
            response.setFollowUpDataRequired((String) responseMap.get("followUpDataRequired"));
            response.setDateTime(LocalDateTime.now());
            response.setMessageId(UUID.randomUUID().toString());
            response.setUserId(request.getUserId());
            response.setModel("spring-ai-message-classifier");
            
            // Handle data field and routing information
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
            if (data == null) {
                data = new HashMap<>();
            }
            data.put("routeTo", responseMap.get("routeTo"));
            data.put("shouldDeductFromRateLimit", responseMap.get("shouldDeductFromRateLimit"));
            response.setData(data);
            
            return response;
            
        } catch (Exception e) {
            logger.error("Failed to parse classification JSON: {}", classificationJson, e);
            return createClassificationErrorResponse(request, e);
        }
    }

    /**
     * Create error response for classification failures
     */
    private AIResponse createClassificationErrorResponse(AIRequest request, Exception error) {
        AIResponse response = new AIResponse();
        response.setClassification("ERROR");
        response.setResponse("<p>I apologize, but I encountered an error while analyzing your message. " +
            "Please try rephrasing your request or contact support if the issue persists.</p>");
        response.setInference("Classification failed due to error: " + error.getMessage());
        response.setDateTime(LocalDateTime.now());
        response.setMessageId(UUID.randomUUID().toString());
        response.setUserId(request.getUserId());
        response.setModel("spring-ai-message-classifier");
        response.setIsFollowUp(false);
        
        Map<String, Object> data = new HashMap<>();
        data.put("routeTo", "MedicalQueryService");
        data.put("shouldDeductFromRateLimit", false);
        response.setData(data);
        
        return response;
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
     * Calculate available requests without deducting from rate limit (for non-health queries)
     */
    private Integer calculateAvailableRequestsWithoutDeduction(AIRequest request) {
        try {
            if (request.getEmail() != null && !request.getEmail().isBlank()) {
                RateLimitingService.UsageStats stats = rateLimitingService.getUsageStatsByEmail(request.getEmail());
                // Return current remaining + 1 since we're not deducting this request
                return (int) stats.remaining() + 1;
            } else if (request.getUserId() != null && !request.getUserId().isBlank()) {
                RateLimitingService.UsageStats stats = rateLimitingService.getUserUsageStats(request.getUserId());
                // Return current remaining + 1 since we're not deducting this request
                return (int) stats.remaining() + 1;
            }
        } catch (Exception e) {
            logger.warn("Failed to calculate available requests without deduction for user: {}", request.getUserId(), e);
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