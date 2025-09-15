package com.hidoc.api.ai.service.impl;

import com.hidoc.api.ai.AIProvider;
import com.hidoc.api.ai.config.AIProvidersProperties;
import com.hidoc.api.ai.model.AIRequest;
import com.hidoc.api.ai.model.AIResponse;
import com.hidoc.api.ai.service.AIService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenAIService implements AIService {

    private final AIProvidersProperties.ProviderConfig cfg;

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
        AIResponse resp = new AIResponse();
        resp.setReply("[OpenAI:" + cfg.getModel() + "] " + safe(request.getMessage()));
        resp.setReasoning("stubbed");
        return resp;
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

    private String safe(String s) { return s == null ? "" : s; }
}
