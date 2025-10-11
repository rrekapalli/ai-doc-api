package com.hidoc.api.ai.service;

import com.hidoc.api.ai.AIProvider;
import com.hidoc.api.ai.model.AIRequest;
import com.hidoc.api.ai.model.AIResponse;
import com.hidoc.api.service.RateLimitingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Test class for HealthChatOrchestrator
 * Verifies basic conversation flow management and integration
 */
@ExtendWith(MockitoExtension.class)
class HealthChatOrchestratorTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatMemoryService chatMemoryService;

    @Mock
    private PromptService promptService;

    @Mock
    private RateLimitingService rateLimitingService;

    @Mock
    private MessageClassifierTool messageClassifierTool;

    @Mock
    private ChatClient.ChatClientRequestSpec chatRequestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    private HealthChatOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new HealthChatOrchestrator(
                chatClient,
                chatMemoryService,
                promptService,
                rateLimitingService,
                messageClassifierTool
        );
    }

    @Test
    void testProcessHealthMessage_Success() {
        // Arrange
        AIRequest request = createTestRequest();
        String classificationJson = "{\"classification\":\"MEDICAL_QUERY\",\"response\":\"<p>Processing your medical query.</p>\",\"inference\":\"Message classified as medical query\",\"data\":null,\"isFollowUp\":false,\"followUpDataRequired\":null,\"routeTo\":\"MedicalQueryService\",\"shouldDeductFromRateLimit\":true}";
        RateLimitingService.UsageStats usageStats = new RateLimitingService.UsageStats(5, 95, 100, "2024-01");

        when(messageClassifierTool.classifyMessage(any(), any(), any(), any())).thenReturn(classificationJson);
        when(rateLimitingService.getUserUsageStats(any())).thenReturn(usageStats);

        // Act
        AIResponse response = orchestrator.processHealthMessage(request);

        // Assert
        assertNotNull(response);
        assertEquals("<p>Processing your medical query.</p>", response.getResponse());
        assertEquals("test-user", response.getUserId());
        assertEquals("MEDICAL_QUERY", response.getClassification());
        assertEquals(95, response.getAvailableRequests());
        assertNotNull(response.getDateTime());
        assertNotNull(response.getMessageId());
        assertNotNull(response.getConversationId());
        assertEquals(false, response.getIsFollowUp());

        // Verify interactions
        verify(messageClassifierTool).classifyMessage("I have a headache", "test-user", null, "No previous conversation history.");
        verify(chatMemoryService, times(2)).addMessage(any(), any());
        verify(rateLimitingService).getUserUsageStats("test-user");
    }

    @Test
    void testProcessHealthMessage_WithEmail() {
        // Arrange
        AIRequest request = createTestRequestWithEmail();
        String classificationJson = "{\"classification\":\"MEDICAL_QUERY\",\"response\":\"<p>Processing your medical query.</p>\",\"inference\":\"Message classified as medical query\",\"data\":null,\"isFollowUp\":false,\"followUpDataRequired\":null,\"routeTo\":\"MedicalQueryService\",\"shouldDeductFromRateLimit\":true}";
        RateLimitingService.UsageStats usageStats = new RateLimitingService.UsageStats(10, 90, 100, "2024-01");

        when(messageClassifierTool.classifyMessage(any(), any(), any(), any())).thenReturn(classificationJson);
        when(rateLimitingService.getUsageStatsByEmail(any())).thenReturn(usageStats);

        // Act
        AIResponse response = orchestrator.processHealthMessage(request);

        // Assert
        assertNotNull(response);
        assertEquals(90, response.getAvailableRequests());
        verify(messageClassifierTool).classifyMessage("I have a headache", "test-user", "test@example.com", "No previous conversation history.");
        verify(rateLimitingService).getUsageStatsByEmail("test@example.com");
    }

    @Test
    void testProcessHealthMessage_Error() {
        // Arrange
        AIRequest request = createTestRequest();
        when(messageClassifierTool.classifyMessage(any(), any(), any(), any())).thenThrow(new RuntimeException("Test error"));

        // Act
        AIResponse response = orchestrator.processHealthMessage(request);

        // Assert
        assertNotNull(response);
        assertEquals("ERROR", response.getClassification());
        assertTrue(response.getResponse().contains("error"));
        assertTrue(response.getInference().contains("Test error"));
    }

    @Test
    void testIsReady_AllDependenciesAvailable() {
        // Arrange
        when(promptService.getSpringAiMasterPrompt()).thenReturn("Test prompt");

        // Act
        boolean ready = orchestrator.isReady();

        // Assert
        assertTrue(ready);
    }

    @Test
    void testIsReady_MissingDependencies() {
        // Arrange
        when(promptService.getSpringAiMasterPrompt()).thenThrow(new RuntimeException("Not available"));

        // Act
        boolean ready = orchestrator.isReady();

        // Assert
        assertFalse(ready);
    }

    @Test
    void testGetStatus_Ready() {
        // Arrange
        when(promptService.getSpringAiMasterPrompt()).thenReturn("Test prompt");

        // Act
        String status = orchestrator.getStatus();

        // Assert
        assertTrue(status.contains("ready and operational"));
    }

    @Test
    void testGetStatus_NotReady() {
        // Arrange
        when(promptService.getSpringAiMasterPrompt()).thenThrow(new RuntimeException("Not available"));

        // Act
        String status = orchestrator.getStatus();

        // Assert
        assertTrue(status.contains("not ready"));
    }

    private AIRequest createTestRequest() {
        AIRequest request = new AIRequest();
        request.setMessage("I have a headache");
        request.setProvider(AIProvider.OPENAI);
        request.setUserId("test-user");
        return request;
    }

    private AIRequest createTestRequestWithEmail() {
        AIRequest request = createTestRequest();
        request.setEmail("test@example.com");
        return request;
    }
}