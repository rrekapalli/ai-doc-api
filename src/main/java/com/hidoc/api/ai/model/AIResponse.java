package com.hidoc.api.ai.model;

import com.hidoc.api.domain.HealthDataEntry;

import java.util.List;
import java.util.Map;

public class AIResponse {
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
}