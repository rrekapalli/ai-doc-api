package com.hidoc.api.service;

import com.hidoc.api.repository.AnalyticsRepository;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepository;

    public AnalyticsService(AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    public AnalyticsSummaryReport getSummary(LocalDateTime from, LocalDateTime to) {
        var range = normalizeRange(from, to);
        long total = analyticsRepository.countTotalBetween(range.from(), range.to());
        long uniqueUsers = analyticsRepository.countDistinctUsersBetween(range.from(), range.to());
        List<Object[]> byProvider = analyticsRepository.countByProviderBetween(range.from(), range.to());
        Map<String, Long> providerCounts = new LinkedHashMap<>();
        for (Object[] row : byProvider) {
            String provider = Objects.toString(row[0], "UNKNOWN");
            Long count = ((Number) row[1]).longValue();
            providerCounts.put(provider, count);
        }
        return new AnalyticsSummaryReport(total, uniqueUsers, providerCounts, range.from(), range.to());
    }

    public List<TrendPoint> getDailyTrends(LocalDateTime from, LocalDateTime to, String provider) {
        var range = normalizeRange(from, to);
        List<Object[]> rows;
        if (provider == null || provider.isBlank()) {
            rows = analyticsRepository.dailyUsageCountsBetween(range.from(), range.to());
        } else {
            rows = analyticsRepository.dailyUsageCountsByProviderBetween(provider, range.from(), range.to());
        }
        List<TrendPoint> result = new ArrayList<>();
        for (Object[] row : rows) {
            // row[0] is java.sql.Date (from DATE()), row[1] is count
            LocalDate day;
            Object dayObj = row[0];
            if (dayObj instanceof Date d) {
                day = d.toLocalDate();
            } else if (dayObj instanceof java.util.Date ud) {
                day = ud.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
            } else if (dayObj instanceof LocalDate ld) {
                day = ld;
            } else {
                day = LocalDate.parse(dayObj.toString());
            }
            long count = ((Number) row[1]).longValue();
            result.add(new TrendPoint(day, count));
        }
        return result;
    }

    private DateRange normalizeRange(LocalDateTime from, LocalDateTime to) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = (to == null) ? now : to;
        LocalDateTime start = (from == null) ? end.minusDays(30) : from;
        if (start.isAfter(end)) {
            // swap
            LocalDateTime tmp = start;
            start = end;
            end = tmp;
        }
        return new DateRange(start, end);
    }

    public record AnalyticsSummaryReport(long totalRequests, long uniqueUsers, Map<String, Long> requestsByProvider,
                                         LocalDateTime from, LocalDateTime to) {}
    public record TrendPoint(LocalDate day, long count) {}
    private record DateRange(LocalDateTime from, LocalDateTime to) {}
}
