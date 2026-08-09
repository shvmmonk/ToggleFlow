package com.featureflags.service;

import com.featureflags.dto.CreateProjectRequest;
import com.featureflags.dto.ProjectResponse;
import com.featureflags.entity.Project;
import com.featureflags.exception.ResourceNotFoundException;
import com.featureflags.repository.ProjectRepository;
import com.featureflags.util.ApiKeyGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request) {
        String apiKey = ApiKeyGenerator.generateApiKey();
        Project project = Project.builder()
                .name(request.getName().trim())
                .apiKey(apiKey)
                .build();

        Project saved = projectRepository.save(project);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(UUID id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
        return mapToResponse(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects() {
        return projectRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Project getEntityByApiKey(String apiKey) {
        return projectRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found for provided API key"));
    }

    private ProjectResponse mapToResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .apiKey(project.getApiKey())
                .createdAt(project.getCreatedAt())
                .build();
    }
}
