package com.hidoc.api.web;

import com.hidoc.api.exception.RateLimitExceededException;
import com.hidoc.api.web.error.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldFormat400_forIllegalArgument() {
        HttpServletRequest req = new MockHttpServletRequest("POST", "/api/ai/openai/chat");
        var resp = handler.handleBadRequest(new IllegalArgumentException("Bad input\ninfo"), req);
        assertThat(resp.getStatusCode().value()).isEqualTo(400);
        Map<?,?> body = (Map<?,?>) resp.getBody();
        assertThat(body.get("error")).isNotNull();
        assertThat(body.get("message")).isNotNull();
        assertThat(body.get("status")).isNotNull();
        assertThat(body.get("timestamp")).isNotNull();
        assertThat(body.get("path")).isNotNull();
        assertThat(((Map<?,?>) resp.getBody()).get("message").toString()).doesNotContain("\n");
    }

    @Test
    void shouldFormat429_forRateLimit() {
        HttpServletRequest req = new MockHttpServletRequest("POST", "/api/ai/openai/chat");
        var resp = handler.handleTooMany(new RateLimitExceededException("Monthly request limit exceeded"), req);
        assertThat(resp.getStatusCode().value()).isEqualTo(429);
        assertThat(((Map<?,?>) resp.getBody()).get("message")).isEqualTo("Monthly request limit exceeded");
    }

    @Test
    void shouldFormat500_forGeneric() {
        HttpServletRequest req = new MockHttpServletRequest("POST", "/x");
        var resp = handler.handleGeneric(new RuntimeException("boom"), req);
        assertThat(resp.getStatusCode().value()).isEqualTo(500);
        assertThat(((Map<?,?>) resp.getBody()).get("message")).isEqualTo("Internal server error");
    }
}
