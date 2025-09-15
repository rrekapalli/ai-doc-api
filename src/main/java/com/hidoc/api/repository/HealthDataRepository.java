package com.hidoc.api.repository;

import com.hidoc.api.domain.HealthDataEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface HealthDataRepository extends JpaRepository<HealthDataEntry, String> {
    List<HealthDataEntry> findByUserIdAndTimestampBetweenOrderByTimestampAsc(String userId, LocalDateTime from, LocalDateTime to);
    List<HealthDataEntry> findByUserIdAndTypeAndTimestampBetweenOrderByTimestampAsc(String userId, String type, LocalDateTime from, LocalDateTime to);
}
