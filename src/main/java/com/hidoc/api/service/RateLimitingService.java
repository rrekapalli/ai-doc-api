package com.hidoc.api.service;

import com.hidoc.api.domain.UsageTracking;
import com.hidoc.api.repository.UsageTrackingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.YearMonth;

@Service
public class RateLimitingService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingService.class);

    private final UsageTrackingRepository usageTrackingRepository;

    @Value("${rate-limiting.monthly-limit:100}")
    private int monthlyLimit = 100;

    public RateLimitingService(UsageTrackingRepository usageTrackingRepository) {
        this.usageTrackingRepository = usageTrackingRepository;
    }

    public boolean isRequestAllowed(String userId) {
        String month = currentMonthYear();
        long used = usageTrackingRepository.countByUserAndMonth(userId, month);
        return used < monthlyLimit;
    }

    @org.springframework.cache.annotation.CacheEvict(cacheNames = "usageStats", key = "#userId")
    public void recordRequest(String userId, String provider, boolean success, String errorMessage) {
        UsageTracking rec = new UsageTracking();
        rec.setUserId(userId);
        rec.setAiProvider(provider);
        rec.setSuccess(success);
        rec.setErrorMessage(success ? null : truncate(errorMessage, 500));
        rec.setMonthYear(currentMonthYear());
        usageTrackingRepository.save(rec);
    }

    @org.springframework.cache.annotation.Cacheable(cacheNames = "usageStats", key = "#userId")
    public UsageStats getUserUsageStats(String userId) {
        String month = currentMonthYear();
        long used = usageTrackingRepository.countByUserAndMonth(userId, month);
        return new UsageStats(used, Math.max(0, monthlyLimit - used), monthlyLimit, month);
    }

    public String currentMonthYear() {
        return YearMonth.now().toString(); // format YYYY-MM
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, max);
    }

    public record UsageStats(long used, long remaining, long limit, String monthYear) {}
}
