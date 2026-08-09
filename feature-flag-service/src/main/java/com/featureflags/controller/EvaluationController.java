package com.featureflags.controller;

import com.featureflags.dto.EvaluationResponse;
import com.featureflags.exception.BadRequestException;
import com.featureflags.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/evaluate")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    @GetMapping("/{flagKey}")
    public ResponseEntity<EvaluationResponse> evaluateFlag(
            @PathVariable String flagKey,
            @RequestParam String projectApiKey,
            @RequestParam String userId) {

        if (projectApiKey == null || projectApiKey.trim().isEmpty()) {
            throw new BadRequestException("Query parameter 'projectApiKey' is required");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new BadRequestException("Query parameter 'userId' is required");
        }

        EvaluationResponse response = evaluationService.evaluateFlag(flagKey, projectApiKey, userId);
        return ResponseEntity.ok(response);
    }
}
