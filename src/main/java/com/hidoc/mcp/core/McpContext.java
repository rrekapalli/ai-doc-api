package com.hidoc.mcp.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.server.ServletServerHttpRequest;

import java.time.Instant;
import java.util.Optional;

public class McpContext {
    private final String connectionId;
    private final Instant requestTime;
    private final ObjectMapper mapper;
    private final String userId;

    public McpContext(String connectionId, Instant requestTime, ObjectMapper mapper, String userId) {
        this.connectionId = connectionId;
        this.requestTime = requestTime;
        this.mapper = mapper;
        this.userId = userId;
    }

    public String getConnectionId() { return connectionId; }
    public Instant getRequestTime() { return requestTime; }
    public ObjectMapper getMapper() { return mapper; }
    public Optional<String> getUserId() { return Optional.ofNullable(userId); }
}
