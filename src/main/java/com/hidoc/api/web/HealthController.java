package com.hidoc.api.web;

import com.hidoc.api.service.HealthCheckService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final HealthCheckService healthCheckService;

    public HealthController(HealthCheckService healthCheckService) {
        this.healthCheckService = healthCheckService;
    }

    // Basic health
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        var sys = healthCheckService.getSystemHealth();
        return ResponseEntity.ok(Map.of(
                "status", sys.status(),
                "timestamp", sys.timestamp(),
                "version", sys.version(),
                "components", sys.components()
        ));
    }

    // Detailed health status
    @GetMapping("/detailed")
    public ResponseEntity<HealthCheckService.SystemHealth> detailed() {
        return ResponseEntity.ok(healthCheckService.getSystemHealth());
    }

    // Readiness probe
    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> readiness() {
        var sys = healthCheckService.getSystemHealth();
        return ResponseEntity.ok(Map.of(
                "status", sys.status(),
                "timestamp", Instant.now().toString()
        ));
    }

    // Liveness probe
    @GetMapping("/live")
    public ResponseEntity<Map<String, Object>> liveness() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", Instant.now().toString()
        ));
    }
}
