# Design Document

## Overview

This design document outlines the architecture for refactoring the AI-Doc-API service to leverage Spring AI's advanced features including @Tool annotations, Chat Orchestrators, Advisors, and Chat Memory. The refactor will modernize the existing health tracking application while maintaining backward compatibility and preserving all current functionality.

The design follows a modular approach where the existing MCP-based system will be enhanced with Spring AI capabilities, creating a hybrid architecture that leverages the best of both approaches.

## Architecture

### High-Level Architecture

```mermaid
graph TB
    Client[Flutter Client] --> Controller[UnifiedAIController]
    Controller --> Orchestrator[HealthChatOrchestrator]
    Orchestrator --> Memory[ChatMemoryService]
    Orchestrator --> Classifier[MessageClassifierTool]
    
    Classifier --> HealthTools[Health Data Tools]
    Classifier --> MedTools[Medical Query Tools]
    Classifier --> TrendTools[Trend Analysis Tools]
    Classifier --> RejectTool[Non-Health Rejection Tool]
    
    HealthTools --> MedicationTool[@Tool MedicationService]
    HealthTools --> ActivityTool[@Tool ActivityService]
    HealthTools --> HealthParamTool[@Tool HealthParameterService]
    HealthTools --> MoodTool[@Tool MoodService]
    HealthTools --> FoodTool[@Tool FoodIntakeService]
    HealthTools --> ReportTool[@Tool ReportProcessingService]
    
    TrendTools --> TrendAnalysisTool[@Tool TrendAnalysisService]
    TrendTools --> PrognosisTool[@Tool PrognosisService]
    
    MedTools --> MedicalQueryTool[@Tool MedicalQueryService]
    
    Orchestrator --> Advisors[Spring AI Advisors]
    Advisors --> PreProcessor[Request Preprocessor]
    Advisors --> PostProcessor[Response Postprocessor]
    Advisors --> ValidationAdvisor[Validation Advisor]
    
    HealthTools --> Database[(Database)]
    TrendTools --> Database
    MedTools --> Database
    
    Orchestrator --> RateLimit[RateLimitingService]
    Orchestrator --> PromptService[PromptService]
```

### Core Components

#### 1. HealthChatOrchestrator
- Central orchestrator implementing Spring AI's ChatClient
- Manages conversation flow and tool routing
- Integrates with chat memory for context preservation
- Coordinates with advisors for cross-cutting concerns

#### 2. Spring AI Tools (@Tool annotated services)
- **MessageClassifierTool**: Routes messages using spring_ai_message_classifier_tool.txt
- **MedicationService**: Handles medication data entry using medication_data_entry_prompt.txt
- **ActivityService**: Manages physical activity tracking using activity_data_entry_prompt.txt
- **HealthParameterService**: Processes vital signs using health_data_entry_prompt.txt
- **MoodService**: Handles mood data entry using mood_data_entry_tool.txt
- **FoodIntakeService**: Manages nutrition tracking using food_intake_tool.txt
- **ReportProcessingService**: Processes medical reports using reports_processing_prompt.txt
- **MedicalQueryService**: Handles medical information using medical_query_elaboration_prompt.txt
- **TrendAnalysisService**: Analyzes health patterns using trend_analysis_tool.txt
- **PrognosisService**: Provides comprehensive health prognosis using prognosis_tool.txt
- **NonHealthRejectionTool**: Handles non-health queries using non_health_rejection_tool.txt

#### 3. Spring AI Advisors
- **RequestPreprocessorAdvisor**: Validates and preprocesses incoming requests
- **ResponsePostprocessorAdvisor**: Formats responses and adds metadata
- **ValidationAdvisor**: Ensures data integrity and format compliance
- **MedicalDisclaimerAdvisor**: Adds medical disclaimers to appropriate responses

#### 4. ChatMemoryService
- Implements Spring AI's ChatMemory interface
- Stores and retrieves conversation history
- Configurable message history limit (default: 100 messages)

## Components and Interfaces

### Enhanced AIRequest Model

```java
public class AIRequest {
    private String message;
    private AIProvider provider;
    private String userId;
    private String email;
    private Map<String, Object> metadata;
    private List<ChatMessage> messageHistory; // New field
    
    // Existing getters/setters + new ones for messageHistory
}
```

### Enhanced AIResponse Model

```java
public class AIResponse {
    // Existing fields
    private String response;
    private String model;
    private Integer tokensUsed;
    private String requestId;
    private HealthDataEntry entry;
    private List<Map<String, Object>> matches;
    
    // New fields
    private String classification;
    private String inference;
    private Map<String, Object> data;
    private LocalDateTime dateTime;
    private String messageId;
    private String conversationId;
    private String userId;
    private Integer availableRequests;
    private Boolean isFollowUp;
    private String followUpDataRequired;
    
    // Getters and setters for all fields
}
```

### Tool Interface Structure

```java
@Component
public class MedicationService {
    
    @Tool("Process medication data entry and queries")
    public ToolResponse processMedicationRequest(
        @ToolParameter("User message about medication") String message,
        @ToolParameter("User ID") String userId,
        @ToolParameter("Conversation context") String context
    ) {
        // Implementation using existing prompts
    }
}
```

### Chat Orchestrator Interface

```java
@Service
public class HealthChatOrchestrator {
    
    private final ChatClient chatClient;
    private final ChatMemoryService chatMemory;
    private final List<Advisor> advisors;
    private final RateLimitingService rateLimitingService;
    
    public AIResponse processHealthMessage(AIRequest request) {
        // Orchestrate the conversation flow
    }
}
```

## Data Models

### Conversation Context Model

```java
public class ConversationContext {
    private String conversationId;
    private String userId;
    private List<ChatMessage> messageHistory;
    private Map<String, Object> sessionData;
    private LocalDateTime lastActivity;
}
```

### Tool Response Model

```java
public class ToolResponse {
    private String classification;
    private String response;
    private String inference;
    private Map<String, Object> data;
    private Boolean isFollowUp;
    private String followUpDataRequired;
    private Boolean shouldDeductFromRateLimit;
}
```

### Historic Data Model (for Prognosis)

```java
public class HistoricHealthData {
    private String userId;
    private Map<String, List<HealthDataEntry>> healthParams;
    private Map<String, List<MedicationEntry>> medications;
    private Map<String, List<ActivityEntry>> activities;
    private Map<String, List<MoodEntry>> moods;
    private Map<String, List<FoodEntry>> foodIntake;
    private List<ReportEntry> reports;
}
```

## Error Handling

### Exception Hierarchy

```java
// Existing exceptions remain
public class RateLimitExceededException extends RuntimeException
public class AIServiceUnavailableException extends RuntimeException
public class InvalidRequestException extends RuntimeException

// New Spring AI specific exceptions
public class ToolExecutionException extends RuntimeException
public class ChatMemoryException extends RuntimeException
public class ConversationContextException extends RuntimeException
```

### Error Response Format

```java
public class ErrorResponse {
    private String error;
    private String message;
    private String classification;
    private LocalDateTime timestamp;
    private String messageId;
    private String conversationId;
    private Integer availableRequests;
}
```

## Testing Strategy

### Unit Testing (Optional - MVP Focus)
- Test individual @Tool annotated methods
- Mock Spring AI components for isolated testing
- Validate prompt processing logic
- Test advisor functionality

### Integration Testing (Optional - MVP Focus)
- Test complete conversation flows
- Validate chat memory persistence
- Test rate limiting integration
- Verify database operations

### End-to-End Testing (Optional - MVP Focus)
- Test complete user journeys through REST API
- Validate conversation context preservation
- Test error handling scenarios
- Performance testing under load

## Implementation Approach

### Phase 1: Core Infrastructure
1. Set up Spring AI dependencies and configuration
2. Create enhanced AIRequest and AIResponse models
3. Implement ChatMemoryService
4. Create base HealthChatOrchestrator

### Phase 2: Tool Implementation
1. Implement MessageClassifierTool using existing classifier prompt
2. Create health data entry tools (medication, activity, health parameters, mood, food)
3. Implement medical query tool
4. Create report processing tool
5. Implement non-health rejection tool

### Phase 3: Advanced Features
1. Implement trend analysis tool
2. Create prognosis tool with historic data processing
3. Add followup mechanism
4. Implement comprehensive response formatting

### Phase 4: Advisors and Enhancement
1. Create request preprocessing advisor
2. Implement response postprocessing advisor
3. Add validation advisor
4. Create medical disclaimer advisor

### Phase 5: Integration and Testing
1. Integrate with existing UnifiedAIController
2. Ensure backward compatibility
3. Test conversation flows
4. Validate rate limiting integration

## Configuration

### Spring AI Configuration

```java
@Configuration
@EnableAutoConfiguration
public class SpringAIConfig {
    
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
            .defaultAdvisors(advisors())
            .build();
    }
    
    @Bean
    public ChatMemory chatMemory() {
        return new InMemoryChatMemory();
    }
    
    private List<Advisor> advisors() {
        return List.of(
            new RequestPreprocessorAdvisor(),
            new ValidationAdvisor(),
            new ResponsePostprocessorAdvisor(),
            new MedicalDisclaimerAdvisor()
        );
    }
}
```

### Prompt Configuration

```java
@Service
public class PromptService {
    
    @Value("classpath:prompts/spring_ai_master_prompt.txt")
    private Resource springAiMasterPrompt;
    
    @Value("classpath:prompts/spring_ai_message_classifier_tool.txt")
    private Resource classifierPrompt;
    
    @Value("classpath:prompts/mood_data_entry_tool.txt")
    private Resource moodPrompt;
    
    @Value("classpath:prompts/food_intake_tool.txt")
    private Resource foodIntakePrompt;
    
    @Value("classpath:prompts/trend_analysis_tool.txt")
    private Resource trendAnalysisPrompt;
    
    @Value("classpath:prompts/prognosis_tool.txt")
    private Resource prognosisPrompt;
    
    @Value("classpath:prompts/non_health_rejection_tool.txt")
    private Resource rejectionPrompt;
    
    // Additional existing prompt resources
    
    public String getPrompt(String promptName) {
        // Load and return prompt content
    }
}
```

## Migration Strategy

### Backward Compatibility
- Maintain existing REST endpoint `/api/ai/chat`
- Preserve existing request/response formats while adding new fields
- Keep existing rate limiting and authentication mechanisms
- Maintain database schema compatibility

### Gradual Migration
1. Deploy new Spring AI infrastructure alongside existing MCP system
2. Route specific message types to new system for testing
3. Gradually migrate all functionality to Spring AI
4. Deprecate MCP components once fully migrated

### Rollback Plan
- Feature flags to switch between old and new systems
- Database migration scripts with rollback capability
- Monitoring and alerting for system health
- Gradual user migration with ability to revert

## Performance Considerations

### Chat Memory Optimization
- Implement configurable message history limits
- Use efficient storage mechanisms for conversation context
- Implement cleanup strategies for old conversations

### Tool Execution Optimization
- Cache frequently used prompts
- Optimize database queries in tools
- Implement async processing where appropriate

### Rate Limiting Integration
- Ensure rate limiting checks are performed before expensive operations
- Optimize rate limit calculations
- Implement efficient tracking mechanisms

## Security Considerations

### Authentication and Authorization
- Maintain existing JWT-based authentication
- Ensure user context is properly passed to all tools
- Validate user permissions for data access

### Data Privacy
- Ensure conversation history is properly isolated by user
- Implement data retention policies
- Secure transmission of health data

### Input Validation
- Validate all user inputs through advisors
- Sanitize data before processing
- Implement rate limiting to prevent abuse

## Monitoring and Observability (Future Enhancement)

### Metrics (Optional - Future Implementation)
- Tool execution times and success rates
- Conversation flow analytics
- Rate limiting effectiveness
- User engagement patterns

### Logging (Optional - Future Implementation)
- Structured logging for all tool executions
- Conversation flow tracing
- Error tracking and alerting
- Performance monitoring

### Health Checks (Optional - Future Implementation)
- Spring AI component health
- Chat memory service health
- Tool availability monitoring
- Database connectivity checks