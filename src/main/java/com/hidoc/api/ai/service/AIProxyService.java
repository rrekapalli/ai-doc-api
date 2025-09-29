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
        String email = request.getEmail();
        // Heuristic: if no explicit email was provided but userId looks like an email, treat it as email
        if ((email == null || email.isBlank()) && userId != null && userId.contains("@")) {
            email = userId;
        }
        AIProvider provider = request.getProvider();
        if ((userId == null || userId.isBlank()) && (email == null || email.isBlank())) {
            throw new IllegalArgumentException("userId or email is required");
        }
        if (provider == null) {
            throw new IllegalArgumentException("provider is required");
        }

        boolean allowed;
        if (email != null && !email.isBlank()) {
            allowed = rateLimitingService.isRequestAllowedByEmail(email);
        } else {
            allowed = rateLimitingService.isRequestAllowed(userId);
        }
        if (!allowed) {
            if (email != null && !email.isBlank()) {
                rateLimitingService.recordRequestByEmail(email, provider.name(), false, "Rate limit exceeded");
            } else {
                rateLimitingService.recordRequest(userId, provider.name(), false, "Rate limit exceeded");
            }
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
            if (email != null && !email.isBlank()) {
                rateLimitingService.recordRequestByEmail(email, provider.name(), success, error);
            } else {
                rateLimitingService.recordRequest(userId, provider.name(), success, error);
            }
        }
    }
}
