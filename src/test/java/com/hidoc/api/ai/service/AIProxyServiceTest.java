package com.hidoc.api.ai.service;

import com.hidoc.api.ai.AIProvider;
import com.hidoc.api.ai.config.AIProvidersProperties;
import com.hidoc.api.ai.model.AIRequest;
import com.hidoc.api.ai.model.AIResponse;
import com.hidoc.api.ai.service.impl.GeminiService;
import com.hidoc.api.ai.service.impl.GrokService;
import com.hidoc.api.ai.service.impl.OpenAIService;
import com.hidoc.api.exception.RateLimitExceededException;
import com.hidoc.api.service.RateLimitingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AIProxyServiceTest {

    private RateLimitingService rateLimitingService;
    private AIProxyService proxyService;

    @BeforeEach
    void setup() {
        rateLimitingService = mock(RateLimitingService.class);

        AIProvidersProperties props = new AIProvidersProperties();
        // Configure props with dummy but non-blank values
        var openai = new AIProvidersProperties.ProviderConfig();
        openai.setApiKey("k"); openai.setModel("m"); openai.setBaseUrl("u");
        var grok = new AIProvidersProperties.ProviderConfig();
        grok.setApiKey("k"); grok.setModel("m"); grok.setBaseUrl("u");
        var gemini = new AIProvidersProperties.ProviderConfig();
        gemini.setApiKey("k"); gemini.setModel("m"); gemini.setBaseUrl("u");
        props.setOpenai(openai);
        props.setGrok(grok);
        props.setGemini(gemini);

        var openAIService = new OpenAIService(props);
        var grokService = new GrokService(props);
        var geminiService = new GeminiService(props);
        proxyService = new AIProxyService(List.of(openAIService, grokService, geminiService), rateLimitingService);
    }

    @Test
    void process_shouldCallProvider_whenAllowed() {
        when(rateLimitingService.isRequestAllowed("user1")).thenReturn(true);

        AIRequest req = new AIRequest();
        req.setUserId("user1");
        req.setProvider(AIProvider.OPENAI);
        req.setMessage("hello");

        AIResponse resp = proxyService.process(req);
        assertThat(resp.getReply()).contains("[OpenAI:");

        verify(rateLimitingService).isRequestAllowed("user1");
        verify(rateLimitingService).recordRequest(eq("user1"), eq("OPENAI"), eq(true), isNull());
    }

    @Test
    void process_shouldThrow429_whenNotAllowed() {
        when(rateLimitingService.isRequestAllowed("user2")).thenReturn(false);

        AIRequest req = new AIRequest();
        req.setUserId("user2");
        req.setProvider(AIProvider.GROK);
        req.setMessage("hello");

        assertThatThrownBy(() -> proxyService.process(req))
                .isInstanceOf(RateLimitExceededException.class);

        verify(rateLimitingService).recordRequest(eq("user2"), eq("GROK"), eq(false), anyString());
        verify(rateLimitingService, never()).recordRequest(eq("user2"), eq("GROK"), eq(true), any());
    }
}
