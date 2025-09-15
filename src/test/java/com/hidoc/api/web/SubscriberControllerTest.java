package com.hidoc.api.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hidoc.api.domain.Subscriber;
import com.hidoc.api.service.SubscriberService;
import com.hidoc.api.web.dto.SubscriberDto;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SubscriberController.class)
class SubscriberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SubscriberService subscriberService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createOrUpdate_shouldReturnCreated() throws Exception {
        SubscriberDto dto = new SubscriberDto();
        dto.setUserId("user-1");
        dto.setEmail("user1@example.com");
        dto.setOauthProvider("GOOGLE");
        dto.setSubscriptionStatus("ACTIVE");

        Subscriber saved = new Subscriber();
        saved.setUserId("user-1");
        saved.setEmail("user1@example.com");
        saved.setOauthProvider("GOOGLE");
        saved.setSubscriptionStatus("ACTIVE");
        saved.setCreatedAt(LocalDateTime.now());
        saved.setUpdatedAt(LocalDateTime.now());

        when(subscriberService.createOrUpdate(any(Subscriber.class))).thenReturn(saved);

        mockMvc.perform(post("/api/subscribers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/subscribers/user-1"))
                .andExpect(jsonPath("$.userId").value("user-1"))
                .andExpect(jsonPath("$.email").value("user1@example.com"));
    }

    @Test
    void getByUserId_shouldReturnOk_whenFound() throws Exception {
        Subscriber existing = new Subscriber();
        existing.setUserId("user-2");
        existing.setEmail("u2@example.com");
        existing.setOauthProvider("MICROSOFT");
        when(subscriberService.findByUserId("user-2")).thenReturn(Optional.of(existing));

        mockMvc.perform(get("/api/subscribers/user-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-2"))
                .andExpect(jsonPath("$.email").value("u2@example.com"));
    }

    @Test
    void updateStatus_shouldReturnOk_whenUpdated() throws Exception {
        Subscriber updated = new Subscriber();
        updated.setUserId("user-3");
        updated.setEmail("u3@example.com");
        updated.setOauthProvider("GOOGLE");
        updated.setSubscriptionStatus("CANCELLED");
        when(subscriberService.updateStatus("user-3", "CANCELLED")).thenReturn(Optional.of(updated));

        mockMvc.perform(put("/api/subscribers/user-3/status").param("status", "CANCELLED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriptionStatus").value("CANCELLED"));
    }
}
