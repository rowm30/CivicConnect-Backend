package com.civicconnect.api.controller;

import com.civicconnect.api.dto.JavaCodeExtractionDTO;
import com.civicconnect.api.entity.JavaCodeExtractionSession;
import com.civicconnect.api.service.JavaCodeExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/code-extraction")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class JavaCodeExtractionController {

    private final JavaCodeExtractionService extractionService;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${file.base-url:http://localhost:8080}")
    private String baseUrl;

    // Store pending uploads for chunked upload sessions
    private final Map<String, ChunkedUploadSession> chunkedSessions = new ConcurrentHashMap<>();

    /**
     * Start a chunked upload session
     * Returns a temporary session ID for uploading images in batches
     */
    @PostMapping("/start-session")
    public ResponseEntity<?> startUploadSession(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "sessionName", required = false) String sessionName,
            @RequestParam("totalImages") int totalImages
    ) {
        String tempSessionId = UUID.randomUUID().toString();
        String extractionDir = "code-extraction/" + tempSessionId;

        try {
            Path extractionPath = Paths.get(uploadDir, extractionDir);
            Files.createDirectories(extractionPath);

            ChunkedUploadSession uploadSession = new ChunkedUploadSession();
            uploadSession.userId = userId;
            uploadSession.sessionName = sessionName != null ? sessionName : "Extraction " + LocalDateTime.now().toString();
            uploadSession.totalImages = totalImages;
            uploadSession.extractionDir = extractionDir;
            uploadSession.imageUrls = new ArrayList<>();
            uploadSession.uploadedCount = 0;

            chunkedSessions.put(tempSessionId, uploadSession);

            log.info("Started chunked upload session {} for user {}, expecting {} images",
                    tempSessionId, userId, totalImages);

            return ResponseEntity.ok(Map.of(
                    "tempSessionId", tempSessionId,
                    "message", "Upload session started. Upload images in batches using /upload-chunk"
            ));

        } catch (IOException e) {
            log.error("Failed to create extraction directory: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Failed to start session");
        }
    }

    /**
     * Upload a chunk of images (batch upload)
     * Call this multiple times until all images are uploaded
     */
    @PostMapping("/upload-chunk")
    public ResponseEntity<?> uploadChunk(
            @RequestParam("tempSessionId") String tempSessionId,
            @RequestParam("images") List<MultipartFile> images,
            @RequestParam("chunkIndex") int chunkIndex
    ) {
        ChunkedUploadSession uploadSession = chunkedSessions.get(tempSessionId);
        if (uploadSession == null) {
            return ResponseEntity.badRequest().body("Invalid or expired session ID");
        }

        log.info("Receiving chunk {} with {} images for session {}",
                chunkIndex, images.size(), tempSessionId);

        try {
            Path extractionPath = Paths.get(uploadDir, uploadSession.extractionDir);

            for (int i = 0; i < images.size(); i++) {
                MultipartFile image = images.get(i);
                String originalFilename = image.getOriginalFilename();
                String extension = originalFilename != null && originalFilename.contains(".")
                        ? originalFilename.substring(originalFilename.lastIndexOf('.'))
                        : ".jpg";

                // Global index across all chunks
                int globalIndex = uploadSession.uploadedCount + i + 1;
                String filename = String.format("%04d_%s%s", globalIndex,
                        UUID.randomUUID().toString().substring(0, 8), extension);
                Path filePath = extractionPath.resolve(filename);
                image.transferTo(filePath.toFile());

                String imageUrl = "/uploads/" + uploadSession.extractionDir + "/" + filename;
                synchronized (uploadSession.imageUrls) {
                    uploadSession.imageUrls.add(imageUrl);
                }

                log.debug("Saved image {}: {}", globalIndex, imageUrl);
            }

            uploadSession.uploadedCount += images.size();

            return ResponseEntity.ok(Map.of(
                    "uploaded", uploadSession.uploadedCount,
                    "total", uploadSession.totalImages,
                    "remaining", uploadSession.totalImages - uploadSession.uploadedCount,
                    "message", uploadSession.uploadedCount >= uploadSession.totalImages
                            ? "All images uploaded. Call /finalize-session to start processing."
                            : "Chunk uploaded successfully. Continue uploading."
            ));

        } catch (IOException e) {
            log.error("Error uploading chunk: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to upload chunk: " + e.getMessage());
        }
    }

    /**
     * Finalize the chunked upload and start processing
     */
    @PostMapping("/finalize-session")
    public ResponseEntity<?> finalizeSession(
            @RequestParam("tempSessionId") String tempSessionId
    ) {
        ChunkedUploadSession uploadSession = chunkedSessions.remove(tempSessionId);
        if (uploadSession == null) {
            return ResponseEntity.badRequest().body("Invalid or expired session ID");
        }

        if (uploadSession.imageUrls.isEmpty()) {
            return ResponseEntity.badRequest().body("No images were uploaded");
        }

        log.info("Finalizing session {} with {} images", tempSessionId, uploadSession.imageUrls.size());

        // Create the actual extraction session
        JavaCodeExtractionSession session = extractionService.createSession(
                uploadSession.userId,
                uploadSession.sessionName,
                uploadSession.imageUrls.size()
        );

        // Start async processing
        extractionService.processExtractionAsync(session.getId(), uploadSession.imageUrls);

        return ResponseEntity.ok(JavaCodeExtractionDTO.ExtractionResponse.builder()
                .sessionId(session.getId())
                .sessionName(session.getSessionName())
                .status(session.getStatus().name())
                .totalImages(session.getTotalImages())
                .processedImages(0)
                .createdAt(session.getCreatedAt())
                .build());
    }

    /**
     * Single request upload (for smaller batches, up to ~50 images)
     * For larger uploads, use the chunked upload flow
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadImagesAndStartExtraction(
            @RequestParam("images") List<MultipartFile> images,
            @RequestParam("userId") Long userId,
            @RequestParam(value = "sessionName", required = false) String sessionName
    ) {
        log.info("Received {} images for extraction from user {}", images.size(), userId);

        if (images.isEmpty()) {
            return ResponseEntity.badRequest().body("No images provided");
        }

        // For very large uploads, recommend chunked approach
        if (images.size() > 200) {
            return ResponseEntity.badRequest().body(
                    "Too many images for single upload. Use chunked upload flow: " +
                    "1) POST /start-session, 2) POST /upload-chunk (multiple times), 3) POST /finalize-session"
            );
        }

        try {
            // Create extraction directory
            String extractionDir = "code-extraction/" + UUID.randomUUID().toString();
            Path extractionPath = Paths.get(uploadDir, extractionDir);
            Files.createDirectories(extractionPath);

            List<String> imageUrls = new ArrayList<>();

            // Save all images
            for (int i = 0; i < images.size(); i++) {
                MultipartFile image = images.get(i);
                String originalFilename = image.getOriginalFilename();
                String extension = originalFilename != null && originalFilename.contains(".")
                        ? originalFilename.substring(originalFilename.lastIndexOf('.'))
                        : ".jpg";

                // Use index in filename to maintain order
                String filename = String.format("%04d_%s%s", i + 1, UUID.randomUUID().toString().substring(0, 8), extension);
                Path filePath = extractionPath.resolve(filename);
                image.transferTo(filePath.toFile());

                String imageUrl = "/uploads/" + extractionDir + "/" + filename;
                imageUrls.add(imageUrl);

                log.debug("Saved image {}: {}", i + 1, imageUrl);
            }

            // Create session
            JavaCodeExtractionSession session = extractionService.createSession(
                    userId,
                    sessionName != null ? sessionName : "Extraction " + LocalDateTime.now().toString(),
                    images.size()
            );

            // Start async processing
            extractionService.processExtractionAsync(session.getId(), imageUrls);

            return ResponseEntity.ok(JavaCodeExtractionDTO.ExtractionResponse.builder()
                    .sessionId(session.getId())
                    .sessionName(session.getSessionName())
                    .status(session.getStatus().name())
                    .totalImages(session.getTotalImages())
                    .processedImages(0)
                    .createdAt(session.getCreatedAt())
                    .build());

        } catch (IOException e) {
            log.error("Error saving images: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to save images: " + e.getMessage());
        }
    }

    /**
     * Get extraction session status
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<?> getSessionStatus(@PathVariable Long sessionId) {
        JavaCodeExtractionSession session = extractionService.getSession(sessionId);

        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(JavaCodeExtractionDTO.ExtractionResponse.builder()
                .sessionId(session.getId())
                .sessionName(session.getSessionName())
                .status(session.getStatus().name())
                .totalImages(session.getTotalImages())
                .processedImages(session.getProcessedImages())
                .extractedCode(session.getExtractedCode())
                .errorMessage(session.getErrorMessage())
                .createdAt(session.getCreatedAt())
                .completedAt(session.getCompletedAt())
                .build());
    }

    /**
     * Get all sessions for a user
     */
    @GetMapping("/sessions")
    public ResponseEntity<List<JavaCodeExtractionDTO.SessionListResponse>> getUserSessions(
            @RequestParam Long userId
    ) {
        List<JavaCodeExtractionDTO.SessionListResponse> sessions = extractionService.getUserSessions(userId);
        return ResponseEntity.ok(sessions);
    }

    /**
     * Get extracted code for a session
     */
    @GetMapping("/session/{sessionId}/code")
    public ResponseEntity<?> getExtractedCode(@PathVariable Long sessionId) {
        JavaCodeExtractionSession session = extractionService.getSession(sessionId);

        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        if (session.getStatus() != JavaCodeExtractionSession.ExtractionStatus.COMPLETED) {
            return ResponseEntity.ok(Map.of(
                    "status", session.getStatus().name(),
                    "message", "Extraction not yet completed",
                    "processedImages", session.getProcessedImages(),
                    "totalImages", session.getTotalImages()
            ));
        }

        return ResponseEntity.ok(Map.of(
                "code", session.getExtractedCode()
        ));
    }

    /**
     * Helper class for chunked upload sessions
     */
    private static class ChunkedUploadSession {
        Long userId;
        String sessionName;
        int totalImages;
        String extractionDir;
        List<String> imageUrls;
        int uploadedCount;
    }
}
