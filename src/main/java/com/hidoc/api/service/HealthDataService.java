package com.hidoc.api.service;

import com.hidoc.api.domain.HealthDataEntry;
import com.hidoc.api.repository.HealthDataRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HealthDataService {

    private final HealthDataRepository healthDataRepository;

    public HealthDataService(HealthDataRepository healthDataRepository) {
        this.healthDataRepository = healthDataRepository;
    }

    @Transactional
    public HealthDataEntry processHealthMessage(String message, String userId, boolean persist) {
        // Minimal interpretation: create a generic entry from the message.
        HealthDataEntry entry = new HealthDataEntry();
        entry.setId(UUID.randomUUID().toString());
        entry.setUserId(userId);
        entry.setType("note");
        entry.setCategory("general");
        entry.setValue(truncate(Objects.toString(message, ""), 2000));
        entry.setTimestamp(LocalDateTime.now());
        entry.setNotes("auto-parsed");
        if (persist) {
            return healthDataRepository.save(entry);
        }
        return entry;
    }

    @Transactional
    public HealthDataEntry saveHealthData(HealthDataEntry entry) {
        if (entry.getId() == null || entry.getId().isBlank()) {
            entry.setId(UUID.randomUUID().toString());
        }
        if (entry.getTimestamp() == null) {
            entry.setTimestamp(LocalDateTime.now());
        }
        return healthDataRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public List<HealthDataEntry> getHistory(String userId, LocalDateTime from, LocalDateTime to, String type) {
        LocalDateTime fromTs = from != null ? from : LocalDateTime.of(LocalDate.now().minusDays(30), LocalTime.MIN);
        LocalDateTime toTs = to != null ? to : LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        if (type == null || type.isBlank()) {
            return healthDataRepository.findByUserIdAndTimestampBetweenOrderByTimestampAsc(userId, fromTs, toTs);
        }
        return healthDataRepository.findByUserIdAndTypeAndTimestampBetweenOrderByTimestampAsc(userId, type, fromTs, toTs);
    }

    @Transactional(readOnly = true)
    public List<TrendPoint> getTrends(String userId, LocalDateTime from, LocalDateTime to, String type) {
        List<HealthDataEntry> entries = getHistory(userId, from, to, type);
        Map<LocalDate, Long> byDay = entries.stream()
                .collect(Collectors.groupingBy(e -> e.getTimestamp().toLocalDate(), TreeMap::new, Collectors.counting()));
        return byDay.entrySet().stream()
                .map(e -> new TrendPoint(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, max);
    }

    public record TrendPoint(LocalDate date, long count) {}
}
