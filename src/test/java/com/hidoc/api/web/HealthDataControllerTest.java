package com.hidoc.api.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hidoc.api.domain.HealthDataEntry;
import com.hidoc.api.service.HealthDataService;
import com.hidoc.api.web.dto.HealthMessageRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = HealthDataController.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@org.springframework.test.context.TestPropertySource(properties = {
        "server.servlet.context-path="
})
class HealthDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HealthDataService healthDataService;

    @MockBean
    private com.hidoc.api.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "user-1")
    void process_shouldReturnCreated_whenPersistTrue() throws Exception {
        HealthDataEntry entry = new HealthDataEntry();
        entry.setId("id-1");
        entry.setUserId("user-1");
        entry.setType("note");
        entry.setCategory("general");
        entry.setValue("hello");
        entry.setTimestamp(LocalDateTime.now());
        Mockito.when(healthDataService.processHealthMessage(anyString(), anyString(), eq(true))).thenReturn(entry);

        HealthMessageRequest req = new HealthMessageRequest();
        req.setMessage("hello");
        req.setPersist(true);

        mockMvc.perform(post("/api/health/process")
                        .with(user("user-1"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("id-1"));
    }

    @Test
    @WithMockUser(username = "user-2")
    void history_shouldReturnOk() throws Exception {
        HealthDataEntry e = new HealthDataEntry();
        e.setId("h1"); e.setUserId("user-2"); e.setType("note"); e.setCategory("c"); e.setValue("v"); e.setTimestamp(LocalDateTime.now());
        Mockito.when(healthDataService.getHistory(eq("user-2"), any(), any(), any())).thenReturn(List.of(e));

        mockMvc.perform(get("/api/health/history").with(user("user-2")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("h1"));
    }

    @Test
    @WithMockUser(username = "user-3")
    void trends_shouldReturnOk() throws Exception {
        var tp = new HealthDataService.TrendPoint(java.time.LocalDate.now(), 3);
        Mockito.when(healthDataService.getTrends(eq("user-3"), any(), any(), any())).thenReturn(List.of(tp));

        mockMvc.perform(get("/api/health/trends").with(user("user-3")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].count").value(3));
    }
}
