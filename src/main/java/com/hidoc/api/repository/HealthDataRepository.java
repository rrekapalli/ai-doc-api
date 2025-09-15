package com.hidoc.api.repository;

import com.hidoc.api.domain.HealthDataEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthDataRepository extends JpaRepository<HealthDataEntry, String> {
}
