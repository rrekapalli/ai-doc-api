package com.hidoc.api.ai.util;

import java.util.UUID;

/**
 * Utility class for conversation ID generation and management.
 */
public class ConversationUtils {

    /**
     * Generate a new unique conversation ID
     */
    public static String generateConversationId() {
        return "conv_" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Generate a new unique message ID
     */
    public static String generateMessageId() {
        return "msg_" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Generate a conversation ID for a specific user (deterministic for session management)
     */
    public static String generateUserConversationId(String userId) {
        return "conv_" + userId + "_" + System.currentTimeMillis();
    }

    /**
     * Validate conversation ID format
     */
    public static boolean isValidConversationId(String conversationId) {
        return conversationId != null && 
               conversationId.startsWith("conv_") && 
               conversationId.length() > 5;
    }

    /**
     * Validate message ID format
     */
    public static boolean isValidMessageId(String messageId) {
        return messageId != null && 
               messageId.startsWith("msg_") && 
               messageId.length() > 4;
    }

    /**
     * Extract user ID from conversation ID if it follows the user-specific format
     */
    public static String extractUserIdFromConversationId(String conversationId) {
        if (conversationId != null && conversationId.startsWith("conv_")) {
            String[] parts = conversationId.split("_");
            if (parts.length >= 3) {
                return parts[1]; // Return the user ID part
            }
        }
        return null;
    }
}