package com.hidoc.api;

import com.hidoc.api.repository.UsageTrackingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "ENABLE_E2E", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IntegrationE2ETest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("hidoc")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsageTrackingRepository usageRepo;

    @Test
    void endToEnd_createSubscriber_thenChat_openai_recordsUsage() throws Exception {
        String body = "{" +
                "\"userId\":\"u-it-1\"," +
                "\"email\":\"u1@example.com\"," +
                "\"oauthProvider\":\"GOOGLE\"," +
                "\"subscriptionStatus\":\"ACTIVE\"" +
                "}";

        mockMvc.perform(post("/api/subscribers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/subscribers/u-it-1"));

        String chat = "{" +
                "\"message\":\"hello world\"" +
                "}";

        mockMvc.perform(post("/api/ai/openai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(chat))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").exists());

        long count = usageRepo.countByUserAndMonth("u-it-1", java.time.YearMonth.now().toString());
        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    @Test
    void health_endpoints_shouldWork() throws Exception {
        mockMvc.perform(get("/api/health")).andExpect(status().isOk());
        mockMvc.perform(get("/api/health/live")).andExpect(status().isOk());
        mockMvc.perform(get("/api/health/ready")).andExpect(status().isOk());
    }
}
