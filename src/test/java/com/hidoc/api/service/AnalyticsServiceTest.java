package com.hidoc.api.service;

import com.hidoc.api.repository.AnalyticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AnalyticsServiceTest {

    private AnalyticsRepository repo;
    private AnalyticsService service;

    @BeforeEach
    void setup() {
        repo = mock(AnalyticsRepository.class);
        service = new AnalyticsService(repo);
    }

    @Test
    void getSummary_shouldAggregateCounts() {
        when(repo.countTotalBetween(any(), any())).thenReturn(123L);
        when(repo.countDistinctUsersBetween(any(), any())).thenReturn(10L);
        when(repo.countByProviderBetween(any(), any())).thenReturn(List.of(
                new Object[]{"OPENAI", 100L},
                new Object[]{"GROK", 20L},
                new Object[]{"GEMINI", 3L}
        ));

        AnalyticsService.AnalyticsSummaryReport report = service.getSummary(null, null);
        assertThat(report.totalRequests()).isEqualTo(123);
        assertThat(report.uniqueUsers()).isEqualTo(10);
        assertThat(report.requestsByProvider()).containsAllEntriesOf(Map.of(
                "OPENAI", 100L,
                "GROK", 20L,
                "GEMINI", 3L
        ));
    }

    @Test
    void getDailyTrends_shouldMapRows() {
        LocalDateTime from = LocalDateTime.now().minusDays(2);
        LocalDateTime to = LocalDateTime.now();
        when(repo.dailyUsageCountsBetween(any(), any())).thenReturn(List.of(
                new Object[]{java.sql.Date.valueOf(LocalDate.now().minusDays(1)), 5L},
                new Object[]{java.sql.Date.valueOf(LocalDate.now()), 7L}
        ));

        var points = service.getDailyTrends(from, to, null);
        assertThat(points).hasSize(2);
        assertThat(points.get(0).day()).isEqualTo(LocalDate.now().minusDays(1));
        assertThat(points.get(0).count()).isEqualTo(5);
        assertThat(points.get(1).count()).isEqualTo(7);
    }
}
