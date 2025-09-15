package com.hidoc.api.web;

import com.hidoc.api.service.HealthCheckService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HealthController.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@org.springframework.test.context.TestPropertySource(properties = {
        "server.servlet.context-path="
})
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HealthCheckService healthCheckService;

    @MockBean
    private com.hidoc.api.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void health_shouldReturnOk() throws Exception {
        var db = new HealthCheckService.DatabaseHealth(HealthCheckService.Status.UP, 5, "ok");
        var ai = new HealthCheckService.AIServicesHealth(HealthCheckService.Status.UP, 3, java.util.Map.of());
        var sys = new HealthCheckService.SystemHealth("UP", java.time.Instant.now().toString(), "dev",
                Map.of("database", "UP", "aiServices", "UP"), db, ai);
        when(healthCheckService.getSystemHealth()).thenReturn(sys);

        mockMvc.perform(get("/api/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.database").value("UP"));
    }

    @Test
    void live_shouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/health/live").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void ready_shouldReturnOk() throws Exception {
        var db = new HealthCheckService.DatabaseHealth(HealthCheckService.Status.UP, 5, "ok");
        var ai = new HealthCheckService.AIServicesHealth(HealthCheckService.Status.UP, 3, java.util.Map.of());
        var sys = new HealthCheckService.SystemHealth("UP", java.time.Instant.now().toString(), "dev",
                Map.of("database", "UP", "aiServices", "UP"), db, ai);
        when(healthCheckService.getSystemHealth()).thenReturn(sys);

        mockMvc.perform(get("/api/health/ready").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
