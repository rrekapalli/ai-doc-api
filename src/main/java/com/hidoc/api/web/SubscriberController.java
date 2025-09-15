package com.hidoc.api.web;

import com.hidoc.api.domain.Subscriber;
import com.hidoc.api.service.SubscriberService;
import com.hidoc.api.web.dto.SubscriberDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;

@RestController
@RequestMapping("/api/subscribers")
@Validated
public class SubscriberController {

    private final SubscriberService subscriberService;

    public SubscriberController(SubscriberService subscriberService) {
        this.subscriberService = subscriberService;
    }

    @PostMapping
    public ResponseEntity<SubscriberDto> createOrUpdate(@Valid @RequestBody SubscriberDto request) {
        Subscriber toSave = mapToEntity(request);
        Subscriber saved = subscriberService.createOrUpdate(toSave);
        SubscriberDto response = mapToDto(saved);
        return ResponseEntity.created(URI.create("/api/subscribers/" + response.getUserId())).body(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<SubscriberDto> getByUserId(@PathVariable("userId") @NotBlank String userId) {
        Optional<Subscriber> sub = subscriberService.findByUserId(userId);
        return sub.map(value -> ResponseEntity.ok(mapToDto(value)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/by-email")
    public ResponseEntity<SubscriberDto> getByEmail(@RequestParam("email") @NotBlank @Email String email) {
        Optional<Subscriber> sub = subscriberService.findByEmail(email);
        return sub.map(value -> ResponseEntity.ok(mapToDto(value)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PutMapping("/{userId}/status")
    public ResponseEntity<SubscriberDto> updateStatus(@PathVariable("userId") @NotBlank String userId,
                                                      @RequestParam("status") @NotBlank String status) {
        Optional<Subscriber> updated = subscriberService.updateStatus(userId, status);
        return updated.map(value -> ResponseEntity.ok(mapToDto(value)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // Mapping helpers
    private static SubscriberDto mapToDto(Subscriber s) {
        SubscriberDto dto = new SubscriberDto();
        dto.setUserId(s.getUserId());
        dto.setEmail(s.getEmail());
        dto.setOauthProvider(s.getOauthProvider());
        dto.setSubscriptionStatus(s.getSubscriptionStatus());
        dto.setPurchaseDate(s.getPurchaseDate());
        dto.setAppStorePlatform(s.getAppStorePlatform());
        dto.setCreatedAt(s.getCreatedAt());
        dto.setUpdatedAt(s.getUpdatedAt());
        return dto;
    }

    private static Subscriber mapToEntity(SubscriberDto dto) {
        Subscriber s = new Subscriber();
        s.setUserId(dto.getUserId());
        s.setEmail(dto.getEmail());
        s.setOauthProvider(dto.getOauthProvider());
        s.setSubscriptionStatus(dto.getSubscriptionStatus());
        s.setPurchaseDate(dto.getPurchaseDate());
        s.setAppStorePlatform(dto.getAppStorePlatform());
        s.setCreatedAt(dto.getCreatedAt());
        s.setUpdatedAt(dto.getUpdatedAt());
        return s;
    }
}
