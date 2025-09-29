package com.hidoc.mcp.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hidoc.mcp.core.McpContext;
import com.hidoc.mcp.core.ToolRegistry;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping(path = "/mcp/jsonrpc")
public class McpJsonRpcController {

    private final ObjectMapper mapper;
    private final ToolRegistry registry;

    public McpJsonRpcController(ObjectMapper mapper, ToolRegistry registry) {
        this.mapper = mapper;
        this.registry = registry;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public CompletableFuture<ObjectNode> handle(@RequestBody ObjectNode request,
                                               @RequestHeader(value = "X-User-Id", required = false) String userId) {
        String id = request.path("id").asText(UUID.randomUUID().toString());
        String method = request.path("method").asText(null);
        JsonNode params = request.path("params");
        if (method == null) {
            return CompletableFuture.completedFuture(error(id, -32600, "Invalid Request: missing method"));
        }
        McpContext ctx = new McpContext("http:jsonrpc", Instant.now(), mapper, userId);
        return registry.call(method, params, ctx)
                .thenApply(result -> success(id, result))
                .exceptionally(ex -> error(id, -32000, ex.getMessage()));
    }

    private ObjectNode success(String id, JsonNode result) {
        ObjectNode resp = mapper.createObjectNode();
        resp.put("jsonrpc", "2.0");
        resp.put("id", id);
        resp.set("result", result == null ? mapper.createObjectNode() : result);
        return resp;
    }

    private ObjectNode error(String id, int code, String message) {
        ObjectNode resp = mapper.createObjectNode();
        resp.put("jsonrpc", "2.0");
        resp.put("id", id);
        ObjectNode err = mapper.createObjectNode();
        err.put("code", code);
        err.put("message", message);
        resp.set("error", err);
        return resp;
    }
}
