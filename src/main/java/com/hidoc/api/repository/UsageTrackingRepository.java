package com.hidoc.api.repository;

import com.hidoc.api.domain.UsageTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsageTrackingRepository extends JpaRepository<UsageTracking, Long> {

    // New email-based aggregations
    @Query("SELECT COALESCE(SUM(u.requestCount), 0) FROM UsageTracking u WHERE u.email = :email AND u.monthYear = :monthYear AND u.success = true")
    long sumSuccessfulByEmailAndMonth(@Param("email") String email, @Param("monthYear") String monthYear);

    @Query("SELECT COALESCE(SUM(u.requestCount), 0) FROM UsageTracking u WHERE u.email = :email AND u.monthYear = :monthYear AND u.success = true")
    long countByEmailAndMonth(@Param("email") String email, @Param("monthYear") String monthYear);

    Optional<UsageTracking> findFirstByEmailAndMonthYearOrderByIdAsc(String email, String monthYear);

    // Legacy userId-based methods (deprecated)
    @Deprecated
    @Query("SELECT COALESCE(SUM(u.requestCount), 0) FROM UsageTracking u WHERE u.userId = :userId AND u.monthYear = :monthYear AND u.success = true")
    long sumSuccessfulByUserAndMonth(@Param("userId") String userId, @Param("monthYear") String monthYear);

    @Deprecated
    @Query("SELECT COALESCE(SUM(u.requestCount), 0) FROM UsageTracking u WHERE u.userId = :userId AND u.monthYear = :monthYear AND u.success = true")
    long countByUserAndMonth(@Param("userId") String userId, @Param("monthYear") String monthYear);

    @Deprecated
    Optional<UsageTracking> findFirstByUserIdAndMonthYearOrderByIdAsc(String userId, String monthYear);
}
