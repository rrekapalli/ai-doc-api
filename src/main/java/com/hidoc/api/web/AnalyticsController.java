package com.hidoc.api.web;

import com.hidoc.api.service.AnalyticsService;
import com.hidoc.api.service.AnalyticsService.AnalyticsSummaryReport;
import com.hidoc.api.service.AnalyticsService.TrendPoint;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public ResponseEntity<AnalyticsSummaryReport> getSummary(
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        AnalyticsSummaryReport report = analyticsService.getSummary(from, to);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/trends")
    public ResponseEntity<List<TrendPoint>> getTrends(
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(value = "provider", required = false) String provider) {
        List<TrendPoint> points = analyticsService.getDailyTrends(from, to, provider);
        return ResponseEntity.ok(points);
    }
}
