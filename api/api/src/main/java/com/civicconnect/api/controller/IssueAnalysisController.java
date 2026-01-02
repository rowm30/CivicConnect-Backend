package com.civicconnect.api.controller;

import com.civicconnect.api.config.GcpConfig;
import com.civicconnect.api.dto.IssueAnalysisRequest;
import com.civicconnect.api.dto.IssueAnalysisResponse;
import com.civicconnect.api.service.ai.ImageAnalysisService;
import com.civicconnect.api.service.ai.VoiceTranscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for AI-powered issue analysis
 * Provides endpoints for analyzing images and audio to auto-fill issue forms
 */
@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class IssueAnalysisController {

    private final ImageAnalysisService imageAnalysisService;
    private final VoiceTranscriptionService voiceTranscriptionService;
    private final GcpConfig gcpConfig;

    /**
     * Analyze an issue image and optional audio to generate form suggestions
     *
     * @param request Contains imageUrl (required), audioUrl (optional), and location info
     * @return IssueAnalysisResponse with suggested title, description, category, priority
     */
    @PostMapping("/analyze-issue")
    public ResponseEntity<IssueAnalysisResponse> analyzeIssue(@RequestBody IssueAnalysisRequest request) {
        log.info("Analyzing issue - imageUrl: {}, audioUrl: {}, location: {}, coords: ({}, {})",
                request.getImageUrl(), request.getAudioUrl(), request.getLocationName(),
                request.getLatitude(), request.getLongitude());

        // Validate request
        if (request.getImageUrl() == null || request.getImageUrl().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(IssueAnalysisResponse.failure("Image URL is required"));
        }

        try {
            // Step 1: Transcribe audio if provided
            String transcription = null;
            if (request.getAudioUrl() != null && !request.getAudioUrl().isEmpty()) {
                log.info("Transcribing audio from: {}", request.getAudioUrl());
                transcription = voiceTranscriptionService.transcribe(request.getAudioUrl());
                log.info("Transcription result: {}", transcription != null ? transcription : "null (transcription failed)");
            }

            // Step 2: Analyze image with Gemini Vision (include transcription, location, and GPS for context)
            IssueAnalysisResponse response = imageAnalysisService.analyzeIssue(
                    request.getImageUrl(),
                    transcription,
                    request.getLocationName(),
                    request.getLatitude(),
                    request.getLongitude()
            );

            log.info("Analysis complete - success: {}, category: {}, priority: {}",
                    response.isSuccess(),
                    response.getSuggestedCategory(),
                    response.getSuggestedPriority());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Issue analysis failed", e);
            return ResponseEntity.internalServerError()
                    .body(IssueAnalysisResponse.failure("Analysis failed: " + e.getMessage()));
        }
    }

    /**
     * Check if AI analysis service is available
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        boolean available = gcpConfig.isAvailable();
        return ResponseEntity.ok(Map.of(
                "available", available,
                "projectId", gcpConfig.getProjectId(),
                "location", gcpConfig.getLocation(),
                "geminiModel", gcpConfig.getGeminiModel()
        ));
    }

    /**
     * Transcribe audio only (for testing/debugging)
     */
    @PostMapping("/transcribe")
    public ResponseEntity<Map<String, Object>> transcribeAudio(@RequestBody Map<String, String> request) {
        String audioUrl = request.get("audioUrl");
        if (audioUrl == null || audioUrl.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "audioUrl is required"));
        }

        String transcription = voiceTranscriptionService.transcribe(audioUrl);
        return ResponseEntity.ok(Map.of(
                "success", transcription != null,
                "transcription", transcription != null ? transcription : ""
        ));
    }
}
