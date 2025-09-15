package com.hidoc.api.service;

import com.hidoc.api.ai.config.AIProvidersProperties;
import com.hidoc.api.ai.service.impl.GeminiService;
import com.hidoc.api.ai.service.impl.GrokService;
import com.hidoc.api.ai.service.impl.OpenAIService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class HealthCheckServiceTest {

    private DataSource dataSource;
    private AIProvidersProperties props;
    private HealthCheckService service;

    @BeforeEach
    void setup() throws Exception {
        dataSource = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        when(conn.isValid(anyInt())).thenReturn(true);
        when(conn.getMetaData()).thenReturn(null);
        when(dataSource.getConnection()).thenReturn(conn);

        props = new AIProvidersProperties();
        var open = new AIProvidersProperties.ProviderConfig(); open.setApiKey("k"); open.setModel("m"); open.setBaseUrl("u");
        var grok = new AIProvidersProperties.ProviderConfig(); grok.setApiKey("k"); grok.setModel("m"); grok.setBaseUrl("u");
        var gem = new AIProvidersProperties.ProviderConfig(); gem.setApiKey("k"); gem.setModel("m"); gem.setBaseUrl("u");
        props.setOpenai(open); props.setGrok(grok); props.setGemini(gem);

        service = new HealthCheckService(dataSource,
                java.util.List.of(new OpenAIService(props), new GrokService(props), new GeminiService(props)), props);
    }

    @Test
    void databaseHealth_upWhenValid() {
        var db = service.checkDatabaseHealth();
        assertThat(db.status()).isEqualTo(HealthCheckService.Status.UP);
    }

    @Test
    void aiServicesHealth_upWhenConfigured() {
        var ai = service.checkAIServicesHealth();
        assertThat(ai.status()).isEqualTo(HealthCheckService.Status.UP);
        assertThat(ai.providers()).isNotEmpty();
    }

    @Test
    void systemHealth_upWhenAllUp() {
        var sys = service.getSystemHealth();
        assertThat(sys.status()).isEqualTo("UP");
    }
}
