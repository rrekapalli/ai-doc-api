package com.hidoc.api.web.ai;

import com.hidoc.api.ai.AIProvider;
import com.hidoc.api.ai.model.AIRequest;
import com.hidoc.api.ai.model.AIResponse;
import com.hidoc.api.ai.service.AIProxyService;
import com.hidoc.api.security.UserInfo;
import com.hidoc.api.web.dto.ChatRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/grok")
public class GrokController {

    private final AIProxyService proxyService;

    public GrokController(AIProxyService proxyService) {
        this.proxyService = proxyService;
    }

    @PostMapping(value = "/chat", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AIResponse> chat(@Valid @RequestBody ChatRequest request, Authentication auth) {
        String userId = extractUserId(auth);
        AIRequest aiRequest = new AIRequest();
        aiRequest.setMessage(request.getMessage());
        aiRequest.setProvider(AIProvider.GROK);
        aiRequest.setUserId(userId);
        aiRequest.setMetadata(request.getMetadata());
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

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
