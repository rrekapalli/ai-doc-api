package com.hidoc.mcp.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class ToolRegistry {
    private final Map<String, McpTool> tools = new LinkedHashMap<>();
    private final ObjectMapper mapper;

    public ToolRegistry(ObjectMapper mapper, java.util.List<McpTool> toolBeans) {
        this.mapper = mapper;
        if (toolBeans != null) {
            for (McpTool t : toolBeans) {
                tools.put(t.name(), t);
            }
        }
    }

    public Map<String, McpTool> list() { return Collections.unmodifiableMap(tools); }

    public CompletableFuture<JsonNode> call(String name, JsonNode params, McpContext ctx) {
        McpTool tool = tools.get(name);
        if (tool == null) {
            ObjectNode error = mapper.createObjectNode();
            error.put("error", "Unknown tool: " + name);
            return CompletableFuture.completedFuture(error);
        }
        return tool.call(params, ctx);
    }
}
