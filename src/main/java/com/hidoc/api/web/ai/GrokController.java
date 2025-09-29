package com.hidoc.api.web.ai;

import com.hidoc.api.ai.AIProvider;
import com.hidoc.api.ai.model.AIRequest;
import com.hidoc.api.ai.model.AIResponse;
import com.hidoc.api.ai.service.AIProxyService;
import com.hidoc.api.security.UserInfo;
import com.hidoc.api.web.dto.ChatPayload;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/grok")
@ConditionalOnProperty(prefix = "ai.providers.grok", name = "enabled", havingValue = "true")
public class GrokController {

    private final AIProxyService proxyService;

    public GrokController(AIProxyService proxyService) {
        this.proxyService = proxyService;
    }

    @PostMapping(value = "/chat", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AIResponse> chat(@Valid @RequestBody ChatPayload request, Authentication auth) {
        String userId;
        String email = null;
        try {
            userId = extractUserId(auth);
            email = extractEmail(auth);
        } catch (IllegalArgumentException ex) {
            userId = request.getUser_id();
        }
        if ((userId == null || userId.isBlank()) && (email == null || email.isBlank())) {
            throw new IllegalArgumentException("user_id or email is required");
        }
        AIRequest aiRequest = new AIRequest();
        aiRequest.setMessage(request.getMessage());
        aiRequest.setProvider(AIProvider.GROK);
        aiRequest.setUserId(userId);
        aiRequest.setEmail(email);
        java.util.Map<String, Object> meta = new java.util.HashMap<>();
        meta.put("conversation_history", request.getConversation_history());
        aiRequest.setMetadata(meta);
        AIResponse resp = proxyService.process(aiRequest);
        return ResponseEntity.ok(resp);
    }

    private String extractUserId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new IllegalArgumentException("Authenticated user is required");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof UserInfo u) {
            return u.getUserId();
        }
        // Fallbacks for tests or alternate security setups
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
            return ud.getUsername();
        }
        if (principal instanceof String s && !s.isBlank()) {
            return s;
        }
        throw new IllegalArgumentException("Invalid authentication principal");
    }

    private String extractEmail(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof UserInfo u) {
            return u.getEmail();
        }
        return null;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
