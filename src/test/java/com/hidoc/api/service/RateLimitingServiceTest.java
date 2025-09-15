package com.hidoc.api.service;

import com.hidoc.api.repository.UsageTrackingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class RateLimitingServiceTest {

    private UsageTrackingRepository repo;
    private RateLimitingService service;

    @BeforeEach
    void setUp() {
        repo = mock(UsageTrackingRepository.class);
        service = new RateLimitingService(repo);
    }

    @Test
    void isRequestAllowed_trueWhenUnderLimit() {
        when(repo.countByUserAndMonth(eq("user1"), anyString())).thenReturn(5L);
        assertThat(service.isRequestAllowed("user1")).isTrue();
    }

    @Test
    void isRequestAllowed_falseWhenAtLimit() {
        // Simulate 100 used already
        when(repo.countByUserAndMonth(eq("user1"), anyString())).thenReturn(100L);
        assertThat(service.isRequestAllowed("user1")).isFalse();
    }

    @Test
    void recordRequest_savesUsageTracking() {
        when(repo.countByUserAndMonth(anyString(), anyString())).thenReturn(0L);
        service.recordRequest("user1", "OPENAI", true, null);
        verify(repo, times(1)).save(any());
    }

    @Test
    void getUserUsageStats_reportsRemaining() {
        when(repo.countByUserAndMonth(eq("user1"), anyString())).thenReturn(10L);
        RateLimitingService.UsageStats stats = service.getUserUsageStats("user1");
        assertThat(stats.used()).isEqualTo(10);
        assertThat(stats.remaining()).isEqualTo(90);
        assertThat(stats.limit()).isEqualTo(100);
        assertThat(stats.monthYear()).matches("\\d{4}-\\d{2}");
    }
}
