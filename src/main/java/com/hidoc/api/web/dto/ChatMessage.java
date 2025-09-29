package com.hidoc.api.web.dto;

import jakarta.validation.constraints.NotBlank;

public class ChatMessage {
    @NotBlank
    private String role; // user | assistant | system
    @NotBlank
    private String content;

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}