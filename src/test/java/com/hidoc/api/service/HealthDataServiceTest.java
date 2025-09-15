package com.hidoc.api.service;

import com.hidoc.api.domain.HealthDataEntry;
import com.hidoc.api.repository.HealthDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class HealthDataServiceTest {

    private HealthDataRepository repo;
    private HealthDataService service;

    @BeforeEach
    void setup() {
        repo = mock(HealthDataRepository.class);
        service = new HealthDataService(repo);
    }

    @Test
    void processHealthMessage_shouldCreateEntry_andPersistWhenRequested() {
        when(repo.save(any(HealthDataEntry.class))).thenAnswer(inv -> inv.getArgument(0));
        HealthDataEntry saved = service.processHealthMessage("BP 120/80", "user-1", true);
        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getUserId()).isEqualTo("user-1");
        verify(repo, times(1)).save(any(HealthDataEntry.class));
    }

    @Test
    void getTrends_groupsByDay() {
        LocalDateTime now = LocalDateTime.now();
        HealthDataEntry e1 = new HealthDataEntry();
        e1.setId("1"); e1.setUserId("u"); e1.setType("note"); e1.setCategory("c"); e1.setValue("v"); e1.setTimestamp(now.minusDays(1));
        HealthDataEntry e2 = new HealthDataEntry();
        e2.setId("2"); e2.setUserId("u"); e2.setType("note"); e2.setCategory("c"); e2.setValue("v"); e2.setTimestamp(now.minusDays(1));
        HealthDataEntry e3 = new HealthDataEntry();
        e3.setId("3"); e3.setUserId("u"); e3.setType("note"); e3.setCategory("c"); e3.setValue("v"); e3.setTimestamp(now);

        when(repo.findByUserIdAndTimestampBetweenOrderByTimestampAsc(eq("u"), any(), any()))
                .thenReturn(List.of(e1, e2, e3));

        var points = service.getTrends("u", now.minusDays(2), now.plusDays(1), null);
        assertThat(points).hasSize(2);
        assertThat(points.get(0).date()).isEqualTo(LocalDate.from(now.minusDays(1)));
        assertThat(points.get(0).count()).isEqualTo(2);
    }
}
