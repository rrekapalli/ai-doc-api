# Implementation Plan

- [x] 1. Set up Spring AI infrastructure and core models
  - Add Spring AI dependencies to pom.xml
  - Create Spring AI configuration classes
  - Update AIRequest model to include messageHistory field
  - Update AIResponse model with new fields (classification, inference, data, dateTime, messageId, conversationId, userId, availableRequests, isFollowUp, followUpDataRequired)
  - _Requirements: 1.1, 1.2, 1.3, 5.4, 9.1, 9.2, 9.3_

- [ ] 2. Implement chat memory and conversation management
  - Create ConversationContext model for managing chat sessions
  - Implement ChatMemoryService using Spring AI's ChatMemory interface
  - Create conversation ID generation and management utilities
  - Configure message history limits (default 100 messages)
  - _Requirements: 2.1, 2.2, 2.3, 2.4_

- [ ] 3. Create core orchestrator and prompt service
  - Implement HealthChatOrchestrator as the central chat coordinator
  - Create PromptService to load and manage prompts from resources/prompts directory including new Spring AI tool prompts
  - Integrate ChatClient with orchestrator
  - Set up basic conversation flow management
  - _Requirements: 3.1, 3.2, 4.1, 4.2_

- [ ] 4. Implement message classification tool
  - Create MessageClassifierTool using @Tool annotation
  - Integrate spring_ai_message_classifier_tool.txt logic for enhanced routing
  - Implement routing logic to determine appropriate specialized tools
  - Handle classification response format matching existing prompt structure
  - _Requirements: 3.1, 3.2, 3.4, 4.2, 4.3_

- [ ] 5. Implement health data entry tools
- [ ] 5.1 Create MedicationService with @Tool annotation
  - Implement medication data entry and query processing
  - Use updated medication_data_entry_prompt.txt with Spring AI format
  - Handle medication intake recording and medication information queries
  - _Requirements: 7.1, 4.3_

- [ ] 5.2 Create ActivityService with @Tool annotation
  - Implement physical activity tracking functionality
  - Use updated activity_data_entry_prompt.txt with Spring AI format
  - Handle activity data entry and activity-related queries
  - _Requirements: 7.2, 4.3_

- [ ] 5.3 Create HealthParameterService with @Tool annotation
  - Implement vital signs and health metrics processing
  - Use updated health_data_entry_prompt.txt with Spring AI format
  - Handle blood pressure, glucose, weight, and other health parameters
  - _Requirements: 7.3, 4.3_

- [ ] 5.4 Create MoodService with @Tool annotation
  - Implement mood data entry functionality using mood_data_entry_tool.txt
  - Create mood tracking and mood-related query handling with supportive responses
  - _Requirements: 7.7, 4.3_

- [ ] 5.5 Create FoodIntakeService with @Tool annotation
  - Implement nutrition tracking functionality using food_intake_tool.txt
  - Handle food intake data entry and nutrition queries with calorie estimation
  - _Requirements: 7.8, 4.3_

- [ ] 6. Implement medical query and report processing tools
- [ ] 6.1 Create MedicalQueryService with @Tool annotation
  - Implement symptom analysis and medical information queries
  - Use updated medical_query_elaboration_prompt.txt with Spring AI format and related prompts
  - Maintain existing HTML response format with semantic classes
  - Ensure medical disclaimers are included
  - _Requirements: 7.4, 4.3, 4.5, 9.4, 9.6_

- [ ] 6.2 Create ReportProcessingService with @Tool annotation
  - Implement medical report parsing functionality
  - Use existing reports_processing_prompt.txt
  - Handle report analysis and data extraction
  - _Requirements: 7.5, 4.3_

- [ ] 7. Implement trend analysis and prognosis tools
- [ ] 7.1 Create TrendAnalysisService with @Tool annotation
  - Implement health pattern analysis functionality using trend_analysis_tool.txt
  - Handle trend analysis requests with TREND_ANALYSIS classification
  - Implement followup logic for prognosis requests
  - Set isFollowUp and followUpDataRequired fields appropriately
  - Provide client guidance for data visualization and SQLite queries
  - _Requirements: 7.6, 7.10, 7.11, 7.12, 7.13_

- [ ] 7.2 Create PrognosisService with @Tool annotation
  - Implement comprehensive health prognosis functionality using prognosis_tool.txt
  - Accept historic data organized by categories (HEALTH_PARAM, MEDICATION_ENTRY, ACTIVITY, etc.)
  - Generate comprehensive prognosis responses based on provided historic data
  - Handle PROGNOSIS classification responses with health scoring and recommendations
  - _Requirements: 7.14, 7.15, 7.16_

- [ ] 8. Implement non-health query rejection tool
  - Create NonHealthRejectionTool with @Tool annotation using non_health_rejection_tool.txt
  - Implement gentle, cordial rejection messages for non-health queries
  - Ensure these requests are NOT deducted from rate limit
  - Provide contextual redirection suggestions based on query type
  - _Requirements: 3.6, 3.7, 7.9_

- [ ] 9. Implement Spring AI advisors
- [ ] 9.1 Create RequestPreprocessorAdvisor
  - Implement request validation and preprocessing
  - Validate user authentication and request format
  - _Requirements: 8.1, 8.4_

- [ ] 9.2 Create ResponsePostprocessorAdvisor
  - Implement response formatting and metadata addition
  - Add dateTime, messageId, conversationId, userId, and availableRequests fields
  - Calculate and populate availableRequests based on rate limiting
  - _Requirements: 8.2, 9.3, 9.8_

- [ ] 9.3 Create ValidationAdvisor
  - Implement data format and completeness validation
  - Ensure response structure compliance
  - _Requirements: 8.4_

- [ ] 9.4 Create MedicalDisclaimerAdvisor
  - Ensure proper medical disclaimer inclusion in medical query responses
  - Maintain existing disclaimer text and formatting
  - _Requirements: 8.3, 9.6_

- [ ] 10. Integrate with existing controller and rate limiting
  - Update UnifiedAIController to use HealthChatOrchestrator
  - Maintain existing endpoint structure and request/response compatibility
  - Integrate with existing RateLimitingService
  - Ensure rate limiting works correctly with new tool-based architecture
  - Handle rate limit calculations for availableRequests field
  - _Requirements: 5.1, 5.2, 5.3, 5.6, 5.7, 5.8, 6.1, 6.2, 6.3_

- [ ] 11. Implement response classification and data formatting
  - Ensure all tools return appropriate classification values (HEALTH_PARAM, MEDICATION_ENTRY, ACTIVITY, etc.)
  - Implement structured JSON data responses for tables and charts when applicable
  - Maintain HTML formatting with semantic classes for medical responses
  - Generate UUID values for messageId and conversationId fields
  - _Requirements: 9.4, 9.5, 9.6, 9.8_

- [ ] 12. Create configuration and setup classes
  - Create Spring AI configuration with ChatClient and ChatMemory beans
  - Configure advisor chain with all implemented advisors
  - Set up prompt loading configuration for all Spring AI tool prompts (spring_ai_master_prompt.txt, mood_data_entry_tool.txt, food_intake_tool.txt, trend_analysis_tool.txt, prognosis_tool.txt, non_health_rejection_tool.txt, etc.)
  - Create application properties for configurable values (message history limit, rate limits)
  - _Requirements: 1.1, 1.2, 1.3, 10.1, 10.4_

- [ ]* 13. Add comprehensive testing
- [ ]* 13.1 Create unit tests for all @Tool annotated services
  - Test individual tool functionality with mock data
  - Validate prompt processing and response formatting
  - _Requirements: 10.3_

- [ ]* 13.2 Create integration tests for orchestrator and advisors
  - Test complete conversation flows
  - Validate chat memory persistence and retrieval
  - Test advisor chain execution
  - _Requirements: 10.3_

- [ ]* 13.3 Create end-to-end tests for REST API
  - Test complete user journeys through UnifiedAIController
  - Validate backward compatibility with existing clients
  - Test error handling scenarios
  - _Requirements: 10.3_

- [ ]* 14. Add monitoring and observability infrastructure
- [ ]* 14.1 Implement structured logging for tool executions
  - Add logging for conversation flows and tool routing
  - Implement error tracking and alerting
  - _Requirements: 10.5_

- [ ]* 14.2 Create health checks for Spring AI components
  - Monitor ChatMemory service health
  - Check tool availability and performance
  - _Requirements: 10.5_

- [ ]* 14.3 Add performance monitoring and metrics
  - Track tool execution times and success rates
  - Monitor conversation flow analytics
  - _Requirements: 10.5_