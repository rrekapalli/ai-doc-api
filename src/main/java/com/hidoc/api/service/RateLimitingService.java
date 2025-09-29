package com.hidoc.api.service;

import com.hidoc.api.domain.UsageTracking;
import com.hidoc.api.repository.UsageTrackingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import jakarta.persistence.PersistenceException;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Optional;

@Service
public class RateLimitingService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingService.class);

    private final UsageTrackingRepository usageTrackingRepository;

    @Value("${rate-limiting.monthly-limit:100}")
    private int monthlyLimit = 100;

    public RateLimitingService(UsageTrackingRepository usageTrackingRepository) {
        this.usageTrackingRepository = usageTrackingRepository;
    }

    // Preferred: email-based check
    public boolean isRequestAllowedByEmail(String email) {
        String month = currentMonthYear();
        try {
            long used = usageTrackingRepository.countByEmailAndMonth(email, month);
            return used < monthlyLimit;
        } catch (RuntimeException ex) {
            log.warn("Rate limit check skipped due to repository error for email={} month={} cause={}", email, month, ex.getMessage());
            return true;
        }
    }

    // Legacy: userId-based check (kept for backward compatibility)
    public boolean isRequestAllowed(String userId) {
        String month = currentMonthYear();
        try {
            long used = usageTrackingRepository.countByUserAndMonth(userId, month);
            return used < monthlyLimit;
        } catch (RuntimeException ex) {
            log.warn("Rate limit check skipped due to repository error for user_id={} month={} cause={}", userId, month, ex.getMessage());
            return true;
        }
    }

    // Preferred: record by email
    @org.springframework.cache.annotation.CacheEvict(cacheNames = "usageStats", key = "#p0")
    public void recordRequestByEmail(String email, String provider, boolean success, String errorMessage) {
        String month = currentMonthYear();
        try {
            Optional<UsageTracking> existingOpt = usageTrackingRepository.findFirstByEmailAndMonthYearOrderByIdAsc(email, month);
            UsageTracking rec = existingOpt.orElseGet(UsageTracking::new);
            if (existingOpt.isEmpty()) {
                rec.setEmail(email);
                rec.setMonthYear(month);
                rec.setRequestCount(0);
            }
            rec.setAiProvider(provider);
            rec.setRequestTimestamp(LocalDateTime.now());
            if (success) {
                rec.setSuccess(true);
                rec.setErrorMessage(null);
                rec.setRequestCount(rec.getRequestCount() + 1);
            } else {
                rec.setSuccess(false);
                rec.setErrorMessage(truncate(errorMessage, 500));
            }
            usageTrackingRepository.save(rec);
        } catch (DataIntegrityViolationException ex) {
            String details = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
            log.warn("Usage record skipped: subscriber missing for email={} provider={} cause={}", email, provider, details);
        } catch (PersistenceException ex) {
            String cause = ex.getMessage() != null ? ex.getMessage() : "Persistence error";
            if (cause.toLowerCase().contains("fk_usage_email")) {
                log.warn("Usage record skipped due to FK (subscriber missing): email={} provider={} cause={}", email, provider, cause);
                return;
            }
            throw ex;
        }
    }

    // Legacy method: record by userId
    @org.springframework.cache.annotation.CacheEvict(cacheNames = "usageStats", key = "#p0")
    public void recordRequest(String userId, String provider, boolean success, String errorMessage) {
        String month = currentMonthYear();
        try {
            Optional<UsageTracking> existingOpt = usageTrackingRepository.findFirstByUserIdAndMonthYearOrderByIdAsc(userId, month);
            UsageTracking rec = existingOpt.orElseGet(UsageTracking::new);
            if (existingOpt.isEmpty()) {
                rec.setUserId(userId);
                rec.setMonthYear(month);
                rec.setRequestCount(0);
            }
            rec.setAiProvider(provider);
            rec.setRequestTimestamp(LocalDateTime.now());
            if (success) {
                rec.setSuccess(true);
                rec.setErrorMessage(null);
                rec.setRequestCount(rec.getRequestCount() + 1);
            } else {
                rec.setSuccess(false);
                rec.setErrorMessage(truncate(errorMessage, 500));
            }
            usageTrackingRepository.save(rec);
        } catch (DataIntegrityViolationException ex) {
            String details = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
            log.warn("Usage record skipped: subscriber missing for user_id={} provider={} cause={}", userId, provider, details);
        } catch (PersistenceException ex) {
            String cause = ex.getMessage() != null ? ex.getMessage() : "Persistence error";
            if (cause.toLowerCase().contains("fk_usage_user")) {
                log.warn("Usage record skipped due to FK (subscriber missing): user_id={} provider={} cause={}", userId, provider, cause);
                return;
            }
            throw ex;
        }
    }

    @org.springframework.cache.annotation.Cacheable(cacheNames = "usageStats", key = "#p0")
    public UsageStats getUserUsageStats(String userId) {
        String month = currentMonthYear();
        long used = usageTrackingRepository.countByUserAndMonth(userId, month);
        return new UsageStats(used, Math.max(0, monthlyLimit - used), monthlyLimit, month);
    }

    @org.springframework.cache.annotation.Cacheable(cacheNames = "usageStats", key = "#p0")
    public UsageStats getUsageStatsByEmail(String email) {
        String month = currentMonthYear();
        long used = usageTrackingRepository.countByEmailAndMonth(email, month);
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
