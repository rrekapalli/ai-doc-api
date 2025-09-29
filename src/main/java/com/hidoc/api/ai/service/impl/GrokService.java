package com.hidoc.api.ai.service.impl;

import com.hidoc.api.ai.AIProvider;
import com.hidoc.api.ai.config.AIProvidersProperties;
import com.hidoc.api.ai.model.AIRequest;
import com.hidoc.api.ai.model.AIResponse;
import com.hidoc.api.ai.service.AIService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@ConditionalOnProperty(prefix = "ai.providers.grok", name = "enabled", havingValue = "true")
public class GrokService implements AIService {

    private final AIProvidersProperties.ProviderConfig cfg;

    public GrokService(AIProvidersProperties props) {
        this.cfg = props.getGrok();
    }

    @Override
    public AIProvider provider() {
        return AIProvider.GROK;
    }

    @Override
    public AIResponse chat(AIRequest request) {
        validateConfigured();
        AIResponse resp = new AIResponse();
        resp.setResponse("[Grok:" + cfg.getModel() + "] " + safe(request.getMessage()));
        resp.setModel(cfg.getModel());
        resp.setRequestId(java.util.UUID.randomUUID().toString());
        return resp;
    }

    private void validateConfigured() {
        if (!StringUtils.hasText(cfg.getApiKey())) {
            throw new IllegalStateException("Grok API key not configured");
        }
        if (!StringUtils.hasText(cfg.getBaseUrl())) {
            throw new IllegalStateException("Grok baseUrl not configured");
        }
        if (!StringUtils.hasText(cfg.getModel())) {
            throw new IllegalStateException("Grok model not configured");
        }
    }

    private String safe(String s) { return s == null ? "" : s; }
}
