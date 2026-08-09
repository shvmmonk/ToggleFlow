package com.featureflags.service;

import com.featureflags.dto.EvaluationResponse;
import com.featureflags.entity.FeatureFlag;
import com.featureflags.entity.FlagEvaluation;
import com.featureflags.exception.ResourceNotFoundException;
import com.featureflags.repository.FeatureFlagRepository;
import com.featureflags.repository.FlagEvaluationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final FeatureFlagRepository flagRepository;
    private final FlagEvaluationRepository evaluationRepository;
    private final StringRedisTemplate redisTemplate;

    private static final Duration CACHE_TTL = Duration.ofSeconds(60);

    @Transactional
    public EvaluationResponse evaluateFlag(String flagKey, String projectApiKey, String userId) {
        String cacheKey = "eval:" + projectApiKey + ":" + flagKey + ":" + userId;

        // 1. Check Valkey / Redis cache first
        try {
            String cachedResult = redisTemplate.opsForValue().get(cacheKey);
            if (cachedResult != null) {
                log.debug("Cache HIT for key: {}", cacheKey);
                boolean enabled = Boolean.parseBoolean(cachedResult);
                return EvaluationResponse.builder()
                        .flagKey(flagKey)
                        .userId(userId)
                        .enabled(enabled)
                        .source("CACHE")
                        .evaluatedAt(LocalDateTime.now())
                        .build();
            }
        } catch (Exception e) {
            log.warn("Valkey/Redis cache read error for key {}: {}. Falling back to PostgreSQL DB.", cacheKey, e.getMessage());
        }

        // 2. Cache MISS -> query PostgreSQL database
        FeatureFlag flag = flagRepository.findByProjectApiKeyAndFlagKey(projectApiKey, flagKey)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Flag '" + flagKey + "' not found for project API key"));

        // 3. Deterministic rollout logic calculation
        boolean enabled = calculateRollout(flag, userId, flagKey);

        // 4. Cache evaluation result in Valkey/Redis with short TTL (60s)
        try {
            redisTemplate.opsForValue().set(cacheKey, String.valueOf(enabled), CACHE_TTL);
            log.debug("Cached result for key: {} with TTL 60s", cacheKey);
        } catch (Exception e) {
            log.warn("Valkey/Redis cache write error for key {}: {}", cacheKey, e.getMessage());
        }

        // 5. Store evaluation log asynchronously / transactionally for analytics
        try {
            FlagEvaluation eval = FlagEvaluation.builder()
                    .flag(flag)
                    .userIdentifier(userId)
                    .result(enabled)
                    .evaluatedAt(LocalDateTime.now())
                    .build();
            evaluationRepository.save(eval);
        } catch (Exception e) {
            log.warn("Failed to log flag evaluation: {}", e.getMessage());
        }

        return EvaluationResponse.builder()
                .flagKey(flagKey)
                .userId(userId)
                .enabled(enabled)
                .source("DATABASE")
                .evaluatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Deterministic percentage rollout calculation:
     * bucket = Math.abs((userId + flagKey).hashCode()) % 100;
     * enabled = flag.isEnabled() && bucket < flag.getRolloutPercentage();
     */
    public boolean calculateRollout(FeatureFlag flag, String userId, String flagKey) {
        if (flag == null || !Boolean.TRUE.equals(flag.getIsEnabled())) {
            return false;
        }

        int percentage = flag.getRolloutPercentage() != null ? flag.getRolloutPercentage() : 100;
        if (percentage >= 100) {
            return true;
        }
        if (percentage <= 0) {
            return false;
        }

        // Deterministic hash computation safely avoiding Integer.MIN_VALUE issue
        int rawHash = (userId + flagKey).hashCode();
        int bucket = Math.abs(rawHash % 100);
        return bucket < percentage;
    }
}
