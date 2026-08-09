package com.featureflags.service;

import com.featureflags.entity.FeatureFlag;
import com.featureflags.entity.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.featureflags.repository.FeatureFlagRepository;
import com.featureflags.repository.FlagEvaluationRepository;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

    @Mock
    private FeatureFlagRepository flagRepository;

    @Mock
    private FlagEvaluationRepository evaluationRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private EvaluationService evaluationService;

    private Project testProject;

    @BeforeEach
    void setUp() {
        testProject = Project.builder()
                .id(UUID.randomUUID())
                .name("Test Project")
                .apiKey("ff_live_test_123456789")
                .build();
    }

    @Test
    @DisplayName("Disabled flag should always evaluate to false")
    void testDisabledFlagReturnsFalse() {
        FeatureFlag flag = FeatureFlag.builder()
                .project(testProject)
                .flagKey("dark_mode")
                .isEnabled(false)
                .rolloutPercentage(100)
                .build();

        boolean result = evaluationService.calculateRollout(flag, "user_123", "dark_mode");
        assertFalse(result, "Disabled flag must return false regardless of rollout percentage");
    }

    @Test
    @DisplayName("Enabled flag with 100% rollout should evaluate to true")
    void testFullRolloutReturnsTrue() {
        FeatureFlag flag = FeatureFlag.builder()
                .project(testProject)
                .flagKey("new_checkout")
                .isEnabled(true)
                .rolloutPercentage(100)
                .build();

        boolean result = evaluationService.calculateRollout(flag, "user_123", "new_checkout");
        assertTrue(result, "100% rollout flag must return true for all users");
    }

    @Test
    @DisplayName("Enabled flag with 0% rollout should evaluate to false")
    void testZeroRolloutReturnsFalse() {
        FeatureFlag flag = FeatureFlag.builder()
                .project(testProject)
                .flagKey("beta_feature")
                .isEnabled(true)
                .rolloutPercentage(0)
                .build();

        boolean result = evaluationService.calculateRollout(flag, "user_123", "beta_feature");
        assertFalse(result, "0% rollout flag must return false for all users");
    }

    @Test
    @DisplayName("Rollout evaluation should be deterministic for the same user and flagKey")
    void testRolloutDeterminism() {
        FeatureFlag flag = FeatureFlag.builder()
                .project(testProject)
                .flagKey("ai_chat")
                .isEnabled(true)
                .rolloutPercentage(50)
                .build();

        String userId = "user_45678";
        boolean firstRun = evaluationService.calculateRollout(flag, userId, "ai_chat");
        
        for (int i = 0; i < 100; i++) {
            boolean currentRun = evaluationService.calculateRollout(flag, userId, "ai_chat");
            assertEquals(firstRun, currentRun, "Evaluation result must be deterministic for identical user and flag");
        }
    }

    @Test
    @DisplayName("Percentage rollout should approximate expected distribution across simulated user population")
    void testRolloutDistribution() {
        int rolloutPercentage = 30;
        FeatureFlag flag = FeatureFlag.builder()
                .project(testProject)
                .flagKey("promo_banner")
                .isEnabled(true)
                .rolloutPercentage(rolloutPercentage)
                .build();

        int sampleSize = 1000;
        int enabledCount = 0;

        for (int i = 0; i < sampleSize; i++) {
            String userId = "simulated_user_" + i;
            if (evaluationService.calculateRollout(flag, userId, "promo_banner")) {
                enabledCount++;
            }
        }

        double actualPercentage = (enabledCount / (double) sampleSize) * 100;
        // Allow a reasonable range (+/- 7%) for hash distribution variance in sample
        assertTrue(actualPercentage >= 23 && actualPercentage <= 37,
                "Expected rollout to be near 30%, but was " + actualPercentage + "%");
    }
}
