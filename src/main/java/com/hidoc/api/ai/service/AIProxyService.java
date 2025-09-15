package com.hidoc.api.ai.service;

import com.hidoc.api.ai.AIProvider;
import com.hidoc.api.ai.model.AIRequest;
import com.hidoc.api.ai.model.AIResponse;
import com.hidoc.api.exception.RateLimitExceededException;
import com.hidoc.api.service.RateLimitingService;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class AIProxyService {

    private final Map<AIProvider, AIService> providers = new EnumMap<>(AIProvider.class);
    private final RateLimitingService rateLimitingService;

    public AIProxyService(List<AIService> providerBeans, RateLimitingService rateLimitingService) {
        for (AIService svc : providerBeans) {
            providers.put(svc.provider(), svc);
        }
        this.rateLimitingService = rateLimitingService;
    }

    public AIResponse process(AIRequest request) {
        String userId = request.getUserId();
        AIProvider provider = request.getProvider();
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        if (provider == null) {
            throw new IllegalArgumentException("provider is required");
        }

        if (!rateLimitingService.isRequestAllowed(userId)) {
            rateLimitingService.recordRequest(userId, provider.name(), false, "Rate limit exceeded");
            throw new RateLimitExceededException("Monthly request limit exceeded");
        }

        boolean success = false;
        String error = null;
        try {
            AIService svc = providers.get(provider);
            if (svc == null) {
                throw new IllegalArgumentException("Unsupported provider: " + provider);
            }
            AIResponse resp = svc.chat(request);
            success = true;
            return resp;
        } catch (RuntimeException ex) {
            error = ex.getMessage();
            throw ex;
        } finally {
            rateLimitingService.recordRequest(userId, provider.name(), success, error);
        }
    }
}
