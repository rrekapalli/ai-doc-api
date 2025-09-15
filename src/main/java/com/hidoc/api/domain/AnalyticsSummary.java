package com.hidoc.api.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "analytics_summary")
public class AnalyticsSummary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "metric_type", nullable = false, length = 100)
    private String metricType;

    @Column(name = "metric_value", nullable = false)
    private Long metricValue;

    @Column(name = "dimension_1", length = 100)
    private String dimension1;

    @Column(name = "dimension_2", length = 100)
    private String dimension2;

    @Column(name = "period_start", nullable = false)
    private LocalDateTime periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDateTime periodEnd;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMetricType() { return metricType; }
    public void setMetricType(String metricType) { this.metricType = metricType; }
    public Long getMetricValue() { return metricValue; }
    public void setMetricValue(Long metricValue) { this.metricValue = metricValue; }
    public String getDimension1() { return dimension1; }
    public void setDimension1(String dimension1) { this.dimension1 = dimension1; }
    public String getDimension2() { return dimension2; }
    public void setDimension2(String dimension2) { this.dimension2 = dimension2; }
    public LocalDateTime getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDateTime periodStart) { this.periodStart = periodStart; }
    public LocalDateTime getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDateTime periodEnd) { this.periodEnd = periodEnd; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
