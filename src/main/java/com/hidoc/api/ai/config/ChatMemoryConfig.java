package com.hidoc.api.ai.config;

import com.hidoc.api.ai.service.ChatMemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Configuration class for chat memory and conversation management.
 * Sets up Spring AI ChatMemory beans and scheduled cleanup tasks.
 */
@Configuration
@EnableScheduling
public class ChatMemoryConfig {

    private static final Logger logger = LoggerFactory.getLogger(ChatMemoryConfig.class);

    private final ChatMemoryService chatMemoryService;

    public ChatMemoryConfig(ChatMemoryService chatMemoryService) {
        this.chatMemoryService = chatMemoryService;
    }

    /**
     * Scheduled task to clean up expired conversations every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
    public void cleanupExpiredConversations() {
        logger.debug("Running scheduled cleanup of expired conversations");
        try {
            int beforeCount = chatMemoryService.getActiveConversationCount();
            chatMemoryService.cleanupExpiredConversations();
            int afterCount = chatMemoryService.getActiveConversationCount();
            
            if (beforeCount != afterCount) {
                logger.info("Cleaned up {} expired conversations. Active conversations: {}", 
                           beforeCount - afterCount, afterCount);
            }
        } catch (Exception e) {
            logger.error("Error during conversation cleanup", e);
        }
    }

    /**
     * Scheduled task to log conversation statistics every 6 hours
     */
    @Scheduled(fixedRate = 21600000) // 6 hours in milliseconds
    public void logConversationStatistics() {
        try {
            int activeConversations = chatMemoryService.getActiveConversationCount();
            logger.info("Chat memory statistics - Active conversations: {}, Max messages per conversation: {}", 
                       activeConversations, chatMemoryService.getMaxMessages());
        } catch (Exception e) {
            logger.error("Error logging conversation statistics", e);
        }
    }
}