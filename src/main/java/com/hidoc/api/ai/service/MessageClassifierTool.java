package com.hidoc.api.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hidoc.api.ai.model.AIResponse;
import com.hidoc.api.web.dto.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Spring AI @Tool for classifying health-related messages and routing them to appropriate specialized tools
 * Uses the spring_ai_message_classifier_tool.txt prompt for enhanced routing logic
 */
@Service
public class MessageClassifierTool {

    private static final Logger logger = LoggerFactory.getLogger(MessageClassifierTool.class);

    private final PromptService promptService;
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    @Autowired
    public MessageClassifierTool(PromptService promptService, ChatModel chatModel) {
        this.promptService = promptService;
        this.chatModel = chatModel;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Classify a user message and determine appropriate routing
     * 
     * @param message The user message to classify
     * @param userId The user ID for context
     * @param email The user email for context
     * @param messageHistory The conversation history
     * @return Classification result with routing information
     */
    public String classifyMessage(String message, String userId, String email, String messageHistory) {
        logger.info("Classifying message for user: {}", userId);
        
        try {
            // Load the classifier prompt
            String classifierPrompt = promptService.getMessageClassifierPrompt();
            
            // Prepare template variables
            Map<String, Object> templateVars = new HashMap<>();
            templateVars.put("message", message);
            templateVars.put("messageHistory", messageHistory != null ? messageHistory : "No previous conversation history.");
            templateVars.put("userContext", formatUserContext(userId, email));
            
            // Create prompt template and generate prompt
            PromptTemplate promptTemplate = new PromptTemplate(classifierPrompt, templateVars);
            Prompt prompt = promptTemplate.create();
            
            // Get classification response from AI model
            String aiResponse = chatModel.call(prompt).getResult().getOutput().getContent();
            
            // Extract and return the JSON response
            String jsonResponse = extractJsonFromResponse(aiResponse);
            
            logger.info("Message classified for user: {}", userId);
            return jsonResponse;
            
        } catch (Exception e) {
            logger.error("Error classifying message for user: {}", userId, e);
            return createErrorJsonResponse(userId, e);
        }
    }



    /**
     * Format user context information for the prompt
     */
    private String formatUserContext(String userId, String email) {
        Map<String, Object> context = new HashMap<>();
        context.put("userId", userId);
        context.put("email", email);
        
        try {
            return objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to serialize user context", e);
            return String.format("{\"userId\": \"%s\", \"email\": \"%s\"}", userId, email);
        }
    }

    /**
     * Create error JSON response for failed classification
     */
    private String createErrorJsonResponse(String userId, Exception error) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("classification", "ERROR");
        errorResponse.put("response", "<p>I apologize, but I encountered an error while analyzing your message. " +
            "Please try rephrasing your request or contact support if the issue persists.</p>");
        errorResponse.put("inference", "Classification failed due to error: " + error.getMessage());
        errorResponse.put("data", null);
        errorResponse.put("isFollowUp", false);
        errorResponse.put("followUpDataRequired", null);
        errorResponse.put("routeTo", "MedicalQueryService");
        errorResponse.put("shouldDeductFromRateLimit", false);
        
        try {
            return objectMapper.writeValueAsString(errorResponse);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize error response", e);
            return "{\"classification\":\"ERROR\",\"response\":\"<p>System error occurred.</p>\",\"inference\":\"Failed to process request\",\"data\":null,\"isFollowUp\":false,\"followUpDataRequired\":null,\"routeTo\":\"MedicalQueryService\",\"shouldDeductFromRateLimit\":false}";
        }
    }

    /**
     * Extract JSON from AI response that might contain additional text
     */
    private String extractJsonFromResponse(String response) {
        // Look for JSON block markers
        if (response.contains("```json")) {
            int start = response.indexOf("```json") + 7;
            int end = response.indexOf("```", start);
            if (end > start) {
                return response.substring(start, end).trim();
            }
        }
        
        // Look for JSON object markers
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        
        // Return as-is if no markers found
        return response.trim();
    }

    /**
     * Request model for message classification
     */
    public static class ClassificationRequest {
        private String message;
        private String userId;
        private String email;
        private String messageHistory;

        // Default constructor
        public ClassificationRequest() {}

        // Constructor with required fields
        public ClassificationRequest(String message, String userId, String email, String messageHistory) {
            this.message = message;
            this.userId = userId;
            this.email = email;
            this.messageHistory = messageHistory;
        }

        // Getters and setters
        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getMessageHistory() {
            return messageHistory;
        }

        public void setMessageHistory(String messageHistory) {
            this.messageHistory = messageHistory;
        }

        @Override
        public String toString() {
            return "ClassificationRequest{" +
                    "message='" + message + '\'' +
                    ", userId='" + userId + '\'' +
                    ", email='" + email + '\'' +
                    ", messageHistory='" + messageHistory + '\'' +
                    '}';
        }
    }
}