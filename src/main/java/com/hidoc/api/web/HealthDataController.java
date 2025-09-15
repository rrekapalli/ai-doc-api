package com.hidoc.api.web;

import com.hidoc.api.domain.HealthDataEntry;
import com.hidoc.api.security.UserInfo;
import com.hidoc.api.service.HealthDataService;
import com.hidoc.api.web.dto.HealthMessageRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/health")
@Validated
public class HealthDataController {

    private final HealthDataService healthDataService;

    public HealthDataController(HealthDataService healthDataService) {
        this.healthDataService = healthDataService;
    }

    @PostMapping(value = "/process", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HealthDataEntry> process(@Valid @RequestBody HealthMessageRequest request) {
        String userId = extractUserId();
        HealthDataEntry entry = healthDataService.processHealthMessage(request.getMessage(), userId, request.isPersist());
        return new ResponseEntity<>(entry, request.isPersist() ? HttpStatus.CREATED : HttpStatus.OK);
    }

    @GetMapping(value = "/history", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<HealthDataEntry>> history(@RequestParam(name = "type", required = false) String type,
                                                         @RequestParam(name = "from", required = false)
                                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                                         @RequestParam(name = "to", required = false)
                                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        String userId = extractUserId();
        List<HealthDataEntry> entries = healthDataService.getHistory(userId, from, to, type);
        return ResponseEntity.ok(entries);
    }

    @GetMapping(value = "/trends", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<com.hidoc.api.service.HealthDataService.TrendPoint>> trends(@RequestParam(name = "type", required = false) String type,
                                                                                           @RequestParam(name = "from", required = false)
                                                                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                                                                           @RequestParam(name = "to", required = false)
                                                                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        String userId = extractUserId();
        var points = healthDataService.getTrends(userId, from, to, type);
        return ResponseEntity.ok(points);
    }

    private String extractUserId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new IllegalArgumentException("Authenticated user is required");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof UserInfo u) {
            return u.getUserId();
        }
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
            return ud.getUsername();
        }
        if (principal instanceof String s && !s.isBlank()) {
            return s;
        }
        throw new IllegalArgumentException("Invalid authentication principal");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
