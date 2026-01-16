package com.civicconnect.api.service;

import com.civicconnect.api.config.GcpConfig;
import com.civicconnect.api.dto.JavaCodeExtractionDTO;
import com.civicconnect.api.entity.JavaCodeExtractionSession;
import com.civicconnect.api.repository.JavaCodeExtractionSessionRepository;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.Part;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.PartMaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class JavaCodeExtractionService {

    private final GcpConfig gcpConfig;
    private final JavaCodeExtractionSessionRepository sessionRepository;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    /**
     * Enhanced OCR prompt designed for maximum accuracy on code screenshots.
     * Key improvements:
     * - Explicit handling of IDE-specific elements (line numbers, gutter icons)
     * - Better guidance for partially visible text
     * - Emphasis on character-level accuracy for code
     * - Instructions for common OCR mistakes in code
     */
    private static final String OCR_PROMPT = """
        You are an expert Java/Kotlin code OCR system with perfect accuracy. Extract source code from this IDE screenshot.

        VISUAL ELEMENTS TO IGNORE:
        - Line numbers in the left gutter (1, 2, 3...)
        - IDE gutter icons (breakpoints, folding arrows, git indicators)
        - Scrollbars, tabs, file names in header
        - Status bar, breadcrumbs, minimap
        - Syntax highlighting colors (these help you read but don't output color info)

        EXTRACTION RULES:
        1. OUTPUT ONLY THE CODE - no explanations, no markdown, no code fences
        2. Character-perfect extraction: every letter, number, symbol matters
        3. Preserve exact indentation using spaces (typical: 4 spaces per level)
        4. Include ALL visible content: imports, annotations, comments, Javadocs
        5. Common OCR mistakes to avoid:
           - 'l' vs '1' vs 'I' (use context: variable names use l, numbers use 1)
           - 'O' vs '0' (class names use O, numbers use 0)
           - ';' vs ':' (Java statements end with semicolons)
           - '(' vs '{' vs '[' (match opening/closing brackets)
           - '@' annotations start with @ not 0 or a
        6. If text is cut off at edges, extract what IS visible - don't guess hidden parts
        7. For blurry/unclear characters, use Java syntax rules to determine correct character

        SPECIAL CASES:
        - String literals: preserve exact content including escape sequences (\\n, \\t, \\")
        - Generic types: <T>, <String, Integer>, etc. - don't confuse < > with ( )
        - Lambda arrows: -> not - > or =>
        - Method references: :: not : :
        - Diamond operator: <> not <>

        OUTPUT: Raw code only, maintaining original formatting. Start immediately with the first character of code visible.
        """;

    /**
     * Enhanced merge prompt for combining extracted code snippets.
     * Handles overlapping content, OCR artifacts, and multi-file codebases.
     */
    private static final String MERGE_PROMPT = """
        You are an expert Java/Kotlin developer. Merge these sequential code snippets from IDE screenshots into complete, compilable source files.

        CONTEXT:
        - Snippets are from consecutive screenshots of scrolling through code
        - Adjacent snippets likely have overlapping lines (same lines visible in both)
        - The order of snippets reflects the order in the original file(s)

        MERGE RULES:
        1. REMOVE DUPLICATES: When the end of snippet N overlaps with the start of snippet N+1, keep only one copy
        2. PRESERVE STRUCTURE: Maintain proper order - package → imports → class → fields → constructors → methods
        3. FIX OCR ARTIFACTS: Correct obvious OCR errors (missing semicolons, misread characters) based on Java syntax
        4. COMPLETE PARTIAL CODE: If a method/block is split across snippets, merge it correctly
        5. HANDLE MULTIPLE FILES: If you detect multiple classes/files, separate them with:
           // ========== FileName.java ==========

        QUALITY CHECKS:
        - All braces { } must be balanced
        - All imports should be valid (no duplicates, properly formatted)
        - Method signatures must be complete
        - String literals must be properly closed
        - Annotations must be valid (@Override, @Nullable, etc.)

        OUTPUT:
        Complete, properly formatted, compilable Java/Kotlin source code.
        No explanations, no markdown code fences, just the code.

        CODE SNIPPETS TO MERGE:
        %s
        """;

    /**
     * Create a new extraction session
     */
    public JavaCodeExtractionSession createSession(Long userId, String sessionName, int totalImages) {
        JavaCodeExtractionSession session = JavaCodeExtractionSession.builder()
                .userId(userId)
                .sessionName(sessionName != null ? sessionName : "Session " + System.currentTimeMillis())
                .status(JavaCodeExtractionSession.ExtractionStatus.PENDING)
                .totalImages(totalImages)
                .processedImages(0)
                .build();

        return sessionRepository.save(session);
    }

    /**
     * Process images and extract Java code asynchronously
     */
    @Async
    public void processExtractionAsync(Long sessionId, List<String> imageUrls) {
        JavaCodeExtractionSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            log.error("Session not found: {}", sessionId);
            return;
        }

        try {
            session.setStatus(JavaCodeExtractionSession.ExtractionStatus.PROCESSING);
            sessionRepository.save(session);

            // Sort images by name/index to maintain order
            List<String> sortedUrls = imageUrls.stream()
                    .sorted(Comparator.comparing(this::extractImageIndex))
                    .collect(Collectors.toList());

            List<String> extractedSnippets = new ArrayList<>();

            // Process each image with rate limiting and retry logic
            int consecutiveErrors = 0;
            final int MAX_CONSECUTIVE_ERRORS = 5;
            final int MAX_RETRIES = 3;
            final long BASE_DELAY_MS = 500; // 500ms between images to avoid rate limiting

            for (int i = 0; i < sortedUrls.size(); i++) {
                String imageUrl = sortedUrls.get(i);
                log.info("Processing image {}/{}: {}", i + 1, sortedUrls.size(), imageUrl);

                boolean success = false;
                Exception lastError = null;

                // Retry loop for each image
                for (int retry = 0; retry < MAX_RETRIES && !success; retry++) {
                    try {
                        if (retry > 0) {
                            // Exponential backoff on retry
                            long backoffMs = BASE_DELAY_MS * (long) Math.pow(2, retry);
                            log.info("Retry {} for image {}, waiting {}ms", retry, i + 1, backoffMs);
                            TimeUnit.MILLISECONDS.sleep(backoffMs);
                        }

                        String code = extractCodeFromImage(imageUrl);
                        if (code != null && !code.trim().isEmpty()) {
                            extractedSnippets.add("// --- Image " + (i + 1) + " ---\n" + code);
                        }
                        success = true;
                        consecutiveErrors = 0; // Reset on success

                    } catch (Exception e) {
                        lastError = e;
                        log.warn("Error on attempt {} for image {}: {}", retry + 1, i + 1, e.getMessage());

                        // Check if it's a rate limit error (429) or quota error
                        if (e.getMessage() != null &&
                            (e.getMessage().contains("429") || e.getMessage().contains("quota") ||
                             e.getMessage().contains("RESOURCE_EXHAUSTED"))) {
                            // Longer wait for rate limiting
                            try {
                                log.info("Rate limit detected, waiting 30 seconds...");
                                TimeUnit.SECONDS.sleep(30);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }
                }

                if (!success) {
                    consecutiveErrors++;
                    log.error("Failed to process image {} after {} retries: {}",
                            i + 1, MAX_RETRIES, lastError != null ? lastError.getMessage() : "Unknown error");
                    extractedSnippets.add("// --- Image " + (i + 1) + " (ERROR: " +
                            (lastError != null ? lastError.getMessage() : "Failed after retries") + ") ---\n");

                    // Stop processing if too many consecutive failures
                    if (consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
                        log.error("Too many consecutive errors ({}), stopping extraction", consecutiveErrors);
                        session.setErrorMessage("Stopped after " + consecutiveErrors + " consecutive failures at image " + (i + 1));
                        break;
                    }
                }

                session.setProcessedImages(i + 1);
                sessionRepository.save(session);

                // Rate limiting: small delay between successful API calls
                if (success && i < sortedUrls.size() - 1) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(BASE_DELAY_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            // Merge all snippets into final code
            String mergedCode;
            if (extractedSnippets.size() > 1) {
                mergedCode = mergeCodeSnippets(extractedSnippets);
            } else if (extractedSnippets.size() == 1) {
                mergedCode = extractedSnippets.get(0);
            } else {
                mergedCode = "// No code could be extracted from the images";
            }

            session.setExtractedCode(mergedCode);
            session.setStatus(JavaCodeExtractionSession.ExtractionStatus.COMPLETED);
            session.setCompletedAt(LocalDateTime.now());
            sessionRepository.save(session);

            log.info("Extraction completed for session {}: {} images processed", sessionId, sortedUrls.size());

        } catch (Exception e) {
            log.error("Extraction failed for session {}: {}", sessionId, e.getMessage(), e);
            session.setStatus(JavaCodeExtractionSession.ExtractionStatus.FAILED);
            session.setErrorMessage(e.getMessage());
            session.setCompletedAt(LocalDateTime.now());
            sessionRepository.save(session);
        }
    }

    /**
     * Extract code from a single image using Vertex AI Gemini Vision
     */
    private String extractCodeFromImage(String imageUrl) throws IOException {
        if (!gcpConfig.isAvailable()) {
            throw new IOException("GCP credentials not available");
        }

        byte[] imageBytes = loadImageBytes(imageUrl);
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IOException("Failed to load image: " + imageUrl);
        }

        String mimeType = detectMimeType(imageUrl);

        GoogleCredentials credentials = gcpConfig.getCredentials();
        if (credentials == null) {
            throw new IOException("GCP credentials are null");
        }

        try (VertexAI vertexAI = new VertexAI.Builder()
                .setProjectId(gcpConfig.getProjectId())
                .setLocation(gcpConfig.getLocation())
                .setCredentials(credentials)
                .build()) {

            GenerativeModel model = new GenerativeModel(gcpConfig.getGeminiModel(), vertexAI);

            Content content = ContentMaker.fromMultiModalData(
                    PartMaker.fromMimeTypeAndData(mimeType, imageBytes),
                    OCR_PROMPT
            );

            GenerateContentResponse response = model.generateContent(content);

            String responseText = response.getCandidatesList().stream()
                    .flatMap(candidate -> candidate.getContent().getPartsList().stream())
                    .filter(Part::hasText)
                    .map(Part::getText)
                    .collect(Collectors.joining("\n"));

            return responseText.trim();
        }
    }

    /**
     * Merge multiple code snippets into coherent Java code
     */
    private String mergeCodeSnippets(List<String> snippets) throws IOException {
        if (!gcpConfig.isAvailable()) {
            // Fallback: just concatenate
            return String.join("\n\n", snippets);
        }

        String allSnippets = String.join("\n\n", snippets);
        String prompt = String.format(MERGE_PROMPT, allSnippets);

        GoogleCredentials credentials = gcpConfig.getCredentials();
        if (credentials == null) {
            return allSnippets;
        }

        try (VertexAI vertexAI = new VertexAI.Builder()
                .setProjectId(gcpConfig.getProjectId())
                .setLocation(gcpConfig.getLocation())
                .setCredentials(credentials)
                .build()) {

            GenerativeModel model = new GenerativeModel(gcpConfig.getGeminiModel(), vertexAI);
            GenerateContentResponse response = model.generateContent(prompt);

            String mergedCode = response.getCandidatesList().stream()
                    .flatMap(candidate -> candidate.getContent().getPartsList().stream())
                    .filter(Part::hasText)
                    .map(Part::getText)
                    .collect(Collectors.joining("\n"));

            return mergedCode.trim();
        }
    }

    /**
     * Get session by ID
     */
    public JavaCodeExtractionSession getSession(Long sessionId) {
        return sessionRepository.findById(sessionId).orElse(null);
    }

    /**
     * Get all sessions for a user
     */
    public List<JavaCodeExtractionDTO.SessionListResponse> getUserSessions(Long userId) {
        return sessionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(s -> JavaCodeExtractionDTO.SessionListResponse.builder()
                        .id(s.getId())
                        .sessionName(s.getSessionName())
                        .status(s.getStatus().name())
                        .totalImages(s.getTotalImages())
                        .createdAt(s.getCreatedAt())
                        .completedAt(s.getCompletedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Load image bytes from URL or local path
     */
    private byte[] loadImageBytes(String imageUrl) throws IOException {
        if (imageUrl.startsWith("/uploads")) {
            String relativePath = imageUrl.substring("/uploads/".length());
            Path filePath = Paths.get(uploadDir, relativePath);
            if (Files.exists(filePath)) {
                return Files.readAllBytes(filePath);
            }
            return null;
        } else if (imageUrl.contains("/uploads/")) {
            String localPath = extractLocalPath(imageUrl);
            if (localPath != null) {
                Path filePath = Paths.get(uploadDir, localPath);
                if (Files.exists(filePath)) {
                    return Files.readAllBytes(filePath);
                }
            }
        }

        Path filePath = Paths.get(imageUrl);
        if (Files.exists(filePath)) {
            return Files.readAllBytes(filePath);
        }
        return null;
    }

    private String extractLocalPath(String url) {
        int uploadsIndex = url.indexOf("/uploads/");
        if (uploadsIndex != -1) {
            return url.substring(uploadsIndex + "/uploads/".length());
        }
        return null;
    }

    private String detectMimeType(String imageUrl) {
        String lowerUrl = imageUrl.toLowerCase();
        if (lowerUrl.endsWith(".png")) return "image/png";
        if (lowerUrl.endsWith(".gif")) return "image/gif";
        if (lowerUrl.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    private int extractImageIndex(String imageUrl) {
        // Try to extract number from filename for ordering
        String filename = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
        try {
            String numStr = filename.replaceAll("[^0-9]", "");
            if (!numStr.isEmpty()) {
                return Integer.parseInt(numStr.substring(0, Math.min(numStr.length(), 9)));
            }
        } catch (Exception e) {
            // Ignore
        }
        return imageUrl.hashCode();
    }
}
