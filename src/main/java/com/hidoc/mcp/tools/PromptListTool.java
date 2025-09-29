package com.hidoc.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.hidoc.mcp.core.McpContext;
import com.hidoc.mcp.core.McpTool;
import com.hidoc.mcp.util.PromptLoader;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class PromptListTool implements McpTool {
    private final ObjectMapper mapper;
    private final PromptLoader loader;

    public PromptListTool(ObjectMapper mapper, PromptLoader loader) {
        this.mapper = mapper;
        this.loader = loader;
    }

    @Override
    public String name() { return "prompts.list"; }

    @Override
    public JsonNode schema() {
        return mapper.createObjectNode().put("type", "object");
    }

    @Override
    public CompletableFuture<JsonNode> call(JsonNode params, McpContext ctx) {
        return CompletableFuture.supplyAsync(() -> {
            ArrayNode arr = mapper.createArrayNode();
            for (String n : loader.list()) arr.add(n);
            return mapper.createObjectNode().set("files", arr);
        });
    }
}
