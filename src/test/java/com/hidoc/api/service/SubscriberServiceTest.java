package com.hidoc.api.service;

import com.hidoc.api.domain.Subscriber;
import com.hidoc.api.repository.SubscriberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class SubscriberServiceTest {

    private SubscriberRepository repository;
    private SubscriberService service;

    @BeforeEach
    void setup() {
        repository = Mockito.mock(SubscriberRepository.class);
        service = new SubscriberService(repository);
    }

    @Test
    void createOrUpdate_shouldCreateNew_whenNotExists() {
        Subscriber input = new Subscriber();
        input.setUserId("user-1");
        input.setEmail("user1@example.com");
        input.setOauthProvider("GOOGLE");
        input.setPurchaseDate(LocalDateTime.now());
        input.setAppStorePlatform("PlayStore");

        when(repository.findById("user-1")).thenReturn(Optional.empty());
        when(repository.findByEmail("user1@example.com")).thenReturn(Optional.empty());
        when(repository.save(any(Subscriber.class))).thenAnswer(inv -> inv.getArgument(0));

        Subscriber saved = service.createOrUpdate(input);

        assertThat(saved.getUserId()).isEqualTo("user-1");
        assertThat(saved.getSubscriptionStatus()).isNotBlank();
        verify(repository, times(1)).save(any(Subscriber.class));
    }

    @Test
    void createOrUpdate_shouldUpdateExisting_whenFoundByEmail() {
        Subscriber existing = new Subscriber();
        existing.setUserId("user-2");
        existing.setEmail("old@example.com");
        existing.setOauthProvider("GOOGLE");
        existing.setSubscriptionStatus("ACTIVE");

        Subscriber input = new Subscriber();
        input.setEmail("new@example.com");
        input.setOauthProvider("MICROSOFT");

        when(repository.findById(null)).thenReturn(Optional.empty());
        when(repository.findByEmail("new@example.com")).thenReturn(Optional.of(existing));
        when(repository.save(any(Subscriber.class))).thenAnswer(inv -> inv.getArgument(0));

        Subscriber updated = service.createOrUpdate(input);
        assertThat(updated.getEmail()).isEqualTo("new@example.com");
        assertThat(updated.getOauthProvider()).isEqualTo("MICROSOFT");
    }

    @Test
    void updateStatus_shouldChangeStatus_whenExists() {
        Subscriber existing = new Subscriber();
        existing.setUserId("user-3");
        existing.setSubscriptionStatus("ACTIVE");

        when(repository.findById("user-3")).thenReturn(Optional.of(existing));
        when(repository.save(any(Subscriber.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = service.updateStatus("user-3", "CANCELLED");
        assertThat(result).isPresent();
        assertThat(result.get().getSubscriptionStatus()).isEqualTo("CANCELLED");

        ArgumentCaptor<Subscriber> captor = ArgumentCaptor.forClass(Subscriber.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getSubscriptionStatus()).isEqualTo("CANCELLED");
    }
}
