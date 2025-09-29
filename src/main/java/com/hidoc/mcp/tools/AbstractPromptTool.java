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
import com.hidoc.mcp.util.PromptLoader;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractPromptTool implements McpTool {
    protected final ObjectMapper mapper;
    protected final AIProxyService proxy;
    protected final PromptLoader loader;

    protected AbstractPromptTool(ObjectMapper mapper, AIProxyService proxy, PromptLoader loader) {
        this.mapper = mapper;
        this.proxy = proxy;
        this.loader = loader;
    }

    protected abstract String promptFile();

    @Override
    public JsonNode schema() {
        ObjectNode s = mapper.createObjectNode();
        s.put("type", "object");
        ObjectNode props = s.putObject("properties");
        props.putObject("user_id").put("type", "string");
        props.putObject("message").put("type", "string");
        props.putObject("context").put("type", "string");
        props.putObject("provider").put("type", "string");
        s.putArray("required").add("user_id").add("message");
        return s;
    }

    @Override
    public CompletableFuture<JsonNode> call(JsonNode params, McpContext ctx) {
        return CompletableFuture.supplyAsync(() -> {
            String userId = params.path("user_id").asText(ctx.getUserId().orElse(""));
            String message = params.path("message").asText("");
            String providerStr = params.path("provider").asText("OPENAI");
            String context = params.path("context").asText("");
            String prompt = loader.get(promptFile());
            if (prompt == null) prompt = "";
            StringBuilder sb = new StringBuilder();
            sb.append(prompt.trim()).append("\n\n");
            if (!context.isBlank()) {
                sb.append("[Context]\n").append(context.trim()).append("\n\n");
            }
            sb.append("[User Message]\n").append(message);

            AIProvider provider;
            try { provider = AIProvider.valueOf(providerStr.toUpperCase(Locale.ROOT)); }
            catch (Exception e) { provider = AIProvider.OPENAI; }

            AIRequest req = new AIRequest();
            req.setUserId(userId);
            // If the provided userId appears to be an email, also set the email field so
            // downstream rate limiting and usage tracking use email-based FK.
            if (userId != null && userId.contains("@")) {
                req.setEmail(userId);
            }
            req.setProvider(provider);
            req.setMessage(sb.toString());
            AIResponse resp;
            try {
                resp = proxy.process(req);
            } catch (RuntimeException ex) {
                ObjectNode out = mapper.createObjectNode();
                out.put("text", fallbackMessage());
                out.put("model", "error");
                out.put("tokensUsed", 0);
                return out;
            }

            ObjectNode out = mapper.createObjectNode();
            out.put("text", resp.getResponse());
            out.put("model", resp.getModel());
            out.put("tokensUsed", resp.getTokensUsed() == null ? 0 : resp.getTokensUsed());
            return out;
        });
    }
    
    protected String fallbackMessage() {
        return "The AI service is temporarily unavailable. Please try again in a minute. Your message has been saved.";
    }
}
