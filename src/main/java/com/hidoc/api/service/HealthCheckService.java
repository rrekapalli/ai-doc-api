package com.hidoc.api.service;

import com.hidoc.api.ai.config.AIProvidersProperties;
import com.hidoc.api.ai.service.AIService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HealthCheckService {

    public enum Status { UP, DOWN, DEGRADED }

    private final DataSource dataSource;
    private final List<AIService> aiServices;
    private final AIProvidersProperties aiProps;

    @Value("${spring.application.name:hi-doc-api-service}")
    private String appName;

    public HealthCheckService(DataSource dataSource, List<AIService> aiServices, AIProvidersProperties aiProps) {
        this.dataSource = dataSource;
        this.aiServices = aiServices;
        this.aiProps = aiProps;
    }

    public SystemHealth getSystemHealth() {
        DatabaseHealth db = checkDatabaseHealth();
        AIServicesHealth ai = checkAIServicesHealth();
        Status overall = (db.status == Status.UP && ai.status == Status.UP) ? Status.UP : Status.DEGRADED;
        if (db.status == Status.DOWN || ai.status == Status.DOWN) {
            overall = Status.DOWN;
        }
        Map<String, String> components = new HashMap<>();
        components.put("database", db.status.name());
        components.put("aiServices", ai.status.name());
        return new SystemHealth(overall.name(), Instant.now().toString(), getVersion(), components, db, ai);
    }

    public DatabaseHealth checkDatabaseHealth() {
        long start = System.nanoTime();
        try (Connection conn = dataSource.getConnection()) {
            boolean valid = conn.isValid(3);
            DatabaseMetaData meta = conn.getMetaData();
            long timeMs = (System.nanoTime() - start) / 1_000_000;
            return new DatabaseHealth(valid ? Status.UP : Status.DOWN, timeMs,
                    meta != null ? meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion() : "unknown");
        } catch (Exception e) {
            long timeMs = (System.nanoTime() - start) / 1_000_000;
            return new DatabaseHealth(Status.DOWN, timeMs, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    public AIServicesHealth checkAIServicesHealth() {
        long started = System.nanoTime();
        Map<String, AIProviderHealth> providers = new HashMap<>();
        Status overall = Status.UP;
        for (AIService svc : aiServices) {
            String name = svc.provider().name();
            long s = System.nanoTime();
            AIProvidersProperties.ProviderConfig cfg = switch (svc.provider()) {
                case OPENAI -> aiProps.getOpenai();
                case GROK -> aiProps.getGrok();
                case GEMINI -> aiProps.getGemini();
            };
            AIProviderHealth pHealth;
            if (cfg != null && notBlank(cfg.getApiKey()) && notBlank(cfg.getBaseUrl()) && notBlank(cfg.getModel())) {
                pHealth = new AIProviderHealth(Status.UP, (System.nanoTime() - s) / 1_000_000, null);
            } else {
                pHealth = new AIProviderHealth(Status.DOWN, (System.nanoTime() - s) / 1_000_000, "Missing configuration");
                overall = Status.DOWN;
            }
            providers.put(name, pHealth);
        }
        long time = (System.nanoTime() - started) / 1_000_000;
        return new AIServicesHealth(overall, time, providers);
    }

    public boolean isSystemHealthy() {
        return getSystemHealth().status.equals(Status.UP.name());
    }

    private String getVersion() {
        try {
            Package p = HealthCheckService.class.getPackage();
            if (p != null && p.getImplementationVersion() != null) {
                return p.getImplementationVersion();
            }
        } catch (Exception ignored) {}
        return "dev";
    }

    private boolean notBlank(String s) { return s != null && !s.isBlank(); }

    // DTOs
    public record SystemHealth(String status, String timestamp, String version, Map<String, String> components,
                               DatabaseHealth database, AIServicesHealth aiServices) {}

    public record DatabaseHealth(Status status, long responseTimeMs, String details) {}

    public record AIServicesHealth(Status status, long responseTimeMs, Map<String, AIProviderHealth> providers) {}

    public record AIProviderHealth(Status status, long responseTimeMs, String lastError) {}
}
