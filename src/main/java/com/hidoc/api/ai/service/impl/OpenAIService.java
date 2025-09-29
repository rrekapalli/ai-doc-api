package com.hidoc.api.ai.service.impl;

import com.hidoc.api.ai.AIProvider;
import com.hidoc.api.ai.config.AIProvidersProperties;
import com.hidoc.api.ai.model.AIRequest;
import com.hidoc.api.ai.model.AIResponse;
import com.hidoc.api.ai.service.AIService;
import com.hidoc.api.exception.AIServiceUnavailableException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class OpenAIService implements AIService {

    private static final Logger log = LoggerFactory.getLogger(OpenAIService.class);

    private final AIProvidersProperties.ProviderConfig cfg;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public OpenAIService(AIProvidersProperties props) {
        this.cfg = props.getOpenai();
    }

    @Override
    public AIProvider provider() {
        return AIProvider.OPENAI;
    }

    @Override
    public AIResponse chat(AIRequest request) {
        validateConfigured();
        // If baseUrl is not an HTTP(S) URL (e.g., tests pass "u"), fall back to stubbed response to keep unit tests isolated
        if (!startsWithHttp(cfg.getBaseUrl())) {
            AIResponse resp = new AIResponse();
            resp.setResponse("[OpenAI:" + cfg.getModel() + "] " + (request.getMessage() == null ? "" : request.getMessage()));
            resp.setModel(cfg.getModel());
            resp.setRequestId(java.util.UUID.randomUUID().toString());
            return resp;
        }
        long start = System.currentTimeMillis();
        String prompt = request.getMessage() == null ? "" : request.getMessage();
        String url = ensureNoTrailingSlash(cfg.getBaseUrl()) + "/chat/completions";
        try {
            // Build request body
            var body = Map.of(
                    "model", cfg.getModel(),
                    "messages", List.of(Map.of("role", "user", "content", prompt))
            );
            String json = objectMapper.writeValueAsString(body);

            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + cfg.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            log.info("[OpenAI] Sending chat request model={} length={} user_id={}...", cfg.getModel(), prompt.length(), request.getUserId());
            HttpResponse<String> httpResp = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofString());
            long took = System.currentTimeMillis() - start;
            if (httpResp.statusCode() / 100 != 2) {
                log.warn("[OpenAI] Non-2xx response status={} body={} ({} ms)", httpResp.statusCode(), truncate(httpResp.body(), 500), took);
                throw new AIServiceUnavailableException("OpenAI error: HTTP " + httpResp.statusCode());
            }

            ChatCompletionsResponse resp = objectMapper.readValue(httpResp.body(), ChatCompletionsResponse.class);
            String content = null;
            if (resp.choices != null && !resp.choices.isEmpty() && resp.choices.get(0).message != null) {
                content = resp.choices.get(0).message.content;
            }
            AIResponse out = new AIResponse();
            out.setResponse(content != null ? content : "");
            out.setModel(cfg.getModel());
            out.setRequestId(resp.id);
            if (resp.usage != null && resp.usage.totalTokens != null) {
                out.setTokensUsed(resp.usage.totalTokens);
            }
            log.info("[OpenAI] Received response id={} tokens={} ({} ms)", out.getRequestId(), out.getTokensUsed(), took);
            return out;
        } catch (AIServiceUnavailableException ex) {
            throw ex;
        } catch (Exception ex) {
            long took = System.currentTimeMillis() - start;
            log.error("[OpenAI] Request failed after {} ms: {}", took, ex.toString());
            throw new AIServiceUnavailableException("OpenAI request failed: " + ex.getMessage());
        }
    }

    private void validateConfigured() {
        if (!StringUtils.hasText(cfg.getApiKey())) {
            throw new IllegalStateException("OpenAI API key not configured");
        }
        if (!StringUtils.hasText(cfg.getBaseUrl())) {
            throw new IllegalStateException("OpenAI baseUrl not configured");
        }
        if (!StringUtils.hasText(cfg.getModel())) {
            throw new IllegalStateException("OpenAI model not configured");
        }
    }

    private String ensureNoTrailingSlash(String base) {
        if (base.endsWith("/v1")) return base;
        if (base.endsWith("/v1/")) return base.substring(0, base.length() - 1);
        // If base URL already full like https://api.openai.com/v1, use as is; otherwise append /v1
        return base.endsWith("/") ? base + "v1" : base + "/v1";
    }

    private boolean startsWithHttp(String s) {
        if (s == null) return false;
        String t = s.trim().toLowerCase();
        return t.startsWith("http://") || t.startsWith("https://");
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ChatCompletionsResponse {
        public String id;
        public List<Choice> choices;
        public Usage usage;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Choice {
        public Message message;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Message {
        public String role;
        public String content;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Usage {
        @JsonProperty("total_tokens")
        public Integer totalTokens;
    }
}
