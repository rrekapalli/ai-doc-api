package com.hidoc.api.ai.model;

import com.hidoc.api.domain.HealthDataEntry;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class AIResponse {
    // Existing fields
    // The AI-generated reply to display to the user
    private String response;
    // The model used for the response (optional)
    private String model;
    // Number of tokens used (optional)
    private Integer tokensUsed;
    // Unique ID for the request (optional)
    private String requestId;
    // Parsed health/medical data, if applicable (optional)
    private HealthDataEntry entry;
    // Array of matched entities or keywords (optional)
    private List<Map<String, Object>> matches;

    // New Spring AI fields
    // Classification of the message type (HEALTH_PARAM, MEDICATION_ENTRY, etc.)
    private String classification;
    // AI inference about the request
    private String inference;
    // Structured data for tables or charts
    private Map<String, Object> data;
    // Timestamp of the response
    private LocalDateTime dateTime;
    // Unique message identifier
    private String messageId;
    // Conversation identifier
    private String conversationId;
    // User identifier
    private String userId;
    // Number of requests available after this one
    private Integer availableRequests;
    // Whether this response requires a followup
    private Boolean isFollowUp;
    // Type of data required for followup
    private String followUpDataRequired;

    // Existing getters and setters
    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public Integer getTokensUsed() { return tokensUsed; }
    public void setTokensUsed(Integer tokensUsed) { this.tokensUsed = tokensUsed; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public HealthDataEntry getEntry() { return entry; }
    public void setEntry(HealthDataEntry entry) { this.entry = entry; }

    public List<Map<String, Object>> getMatches() { return matches; }
    public void setMatches(List<Map<String, Object>> matches) { this.matches = matches; }

    // New getters and setters
    public String getClassification() { return classification; }
    public void setClassification(String classification) { this.classification = classification; }

    public String getInference() { return inference; }
    public void setInference(String inference) { this.inference = inference; }

    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }

    public LocalDateTime getDateTime() { return dateTime; }
    public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime; }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Integer getAvailableRequests() { return availableRequests; }
    public void setAvailableRequests(Integer availableRequests) { this.availableRequests = availableRequests; }

    public Boolean getIsFollowUp() { return isFollowUp; }
    public void setIsFollowUp(Boolean isFollowUp) { this.isFollowUp = isFollowUp; }

    public String getFollowUpDataRequired() { return followUpDataRequired; }
    public void setFollowUpDataRequired(String followUpDataRequired) { this.followUpDataRequired = followUpDataRequired; }
}