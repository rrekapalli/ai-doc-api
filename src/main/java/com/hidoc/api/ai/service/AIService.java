package com.hidoc.api.ai.service;

import com.hidoc.api.ai.AIProvider;
import com.hidoc.api.ai.model.AIRequest;
import com.hidoc.api.ai.model.AIResponse;

public interface AIService {
    AIProvider provider();
    AIResponse chat(AIRequest request);
}
