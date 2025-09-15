# Requirements Document

## Introduction

The Hi-Doc API Service is a Java Spring Boot application that serves as a secure API layer for the Hi-Doc Flutter application. The primary purpose is to provide subscriber management capabilities for users who purchase the app from app stores and act as a secure proxy for AI service calls (ChatGPT, Grok, Gemini, etc.) while implementing rate limiting and usage tracking. This service ensures that AI service API keys are not embedded in the Flutter application, maintaining security and preventing exposure to external parties.

## Requirements

### Requirement 1: Subscriber Management System

**User Story:** As a Hi-Doc app administrator, I want to manage subscriber information and analytics, so that I can track user engagement and app store purchases effectively.

#### Acceptance Criteria

1. WHEN a new user purchases the app from an app store THEN the system SHALL store their subscriber details including user ID, purchase date, app store platform, and subscription status
2. WHEN subscriber information is requested THEN the system SHALL provide user details for analytics purposes without exposing sensitive personal information
3. WHEN a subscriber's status changes THEN the system SHALL update their record with the new status and timestamp
4. IF a subscriber record already exists THEN the system SHALL update existing information rather than create duplicates
5. WHEN subscriber analytics are requested THEN the system SHALL provide aggregated data including total subscribers, platform distribution, and subscription trends

### Requirement 2: AI Service Proxy and Rate Limiting

**User Story:** As a Hi-Doc app user, I want to make AI service requests through a secure proxy, so that my requests are processed without exposing API keys while staying within usage limits.

#### Acceptance Criteria

1. WHEN a user makes an AI service request THEN the system SHALL proxy the request to the appropriate AI service (ChatGPT, Grok, Gemini) without storing the user's request content
2. WHEN a user exceeds 100 requests per month THEN the system SHALL reject additional requests with an appropriate error message
3. WHEN an AI service request is made THEN the system SHALL increment the user's monthly request counter
4. WHEN a new month begins THEN the system SHALL reset all users' request counters to zero
5. IF an AI service is unavailable THEN the system SHALL return an appropriate error response without exposing internal service details
6. WHEN tracking usage THEN the system SHALL store only metadata (user ID, timestamp, service used, success/failure) and never store actual request content or responses

### Requirement 3: Multi-Provider AI Service Support

**User Story:** As a Hi-Doc app user, I want to access different AI services through the same API interface, so that I can choose the best service for my needs without changing my app integration.

#### Acceptance Criteria

1. WHEN the system receives a request for ChatGPT THEN it SHALL route the request to OpenAI's API using the configured API key
2. WHEN the system receives a request for Grok THEN it SHALL route the request to X.AI's API using the configured API key
3. WHEN the system receives a request for Gemini THEN it SHALL route the request to Google's Gemini API using the configured API key
4. WHEN adding a new AI provider THEN the system SHALL support it through a consistent controller interface
5. IF an AI provider's API key is invalid or missing THEN the system SHALL return an appropriate error without exposing the key details

### Requirement 4: OAuth Authentication Integration

**User Story:** As a Hi-Doc app user, I want to use my existing Microsoft/Google OAuth authentication for API calls, so that I don't need to authenticate again when making AI service requests.

#### Acceptance Criteria

1. WHEN a user makes an API request THEN the system SHALL validate the OAuth token provided by the Flutter app
2. WHEN validating OAuth tokens THEN the system SHALL verify tokens with Microsoft or Google OAuth providers as appropriate
3. WHEN a valid OAuth token is provided THEN the system SHALL extract user identity and associate it with the request
4. IF an OAuth token is invalid or expired THEN the system SHALL return appropriate authentication error responses
5. WHEN processing AI requests THEN the system SHALL use the authenticated user's identity for rate limiting and usage tracking
6. WHEN OAuth token validation fails THEN the system SHALL not process the AI request and return authentication errors

### Requirement 5: Security and API Key Management

**User Story:** As a system administrator, I want AI service API keys to be securely managed on the server side, so that they are never exposed to client applications or external parties.

#### Acceptance Criteria

1. WHEN the system starts THEN it SHALL load AI service API keys from secure environment variables or configuration files
2. WHEN processing AI requests THEN the system SHALL use stored API keys without exposing them in responses or logs
3. WHEN API keys are rotated THEN the system SHALL support hot-reloading of configuration without service restart
4. IF API keys are missing or invalid THEN the system SHALL log appropriate warnings without exposing the actual key values
5. WHEN handling errors THEN the system SHALL never include API keys or sensitive configuration in error responses

### Requirement 6: Health Data Processing Endpoints

**User Story:** As a Hi-Doc app user, I want to process health messages and store health data entries, so that I can track my health information through AI-assisted interpretation.

#### Acceptance Criteria

1. WHEN a health message interpretation request is received THEN the system SHALL process it using the configured AI service and return structured health data
2. WHEN health data needs to be stored THEN the system SHALL save it to the database and return the stored entry with assigned ID
3. WHEN processing health messages with prompts THEN the system SHALL load prompts from server-side files and apply them to AI requests
4. IF health data processing fails THEN the system SHALL return appropriate error messages without exposing internal processing details

### Requirement 7: Modern Java and Spring Boot Implementation

**User Story:** As a system administrator, I want the API service to use modern Java features and Spring Boot, so that I can benefit from the latest performance improvements and development practices.

#### Acceptance Criteria

1. WHEN building the application THEN it SHALL use Java 25 with virtual threading enabled for optimal performance
2. WHEN Java 25 packages are incompatible THEN the system SHALL fall back to JDK 21 with appropriate Spring Boot version
3. WHEN handling concurrent requests THEN the system SHALL leverage virtual threading for improved scalability
4. WHEN selecting Spring Boot version THEN it SHALL use the latest version compatible with the chosen Java version
5. IF virtual threading is not available THEN the system SHALL gracefully fall back to traditional threading models

### Requirement 8: Single Bundle Deployment

**User Story:** As a system administrator, I want to deploy the Hi-Doc API service as a single executable JAR file, so that deployment and maintenance are simplified across different environments.

#### Acceptance Criteria

1. WHEN building the application THEN the system SHALL create a single executable JAR file containing all dependencies
2. WHEN deploying the service THEN it SHALL require only Java runtime environment and configuration files
3. WHEN the service starts THEN it SHALL initialize all required components including database connections and AI service configurations
4. WHEN configuration changes are needed THEN they SHALL be possible through external configuration files or environment variables
5. IF the service fails to start THEN it SHALL provide clear error messages indicating the cause of failure

### Requirement 9: PostgreSQL Database Integration and Schema Management

**User Story:** As a system administrator, I want the API service to use PostgreSQL database with proper schema management, so that I can ensure data persistence and perform user analytics effectively.

#### Acceptance Criteria

1. WHEN the system starts THEN it SHALL connect to PostgreSQL v17 database at postgres.tailce422e.ts.net
2. WHEN deploying the system THEN it SHALL include SQL scripts to create all required tables including users, usage_tracking, and analytics tables
3. WHEN database operations are performed THEN the system SHALL use connection pooling for optimal performance
4. WHEN database schema changes are needed THEN the system SHALL support migration scripts for version management
5. IF database connection fails THEN the system SHALL provide clear error messages and retry mechanisms

### Requirement 10: User Analytics and Reporting

**User Story:** As a system administrator, I want comprehensive user analytics and reporting capabilities, so that I can track usage patterns and make data-driven decisions.

#### Acceptance Criteria

1. WHEN users make AI requests THEN the system SHALL track usage metrics including request count, service type, and timestamp
2. WHEN generating analytics reports THEN the system SHALL provide insights on user engagement, popular AI services, and usage trends
3. WHEN storing analytics data THEN the system SHALL ensure user privacy by storing only aggregated and anonymized metrics
4. WHEN querying analytics THEN the system SHALL support filtering by date ranges, user segments, and service types
5. IF analytics queries are complex THEN the system SHALL optimize database queries for performance

### Requirement 11: Health Check and Monitoring

**User Story:** As a system administrator, I want comprehensive health monitoring and status endpoints, so that I can ensure system reliability and troubleshoot issues effectively.

#### Acceptance Criteria

1. WHEN a health check is requested THEN the system SHALL verify PostgreSQL connectivity and AI service availability
2. WHEN monitoring system health THEN administrators SHALL be able to check service status through dedicated health endpoints
3. WHEN database operations fail THEN the system SHALL handle errors gracefully and provide appropriate error responses
4. WHEN critical components are unavailable THEN the health check SHALL indicate the specific component status and error details
5. IF system performance degrades THEN monitoring endpoints SHALL provide metrics on response times and error rates