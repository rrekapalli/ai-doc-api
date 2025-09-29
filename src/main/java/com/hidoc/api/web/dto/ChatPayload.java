package com.hidoc.api.web.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class ChatPayload {
    @NotBlank
    private String message;

    // Optional: can be provided by client; server will prefer authenticated user when available
    @JsonProperty("user_id")
    @JsonAlias({"userId"})
    private String user_id;

    @Valid
    @JsonProperty("conversation_history")
    @JsonAlias({"conversationHistory"})
    private List<ChatMessage> conversation_history;

    // Optional model hint from client (e.g., "default")
    @JsonAlias({"model"})
    private String model;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getUser_id() { return user_id; }
    public void setUser_id(String user_id) { this.user_id = user_id; }

    public List<ChatMessage> getConversation_history() { return conversation_history; }
    public void setConversation_history(List<ChatMessage> conversation_history) { this.conversation_history = conversation_history; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
}