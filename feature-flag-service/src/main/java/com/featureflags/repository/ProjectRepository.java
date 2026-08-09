package com.featureflags.repository;

import com.featureflags.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    Optional<Project> findByApiKey(String apiKey);
    boolean existsByApiKey(String apiKey);
}
