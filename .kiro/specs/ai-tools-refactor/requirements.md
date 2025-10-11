# Requirements Document

## Introduction

This document outlines the requirements for refactoring the AI-Doc-API service to leverage the latest Spring AI features including @Tool annotations, Chat Orchestrators, Advisors, and Chat Memory. The refactor will modernize the existing health tracking application's AI capabilities while maintaining backward compatibility with existing REST endpoints and preserving all current functionality.

## Requirements

### Requirement 1

**User Story:** As a developer, I want to modernize the AI infrastructure to use Spring AI's @Tool annotations, so that the system can leverage advanced features like chat orchestration, advisors, and conversation memory.

#### Acceptance Criteria

1. WHEN the system is refactored THEN it SHALL use Spring AI's @Tool annotations for all health-related AI tools
2. WHEN the system processes user messages THEN it SHALL maintain the existing REST API endpoints without breaking changes
3. WHEN the system is deployed THEN it SHALL preserve all existing functionality while adding new Spring AI capabilities
4. WHEN tools are implemented THEN they SHALL correspond to the existing prompt classifications: medication data entry, activity data entry, health condition data entry, food intake, mood data entry, health parameter data entry, medical queries, report parsing, and health trend analysis

### Requirement 2

**User Story:** As a user, I want to have conversational interactions with the AI system, so that I can maintain context across multiple messages and receive more personalized health insights.

#### Acceptance Criteria

1. WHEN a user sends multiple messages THEN the system SHALL maintain conversation history and context
2. WHEN processing user input THEN the system SHALL consider previous conversation context for better responses
3. WHEN storing conversations THEN the system SHALL implement Spring AI's Chat Memory capabilities
4. WHEN a conversation spans multiple interactions THEN the system SHALL provide contextually relevant responses based on chat history

### Requirement 3

**User Story:** As a user, I want a central chat orchestrator to intelligently route my health-related messages, so that I receive appropriate responses regardless of the complexity or type of my query.

#### Acceptance Criteria

1. WHEN a user sends a message THEN the system SHALL use a central chat orchestrator to classify and route the message
2. WHEN message classification occurs THEN the orchestrator SHALL determine the appropriate AI tool for processing
3. WHEN routing decisions are made THEN the system SHALL support all existing health categories: medication, activity, health conditions, food intake, mood, health parameters, medical queries, reports, and trend analysis
4. WHEN the orchestrator processes messages THEN it SHALL maintain the existing prompt-based classification logic
5. WHEN routing to tools THEN the system SHALL use Spring AI's advisor pattern for enhanced processing
6. WHEN a user sends non-health related queries THEN the system SHALL reject them with a gentle and cordial message using a generic @Tool
7. WHEN rejecting non-health related queries THEN the system SHALL NOT deduct these requests from the user's rate limit

### Requirement 4

**User Story:** As a user, I want the system to use existing health-related prompts for message processing, so that the quality and accuracy of AI responses are maintained during the refactor.

#### Acceptance Criteria

1. WHEN processing health messages THEN the system SHALL use prompts from the `src/main/resources/prompts` directory including new Spring AI tool prompts
2. WHEN classifying messages THEN the system SHALL use the new `spring_ai_message_classifier_tool.txt` for Spring AI @Tool routing
3. WHEN processing specific health categories THEN the system SHALL use corresponding specialized tool prompts (medication_data_entry_prompt.txt, health_data_entry_prompt.txt, mood_data_entry_tool.txt, food_intake_tool.txt, etc.)
4. WHEN generating responses THEN the system SHALL return a standardized JSON structure as defined in spring_ai_master_prompt.txt containing classification, response, inference, data, dateTime, messageId, conversationId, availableRequests, and userId fields
5. WHEN handling medical queries THEN the system SHALL use medical_query_elaboration_prompt.txt and preserve medical disclaimer and response structure within the response field

### Requirement 5

**User Story:** As a user, I want the system to maintain all existing REST endpoints, so that my current integrations and client applications continue to work without modification.

#### Acceptance Criteria

1. WHEN the refactor is complete THEN the system SHALL maintain the existing `/api/ai/chat` endpoint
2. WHEN processing requests THEN the system SHALL accept the same request payload format as the current implementation
3. WHEN returning responses THEN the system SHALL extend the existing AIResponse format with additional fields while preserving backward compatibility
4. WHEN updating the AIRequest THEN the system SHALL add a messageHistory field that can contain the last N user messages (configurable, default 100)
5. WHEN handling authentication THEN the system SHALL preserve the current user authentication and authorization mechanisms
6. WHEN rate limiting is applied THEN the system SHALL maintain the existing rate limiting functionality
7. WHEN a user exceeds the monthly question limit THEN the system SHALL enforce the configurable rate limit (default 100 questions per month)
8. WHEN rate limiting is enforced THEN the system SHALL use the existing RateLimitingService infrastructure

### Requirement 6

**User Story:** As a system administrator, I want the refactored system to integrate with existing infrastructure, so that deployment and maintenance remain consistent with current practices.

#### Acceptance Criteria

1. WHEN the system is deployed THEN it SHALL use the existing database schema and data models
2. WHEN storing conversation data THEN it SHALL integrate with the existing messages table structure
3. WHEN processing health data THEN it SHALL use existing data tables (health_data, medications, activities, reports)
4. WHEN handling errors THEN it SHALL maintain the existing error handling and logging mechanisms
5. WHEN rate limiting is enforced THEN it SHALL integrate with the existing RateLimitingService

### Requirement 7

**User Story:** As a user, I want AI tools to handle specific health data categories, so that I can efficiently record and query different types of health information.

#### Acceptance Criteria

1. WHEN implementing medication tools THEN the system SHALL create @Tool annotated methods for medication data entry and queries
2. WHEN implementing activity tools THEN the system SHALL create @Tool annotated methods for physical activity tracking
3. WHEN implementing health parameter tools THEN the system SHALL create @Tool annotated methods for vital signs and health metrics
4. WHEN implementing medical query tools THEN the system SHALL create @Tool annotated methods for symptom analysis and medical information
5. WHEN implementing report processing tools THEN the system SHALL create @Tool annotated methods for medical report parsing
6. WHEN implementing trend analysis tools THEN the system SHALL create @Tool annotated methods using trend_analysis_tool.txt for health pattern analysis
7. WHEN implementing mood tracking tools THEN the system SHALL create @Tool annotated methods using mood_data_entry_tool.txt for mood data entry
8. WHEN implementing food intake tools THEN the system SHALL create @Tool annotated methods using food_intake_tool.txt for nutrition tracking
9. WHEN implementing query filtering THEN the system SHALL create a generic @Tool using non_health_rejection_tool.txt to reject non-health related queries with gentle, cordial messages
10. WHEN handling trend analysis requests THEN the system SHALL respond with classification 'TREND_ANALYSIS' and include the category of data requested
11. WHEN processing historic data requests THEN the system SHALL provide guidance to help the client determine what data to pull from the local SQLite database
12. WHEN generating trend analysis responses THEN the inference field SHALL indicate whether the request is for data display, chart visualization, or if a followup is needed
13. WHEN a followup is required THEN the system SHALL set isFollowUp to true and specify the required data type in followUpDataRequired field
14. WHEN implementing prognosis tools THEN the system SHALL create a separate @Tool using prognosis_tool.txt that accepts historic data organized by categories (HEALTH_PARAM, MEDICATION_ENTRY, ACTIVITY, etc.)
15. WHEN determining prognosis intent THEN the system SHALL identify when TREND_ANALYSIS requests require comprehensive health analysis and include followup messages with PROGNOSIS classification
16. WHEN followup data is provided for prognosis THEN the system SHALL generate a comprehensive prognosis response based on the historic data provided

### Requirement 8

**User Story:** As a developer, I want the system to use Spring AI's advisor pattern, so that AI processing can be enhanced with cross-cutting concerns like validation, logging, and response formatting.

#### Acceptance Criteria

1. WHEN processing AI requests THEN the system SHALL implement Spring AI advisors for request preprocessing
2. WHEN generating AI responses THEN the system SHALL implement advisors for response post-processing
3. WHEN handling medical queries THEN advisors SHALL ensure proper medical disclaimer inclusion
4. WHEN processing data entries THEN advisors SHALL validate data format and completeness
5. WHEN errors occur THEN advisors SHALL handle error formatting and logging consistently

### Requirement 9

**User Story:** As a user, I want the system to provide enhanced response format, so that client applications can access richer AI response data while maintaining backward compatibility.

#### Acceptance Criteria

1. WHEN updating the AIResponse class THEN the system SHALL add new fields: classification, inference, data, dateTime, messageId, conversationId, userId, availableRequests, isFollowUp, and followUpDataRequired
2. WHEN maintaining backward compatibility THEN the system SHALL preserve existing AIResponse fields (response, model, tokensUsed)
3. WHEN populating the availableRequests field THEN the system SHALL show how many requests are available after the current one is deducted from the user's rate limit
4. WHEN providing health information THEN the response field SHALL contain HTML formatted content with semantic classes (medical-response, ranges-box, alert-box, warning-box, disclaimer)
5. WHEN processing data entries THEN the data field SHALL contain structured JSON data for tables or charts when applicable
6. WHEN classifying messages THEN the classification field SHALL use standardized values like 'HEALTH_PARAM', 'MEDICATION_ENTRY', 'ACTIVITY', etc.
7. WHEN including medical disclaimers THEN the system SHALL use the existing disclaimer text and formatting within the response field
8. WHEN generating unique identifiers THEN the system SHALL create UUID values for messageId and conversationId fields

### Requirement 10

**User Story:** As a developer, I want to implement a bare minimum viable refactor, so that the core Spring AI functionality can be delivered quickly while deferring advanced infrastructure features.

#### Acceptance Criteria

1. WHEN implementing the refactor THEN the system SHALL focus on core Spring AI features (@Tool annotations, Chat Orchestrators, Advisors, Chat Memory)
2. WHEN prioritizing features THEN the system SHALL defer advanced infrastructure tasks like telemetry, observability, and caching for later implementation
3. WHEN implementing testing THEN comprehensive testing SHALL be marked as optional and can be implemented in future iterations
4. WHEN building the solution THEN the system SHALL provide a working MVP that demonstrates all core Spring AI capabilities
5. WHEN documenting optional features THEN the system SHALL clearly identify which infrastructure components can be added later