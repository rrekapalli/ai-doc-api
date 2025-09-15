package com.hidoc.api.repository;

import com.hidoc.api.domain.UsageTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsageTrackingRepository extends JpaRepository<UsageTracking, Long> {

    @Query("SELECT COUNT(u) FROM UsageTracking u WHERE u.userId = :userId AND u.monthYear = :monthYear")
    long countByUserAndMonth(@Param("userId") String userId, @Param("monthYear") String monthYear);
}
