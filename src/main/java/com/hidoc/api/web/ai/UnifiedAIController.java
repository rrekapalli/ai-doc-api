package com.hidoc.api.web.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hidoc.api.ai.model.AIResponse;
import com.hidoc.api.security.UserInfo;
import com.hidoc.api.web.dto.ChatPayload;
import com.hidoc.mcp.core.McpContext;
import com.hidoc.mcp.core.ToolRegistry;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * Unified AI chat endpoint for clients calling /api/ai/chat.
 *
 * Now routes requests through the internal MCP server implementation
 * by invoking the orchestrator tool: ai.route_with_master_prompt.
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/ai")
public class UnifiedAIController {

    private final ObjectMapper mapper;
    private final ToolRegistry toolRegistry;

    public UnifiedAIController(ObjectMapper mapper, ToolRegistry toolRegistry) {
        this.mapper = mapper;
        this.toolRegistry = toolRegistry;
    }

    @PostMapping(value = "/chat", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AIResponse> chat(@Valid @RequestBody ChatPayload request, Authentication auth) {
        String userId;
        try {
            userId = extractUserId(auth);
        } catch (IllegalArgumentException ex) {
            userId = request.getUser_id();
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("user_id is required");
        }

        ObjectNode params = mapper.createObjectNode();
        params.put("user_id", userId);
        params.put("message", request.getMessage());
        if (request.getConversation_history() != null) {
            params.set("conversation_history", mapper.valueToTree(request.getConversation_history()));
        }
        params.put("include_rag", true);

        McpContext ctx = new McpContext("rest:unified", Instant.now(), mapper, userId);
        JsonNode result = toolRegistry.call("ai.route_with_master_prompt", params, ctx).join();

        if (result != null && result.has("error")) {
            throw new IllegalArgumentException(result.path("error").asText("Unknown MCP error"));
        }

        AIResponse out = new AIResponse();
        out.setResponse(result == null ? "" : result.path("text").asText(""));
        if (result != null && result.has("model")) {
            out.setModel(result.path("model").asText(null));
        }
        if (result != null && result.has("tokensUsed")) {
            out.setTokensUsed(result.path("tokensUsed").isInt() ? result.path("tokensUsed").asInt() : null);
        }
        return ResponseEntity.ok(out);
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
