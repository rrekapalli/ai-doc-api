package com.hidoc.api.repository;

import com.hidoc.api.domain.AnalyticsSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AnalyticsRepository extends JpaRepository<AnalyticsSummary, Long> {

    @Query(value = "SELECT COUNT(*) FROM usage_tracking WHERE request_timestamp BETWEEN :from AND :to", nativeQuery = true)
    long countTotalBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = "SELECT COUNT(DISTINCT user_id) FROM usage_tracking WHERE request_timestamp BETWEEN :from AND :to", nativeQuery = true)
    long countDistinctUsersBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = "SELECT ai_provider, COUNT(*) FROM usage_tracking WHERE request_timestamp BETWEEN :from AND :to GROUP BY ai_provider", nativeQuery = true)
    List<Object[]> countByProviderBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = "SELECT DATE(request_timestamp) as day, COUNT(*) FROM usage_tracking WHERE request_timestamp BETWEEN :from AND :to GROUP BY day ORDER BY day", nativeQuery = true)
    List<Object[]> dailyUsageCountsBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = "SELECT DATE(request_timestamp) as day, COUNT(*) FROM usage_tracking WHERE ai_provider = :provider AND request_timestamp BETWEEN :from AND :to GROUP BY day ORDER BY day", nativeQuery = true)
    List<Object[]> dailyUsageCountsByProviderBetween(@Param("provider") String provider, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
