package com.hidoc.mcp.core;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.concurrent.CompletableFuture;

public interface McpTool {
    String name();
    JsonNode schema();
    CompletableFuture<JsonNode> call(JsonNode params, McpContext ctx);
}
