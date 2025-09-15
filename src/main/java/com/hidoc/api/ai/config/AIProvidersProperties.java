package com.hidoc.api.ai.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "ai.providers")
public class AIProvidersProperties {

    @Valid
    private ProviderConfig openai = new ProviderConfig();
    @Valid
    private ProviderConfig grok = new ProviderConfig();
    @Valid
    private ProviderConfig gemini = new ProviderConfig();

    public ProviderConfig getOpenai() { return openai; }
    public void setOpenai(ProviderConfig openai) { this.openai = openai; }
    public ProviderConfig getGrok() { return grok; }
    public void setGrok(ProviderConfig grok) { this.grok = grok; }
    public ProviderConfig getGemini() { return gemini; }
    public void setGemini(ProviderConfig gemini) { this.gemini = gemini; }

    public static class ProviderConfig {
        @NotBlank(message = "API key is required")
        private String apiKey;
        @NotBlank(message = "Model is required")
        private String model;
        @NotBlank(message = "Base URL is required")
        private String baseUrl;

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    }
}
