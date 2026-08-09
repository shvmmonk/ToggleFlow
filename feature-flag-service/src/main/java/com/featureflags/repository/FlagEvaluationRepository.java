package com.featureflags.repository;

import com.featureflags.entity.FlagEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FlagEvaluationRepository extends JpaRepository<FlagEvaluation, UUID> {
    long countByFlagIdAndResult(UUID flagId, Boolean result);
    long countByFlagId(UUID flagId);
}
