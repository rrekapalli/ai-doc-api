package com.hidoc.api.web.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public class ChatRequest {
    @NotBlank
    private String message;
    private Map<String, Object> metadata;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
