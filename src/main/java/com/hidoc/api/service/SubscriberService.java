package com.hidoc.api.service;

import com.hidoc.api.domain.Subscriber;
import com.hidoc.api.repository.SubscriberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class SubscriberService {

    private final SubscriberRepository subscriberRepository;

    public SubscriberService(SubscriberRepository subscriberRepository) {
        this.subscriberRepository = subscriberRepository;
    }

    @org.springframework.cache.annotation.CacheEvict(cacheNames = {"subscriberById", "subscriberByEmail"}, allEntries = true)
    @Transactional
    public Subscriber createOrUpdate(Subscriber input) {
        // Upsert by userId if present; otherwise, by email
        Optional<Subscriber> existingOpt = Optional.empty();
        if (input.getUserId() != null && !input.getUserId().isBlank()) {
            existingOpt = subscriberRepository.findById(input.getUserId());
        }
        if (existingOpt.isEmpty() && input.getEmail() != null && !input.getEmail().isBlank()) {
            existingOpt = subscriberRepository.findByEmail(input.getEmail());
        }

        if (existingOpt.isPresent()) {
            Subscriber existing = existingOpt.get();
            // Update mutable fields
            if (input.getEmail() != null && !input.getEmail().isBlank()) {
                existing.setEmail(input.getEmail());
            }
            if (input.getOauthProvider() != null && !input.getOauthProvider().isBlank()) {
                existing.setOauthProvider(input.getOauthProvider());
            }
            if (input.getSubscriptionStatus() != null && !input.getSubscriptionStatus().isBlank()) {
                existing.setSubscriptionStatus(input.getSubscriptionStatus());
            }
            if (input.getPurchaseDate() != null) {
                existing.setPurchaseDate(input.getPurchaseDate());
            }
            if (input.getAppStorePlatform() != null && !input.getAppStorePlatform().isBlank()) {
                existing.setAppStorePlatform(input.getAppStorePlatform());
            }
            existing.setUpdatedAt(LocalDateTime.now());
            return subscriberRepository.save(existing);
        } else {
            // Create
            if (input.getUserId() == null || input.getUserId().isBlank()) {
                // In this system, userId is required as PK. If missing, fallback to email as userId.
                input.setUserId(input.getEmail());
            }
            if (input.getSubscriptionStatus() == null || input.getSubscriptionStatus().isBlank()) {
                input.setSubscriptionStatus("ACTIVE");
            }
            input.setCreatedAt(LocalDateTime.now());
            input.setUpdatedAt(input.getCreatedAt());
            return subscriberRepository.save(input);
        }
    }

    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable(cacheNames = "subscriberById", key = "#p0")
    public Optional<Subscriber> findByUserId(String userId) {
        return subscriberRepository.findById(userId);
    }

    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable(cacheNames = "subscriberByEmail", key = "#p0")
    public Optional<Subscriber> findByEmail(String email) {
        return subscriberRepository.findByEmail(email);
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(cacheNames = {"subscriberById", "subscriberByEmail"}, allEntries = true)
    public Optional<Subscriber> updateStatus(String userId, String newStatus) {
        return subscriberRepository.findById(userId).map(sub -> {
            sub.setSubscriptionStatus(newStatus);
            sub.setUpdatedAt(LocalDateTime.now());
            return subscriberRepository.save(sub);
        });
    }
}
