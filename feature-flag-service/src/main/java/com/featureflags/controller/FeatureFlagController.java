package com.featureflags.controller;

import com.featureflags.dto.CreateFlagRequest;
import com.featureflags.dto.FlagResponse;
import com.featureflags.dto.UpdateFlagRequest;
import com.featureflags.service.FeatureFlagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/flags")
@RequiredArgsConstructor
public class FeatureFlagController {

    private final FeatureFlagService flagService;

    @PostMapping
    public ResponseEntity<FlagResponse> createFlag(@Valid @RequestBody CreateFlagRequest request) {
        FlagResponse response = flagService.createFlag(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<FlagResponse>> getFlagsForProject(@RequestParam UUID projectId) {
        List<FlagResponse> response = flagService.getFlagsForProject(projectId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlagResponse> getFlag(@PathVariable UUID id) {
        FlagResponse response = flagService.getFlag(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FlagResponse> updateFlag(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFlagRequest request) {
        FlagResponse response = flagService.updateFlag(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFlag(@PathVariable UUID id) {
        flagService.deleteFlag(id);
        return ResponseEntity.noContent().build();
    }
}
