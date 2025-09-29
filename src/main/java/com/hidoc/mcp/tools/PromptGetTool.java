package com.hidoc.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hidoc.mcp.core.McpContext;
import com.hidoc.mcp.core.McpTool;
import com.hidoc.mcp.util.PromptLoader;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class PromptGetTool implements McpTool {
    private final ObjectMapper mapper;
    private final PromptLoader loader;

    public PromptGetTool(ObjectMapper mapper, PromptLoader loader) {
        this.mapper = mapper;
        this.loader = loader;
    }

    @Override
    public String name() { return "prompts.get"; }

    @Override
    public JsonNode schema() {
        ObjectNode s = mapper.createObjectNode();
        s.put("type", "object");
        ObjectNode props = s.putObject("properties");
        props.putObject("name").put("type", "string");
        s.putArray("required").add("name");
        return s;
    }

    @Override
    public CompletableFuture<JsonNode> call(JsonNode params, McpContext ctx) {
        return CompletableFuture.supplyAsync(() -> {
            String name = params.path("name").asText(null);
            String content = name == null ? null : loader.get(name);
            ObjectNode out = mapper.createObjectNode();
            if (content == null) {
                out.put("error", "Prompt not found: " + name);
            } else {
                out.put("name", name);
                out.put("content", content);
            }
            return out;
        });
    }
}
