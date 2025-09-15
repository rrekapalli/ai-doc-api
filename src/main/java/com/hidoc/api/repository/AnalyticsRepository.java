package com.hidoc.api.repository;

import com.hidoc.api.domain.AnalyticsSummary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalyticsRepository extends JpaRepository<AnalyticsSummary, Long> {
}
