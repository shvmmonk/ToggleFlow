package com.featureflags.repository;

import com.featureflags.entity.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, UUID> {

    List<FeatureFlag> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    Optional<FeatureFlag> findByProjectIdAndFlagKey(UUID projectId, String flagKey);

    @Query("SELECT f FROM FeatureFlag f WHERE f.project.apiKey = :apiKey AND f.flagKey = :flagKey")
    Optional<FeatureFlag> findByProjectApiKeyAndFlagKey(@Param("apiKey") String apiKey, @Param("flagKey") String flagKey);

    boolean existsByProjectIdAndFlagKey(UUID projectId, String flagKey);
}
