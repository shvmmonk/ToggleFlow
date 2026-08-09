package com.featureflags.service;

import com.featureflags.dto.CreateFlagRequest;
import com.featureflags.dto.FlagResponse;
import com.featureflags.dto.UpdateFlagRequest;
import com.featureflags.entity.FeatureFlag;
import com.featureflags.entity.Project;
import com.featureflags.exception.BadRequestException;
import com.featureflags.exception.ResourceNotFoundException;
import com.featureflags.repository.FeatureFlagRepository;
import com.featureflags.repository.FlagEvaluationRepository;
import com.featureflags.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureFlagService {

    private final FeatureFlagRepository flagRepository;
    private final ProjectRepository projectRepository;
    private final FlagEvaluationRepository evaluationRepository;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public FlagResponse createFlag(CreateFlagRequest request) {
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + request.getProjectId()));

        if (flagRepository.existsByProjectIdAndFlagKey(request.getProjectId(), request.getFlagKey())) {
            throw new BadRequestException("Flag with key '" + request.getFlagKey() + "' already exists in this project");
        }

        FeatureFlag flag = FeatureFlag.builder()
                .project(project)
                .flagKey(request.getFlagKey().trim())
                .name(request.getName().trim())
                .description(request.getDescription())
                .isEnabled(request.getIsEnabled() != null ? request.getIsEnabled() : false)
                .rolloutPercentage(request.getRolloutPercentage() != null ? request.getRolloutPercentage() : 100)
                .build();

        FeatureFlag saved = flagRepository.save(flag);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<FlagResponse> getFlagsForProject(UUID projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found with id: " + projectId);
        }

        List<FeatureFlag> flags = flagRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        return flags.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FlagResponse getFlag(UUID id) {
        FeatureFlag flag = flagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feature flag not found with id: " + id));
        return mapToResponse(flag);
    }

    @Transactional
    public FlagResponse updateFlag(UUID id, UpdateFlagRequest request) {
        FeatureFlag flag = flagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feature flag not found with id: " + id));

        if (request.getName() != null) {
            flag.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            flag.setDescription(request.getDescription());
        }
        if (request.getIsEnabled() != null) {
            flag.setIsEnabled(request.getIsEnabled());
        }
        if (request.getRolloutPercentage() != null) {
            flag.setRolloutPercentage(request.getRolloutPercentage());
        }

        FeatureFlag updated = flagRepository.save(flag);

        // Evict cache for this flag
        evictCache(flag.getProject().getApiKey(), flag.getFlagKey());

        return mapToResponse(updated);
    }

    @Transactional
    public void deleteFlag(UUID id) {
        FeatureFlag flag = flagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feature flag not found with id: " + id));

        String apiKey = flag.getProject().getApiKey();
        String flagKey = flag.getFlagKey();

        flagRepository.delete(flag);

        // Evict cache for this flag
        evictCache(apiKey, flagKey);
    }

    private void evictCache(String apiKey, String flagKey) {
        try {
            String cachePattern = "eval:" + apiKey + ":" + flagKey + ":*";
            Set<String> keys = redisTemplate.keys(cachePattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Evicted {} cache keys for flagKey={}", keys.size(), flagKey);
            }
        } catch (Exception e) {
            log.warn("Failed to evict Redis cache for flagKey={}: {}", flagKey, e.getMessage());
        }
    }

    private FlagResponse mapToResponse(FeatureFlag flag) {
        long trueCount = 0;
        long falseCount = 0;
        try {
            trueCount = evaluationRepository.countByFlagIdAndResult(flag.getId(), true);
            falseCount = evaluationRepository.countByFlagIdAndResult(flag.getId(), false);
        } catch (Exception e) {
            log.debug("Could not count evaluations: {}", e.getMessage());
        }

        return FlagResponse.builder()
                .id(flag.getId())
                .projectId(flag.getProject().getId())
                .flagKey(flag.getFlagKey())
                .name(flag.getName())
                .description(flag.getDescription())
                .isEnabled(flag.getIsEnabled())
                .rolloutPercentage(flag.getRolloutPercentage())
                .createdAt(flag.getCreatedAt())
                .updatedAt(flag.getUpdatedAt())
                .trueEvaluations(trueCount)
                .falseEvaluations(falseCount)
                .build();
    }
}
