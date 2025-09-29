package com.hidoc.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hidoc.api.ai.AIProvider;
import com.hidoc.api.ai.model.AIRequest;
import com.hidoc.api.ai.model.AIResponse;
import com.hidoc.api.ai.service.AIProxyService;
import com.hidoc.mcp.core.McpContext;
import com.hidoc.mcp.core.McpTool;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class AiChatTool implements McpTool {
    private final ObjectMapper mapper;
    private final AIProxyService proxy;

    public AiChatTool(ObjectMapper mapper, AIProxyService proxy) {
        this.mapper = mapper;
        this.proxy = proxy;
    }

    @Override
    public String name() { return "ai.chat"; }

    @Override
    public JsonNode schema() {
        ObjectNode s = mapper.createObjectNode();
        s.put("type", "object");
        ObjectNode props = s.putObject("properties");
        props.putObject("user_id").put("type", "string");
        props.putObject("message").put("type", "string");
        props.putObject("provider").put("type", "string");
        props.putObject("metadata").put("type", "object");
        s.putArray("required").add("user_id").add("message");
        return s;
    }

    @Override
    public CompletableFuture<JsonNode> call(JsonNode params, McpContext ctx) {
        return CompletableFuture.supplyAsync(() -> {
            String userId = val(params, "user_id", ctx.getUserId().orElse(null));
            String message = val(params, "message", null);
            String providerStr = val(params, "provider", "OPENAI");
            if (userId == null || message == null) {
                ObjectNode err = mapper.createObjectNode();
                err.put("error", "user_id and message are required");
                return err;
            }
            AIProvider provider;
            try {
                provider = AIProvider.valueOf(providerStr.toUpperCase(Locale.ROOT));
            } catch (Exception e) {
                provider = AIProvider.OPENAI;
            }
            AIRequest req = new AIRequest();
            req.setUserId(userId);
            req.setMessage(message);
            req.setProvider(provider);
            if (params.has("metadata") && params.get("metadata").isObject()) {
                Map<String, Object> meta = mapper.convertValue(params.get("metadata"), Map.class);
                req.setMetadata(meta);
            } else {
                req.setMetadata(new HashMap<>());
            }
            AIResponse resp = proxy.process(req);
            ObjectNode out = mapper.createObjectNode();
            out.put("text", resp.getResponse());
            out.put("model", resp.getModel());
            out.put("tokensUsed", resp.getTokensUsed() == null ? 0 : resp.getTokensUsed());
            return out;
        });
    }

    private String val(JsonNode node, String field, String def) {
        JsonNode n = node.get(field);
        return n == null || n.isNull() ? def : n.asText();
    }
}
