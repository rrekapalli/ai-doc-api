package com.hidoc.api.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI configuration for chat orchestration and memory management
 */
@Configuration
public class SpringAIConfig {
    
    /**
     * Configure ChatClient with default settings for health chat orchestration
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
            .build();
    }
    
    /**
     * Configure in-memory chat memory for conversation context
     * TODO: Consider persistent storage for production use
     */
    @Bean
    public ChatMemory chatMemory() {
        return new InMemoryChatMemory();
    }
}