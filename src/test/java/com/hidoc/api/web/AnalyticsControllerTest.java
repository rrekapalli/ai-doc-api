package com.hidoc.api.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hidoc.api.service.AnalyticsService;
import com.hidoc.api.service.AnalyticsService.AnalyticsSummaryReport;
import com.hidoc.api.service.AnalyticsService.TrendPoint;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AnalyticsController.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@org.springframework.test.context.TestPropertySource(properties = {
        "server.servlet.context-path="
})
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @MockBean
    private com.hidoc.api.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "admin")
    void summary_shouldReturnOk() throws Exception {
        AnalyticsSummaryReport report = new AnalyticsSummaryReport(200, 50,
                Map.of("OPENAI", 150L, "GROK", 30L, "GEMINI", 20L),
                LocalDateTime.now().minusDays(30), LocalDateTime.now());
        Mockito.when(analyticsService.getSummary(any(), any())).thenReturn(report);

        mockMvc.perform(get("/api/analytics/summary")
                        .with(user("admin"))
                        .with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequests").value(200))
                .andExpect(jsonPath("$.uniqueUsers").value(50))
                .andExpect(jsonPath("$.requestsByProvider.OPENAI").value(150));
    }

    @Test
    @WithMockUser(username = "admin")
    void trends_shouldReturnOk() throws Exception {
        List<TrendPoint> points = List.of(new TrendPoint(LocalDate.now().minusDays(1), 5), new TrendPoint(LocalDate.now(), 7));
        Mockito.when(analyticsService.getDailyTrends(any(), any(), any())).thenReturn(points);

        mockMvc.perform(get("/api/analytics/trends")
                        .with(user("admin"))
                        .with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
