package com.hidoc.api.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usage_tracking")
public class UsageTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", length = 255)
    private String userId; // legacy, may be null

    @Column(name = "email", length = 255)
    private String email; // new FK to subscribers(email)

    @Column(name = "ai_provider", nullable = false, length = 50)
    private String aiProvider;

    @Column(name = "request_timestamp")
    private LocalDateTime requestTimestamp = LocalDateTime.now();

    @Column(nullable = false)
    private boolean success = true;

    @Column(name = "request_count", nullable = false)
    private Integer requestCount = 1; // Use wrapper to avoid null assignment errors

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "month_year", nullable = false, length = 7)
    private String monthYear;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAiProvider() { return aiProvider; }
    public void setAiProvider(String aiProvider) { this.aiProvider = aiProvider; }
    public LocalDateTime getRequestTimestamp() { return requestTimestamp; }
    public void setRequestTimestamp(LocalDateTime requestTimestamp) { this.requestTimestamp = requestTimestamp; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public int getRequestCount() { return requestCount == null ? 0 : requestCount; }
    public void setRequestCount(Integer requestCount) { this.requestCount = requestCount; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getMonthYear() { return monthYear; }
    public void setMonthYear(String monthYear) { this.monthYear = monthYear; }
}
