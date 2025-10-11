package com.hidoc.api.ai.config;

import com.hidoc.api.ai.service.MessageClassifierTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

/**
 * Spring AI configuration for chat orchestration and memory management
 */
@Configuration
public class SpringAIConfig {
    
    /**
     * Configure ChatClient for health chat orchestration with function calling
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
            .defaultFunctions("classifyMessage")
            .build();
    }
    
    /**
     * Register message classifier as a function for Spring AI
     */
    @Bean
    @Description("Classify health-related messages and route them to appropriate specialized tools")
    public Function<MessageClassifierTool.ClassificationRequest, String> classifyMessage(MessageClassifierTool messageClassifierTool) {
        return request -> messageClassifierTool.classifyMessage(
            request.getMessage(), 
            request.getUserId(), 
            request.getEmail(), 
            request.getMessageHistory()
        );
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