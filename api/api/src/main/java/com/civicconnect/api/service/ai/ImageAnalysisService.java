package com.civicconnect.api.service.ai;

import com.civicconnect.api.config.GcpConfig;
import com.civicconnect.api.dto.IssueAnalysisResponse;
import com.civicconnect.api.entity.Issue;
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
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for analyzing issue images using Google Gemini Vision API
 * Extracts issue details, category, and priority from photos
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ImageAnalysisService {

    private final GcpConfig gcpConfig;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    // Prompt template for citizen complaint generation
    private static final String ANALYSIS_PROMPT = """
            You are an AI assistant helping Indian citizens file formal civic complaints to government authorities.

            A citizen has captured a photo of a civic issue they want to report. Your job is to:
            1. Analyze the image to understand what civic problem is shown
            2. Generate a formal complaint that the citizen can submit to local government

            CITIZEN'S INPUT:
            %s

            IMPORTANT INSTRUCTIONS:
            - Write as if YOU are the citizen filing this complaint
            - Be SPECIFIC about what you observe in the image (size, location, condition)
            - Use formal but clear language suitable for government submission
            - Focus on FACTS visible in the image, not assumptions
            - If voice description is provided, incorporate those details naturally
            - The description should explain WHY this needs government attention

            Respond with EXACTLY this format:

            TITLE: [Specific issue title - what and where, max 10 words]
            Examples of GOOD titles:
            - "Damaged Road Surface Near Market Area"
            - "Overflowing Garbage Bin at Street Corner"
            - "Broken Street Light Causing Safety Hazard"
            Examples of BAD titles (too generic):
            - "Road Problem"
            - "Issue in Area"

            DESCRIPTION: [3-4 sentences as a formal citizen complaint that includes:
            1. What the problem is (be specific about what you see)
            2. Observable details (size, extent, condition)
            3. Impact on citizens/public safety/convenience
            4. Request for action]

            Example GOOD description:
            "I am reporting a significant road damage near the main intersection. The damaged section spans approximately 2 meters and has exposed underlying material, creating a hazard for vehicles and pedestrians. This issue is causing traffic disruptions and poses risk of accidents, especially during evening hours. I request the concerned authorities to inspect and repair this at the earliest."

            CATEGORY: [One of: ROADS, WATER, ELECTRICITY, WASTE, SAFETY, PARKS, BUILDING, TRAFFIC, NOISE, OTHER]

            PRIORITY: [One of: LOW, MEDIUM, HIGH, URGENT]
            - URGENT: Immediate danger to life (exposed wires, collapsed structure, open manholes)
            - HIGH: Significant safety risk or major inconvenience (large potholes, broken lights on main road)
            - MEDIUM: Moderate issue needing attention (small potholes, overflowing bins)
            - LOW: Minor issue (faded road markings, minor cracks)

            CONFIDENCE: [0.0 to 1.0 - how confident you are in your analysis]

            Category Guidelines:
            - ROADS: Potholes, broken roads, damaged footpaths, missing road signs, speed breaker issues
            - WATER: Leaking pipes, waterlogging, contaminated water, broken taps, drainage issues
            - ELECTRICITY: Broken/non-functional street lights, exposed wires, transformer issues
            - WASTE: Garbage accumulation, overflowing bins, illegal dumping, lack of dustbins
            - SAFETY: Dangerous structures, missing railings, open manholes, construction hazards
            - PARKS: Damaged parks, broken equipment, unmaintained public spaces
            - BUILDING: Damaged government buildings, illegal encroachments on public land
            - TRAFFIC: Broken signals, missing/damaged signs, road marking issues
            - NOISE: Construction noise, loudspeaker violations, industrial noise
            - OTHER: Issues not fitting above categories
            """;

    /**
     * Analyze an image and optional voice transcription to generate issue suggestions
     *
     * @param imageUrl URL or path to the issue image
     * @param voiceTranscription Transcribed text from voice recording (can be null)
     * @param locationName Human-readable location name
     * @param latitude GPS latitude (can be null)
     * @param longitude GPS longitude (can be null)
     */
    public IssueAnalysisResponse analyzeIssue(String imageUrl, String voiceTranscription,
                                               String locationName, Double latitude, Double longitude) {
        if (!gcpConfig.isAvailable()) {
            log.warn("GCP credentials not available, returning failure");
            return IssueAnalysisResponse.failure("AI service not configured");
        }

        try {
            byte[] imageBytes = loadImageBytes(imageUrl);
            if (imageBytes == null || imageBytes.length == 0) {
                return IssueAnalysisResponse.failure("Failed to load image");
            }

            String mimeType = detectMimeType(imageUrl);
            return analyzeWithGemini(imageBytes, mimeType, voiceTranscription, locationName, latitude, longitude);

        } catch (Exception e) {
            log.error("Image analysis failed: {}", e.getMessage(), e);
            return IssueAnalysisResponse.failure("Analysis failed: " + e.getMessage());
        }
    }

    /**
     * Load image bytes from URL or local file
     */
    private byte[] loadImageBytes(String imageUrl) throws IOException {
        if (imageUrl.startsWith("/uploads")) {
            String relativePath = imageUrl.substring("/uploads/".length());
            Path filePath = Paths.get(uploadDir, relativePath);
            if (Files.exists(filePath)) {
                log.info("Loading image from local file: {}", filePath);
                return Files.readAllBytes(filePath);
            }
            log.warn("Local image file not found: {}", filePath);
            return null;
        } else if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            // Check if this URL points to our own server - if so, load locally
            // This is more efficient and reliable than HTTP request to self
            if (imageUrl.contains("/uploads/")) {
                String localPath = extractLocalPath(imageUrl);
                if (localPath != null) {
                    Path filePath = Paths.get(uploadDir, localPath);
                    if (Files.exists(filePath)) {
                        log.info("Loading image from local file (extracted from URL): {}", filePath);
                        return Files.readAllBytes(filePath);
                    }
                }
            }

            // It's a remote URL, download it
            log.info("Downloading image from URL: {}", imageUrl);
            URL url = URI.create(imageUrl).toURL();
            try (InputStream is = url.openStream()) {
                return is.readAllBytes();
            }
        } else {
            Path filePath = Paths.get(imageUrl);
            if (Files.exists(filePath)) {
                return Files.readAllBytes(filePath);
            }
            return null;
        }
    }

    /**
     * Extract local path from full URL
     * e.g., "http://192.168.x.x:8080/uploads/issues/file.jpg" -> "issues/file.jpg"
     */
    private String extractLocalPath(String url) {
        int uploadsIndex = url.indexOf("/uploads/");
        if (uploadsIndex != -1) {
            return url.substring(uploadsIndex + "/uploads/".length());
        }
        return null;
    }

    /**
     * Detect MIME type from file extension
     */
    private String detectMimeType(String imageUrl) {
        String lowerUrl = imageUrl.toLowerCase();
        if (lowerUrl.endsWith(".png")) return "image/png";
        if (lowerUrl.endsWith(".gif")) return "image/gif";
        if (lowerUrl.endsWith(".webp")) return "image/webp";
        return "image/jpeg"; // Default
    }

    /**
     * Analyze image using Gemini Vision API
     */
    private IssueAnalysisResponse analyzeWithGemini(byte[] imageBytes, String mimeType,
                                                     String voiceTranscription, String locationName,
                                                     Double latitude, Double longitude) throws IOException {
        log.info("Starting Gemini Vision analysis for {} bytes image", imageBytes.length);

        // Get credentials from GcpConfig
        GoogleCredentials credentials = gcpConfig.getCredentials();
        if (credentials == null) {
            log.error("GCP credentials are null");
            return IssueAnalysisResponse.failure("GCP credentials not configured");
        }

        // Build VertexAI with explicit credentials
        try (VertexAI vertexAI = new VertexAI.Builder()
                .setProjectId(gcpConfig.getProjectId())
                .setLocation(gcpConfig.getLocation())
                .setCredentials(credentials)
                .build()) {

            GenerativeModel model = new GenerativeModel(gcpConfig.getGeminiModel(), vertexAI);

            // Build rich context from voice transcription, location, and GPS
            StringBuilder context = new StringBuilder();

            // Location context with GPS coordinates
            if (locationName != null && !locationName.isEmpty()) {
                context.append("📍 LOCATION: ").append(locationName);
                if (latitude != null && longitude != null) {
                    context.append(String.format(" (GPS: %.6f, %.6f)", latitude, longitude));
                }
                context.append("\n");
            } else if (latitude != null && longitude != null) {
                context.append(String.format("📍 GPS COORDINATES: %.6f, %.6f\n", latitude, longitude));
            } else {
                context.append("📍 LOCATION: Not specified\n");
            }

            // Voice description - this is the citizen's own words describing the issue
            if (voiceTranscription != null && !voiceTranscription.isEmpty()) {
                context.append("\n🎤 CITIZEN'S VOICE DESCRIPTION:\n");
                context.append("\"").append(voiceTranscription).append("\"\n");
                context.append("\n(IMPORTANT: The citizen has provided a voice description. ");
                context.append("Use their words to understand the issue better and incorporate ");
                context.append("relevant details into the formal complaint. Their description may ");
                context.append("contain information not visible in the image.)\n");
            } else {
                context.append("\n🎤 CITIZEN'S VOICE DESCRIPTION: None provided\n");
                context.append("(Analyze the image only to generate the complaint.)\n");
            }

            String prompt = String.format(ANALYSIS_PROMPT, context.toString());

            log.info("Generated prompt context:\n{}", context.toString());

            // Create content with image and text
            Content content = ContentMaker.fromMultiModalData(
                    PartMaker.fromMimeTypeAndData(mimeType, imageBytes),
                    prompt
            );

            // Generate response
            GenerateContentResponse response = model.generateContent(content);

            // Extract text from response
            String responseText = response.getCandidatesList().stream()
                    .flatMap(candidate -> candidate.getContent().getPartsList().stream())
                    .filter(Part::hasText)
                    .map(Part::getText)
                    .collect(Collectors.joining("\n"));

            log.debug("Gemini response: {}", responseText);

            return parseGeminiResponse(responseText, voiceTranscription);
        }
    }

    /**
     * Parse Gemini response into structured IssueAnalysisResponse
     */
    private IssueAnalysisResponse parseGeminiResponse(String response, String voiceTranscription) {
        try {
            String title = extractField(response, "TITLE");
            String description = extractField(response, "DESCRIPTION");
            String category = extractField(response, "CATEGORY");
            String priority = extractField(response, "PRIORITY");
            String confidenceStr = extractField(response, "CONFIDENCE");

            // Validate category
            if (!isValidCategory(category)) {
                log.warn("Invalid category from Gemini: {}, defaulting to OTHER", category);
                category = "OTHER";
            }

            // Validate priority
            if (!isValidPriority(priority)) {
                log.warn("Invalid priority from Gemini: {}, defaulting to MEDIUM", priority);
                priority = "MEDIUM";
            }

            // Parse confidence
            float confidence = 0.7f;
            try {
                confidence = Float.parseFloat(confidenceStr);
                confidence = Math.min(1.0f, Math.max(0.0f, confidence));
            } catch (NumberFormatException e) {
                log.debug("Could not parse confidence: {}", confidenceStr);
            }

            log.info("Analysis complete - Category: {}, Priority: {}, Confidence: {}", category, priority, confidence);

            return IssueAnalysisResponse.success(
                    title != null ? title : "Civic Issue",
                    description != null ? description : "Issue requires attention",
                    category,
                    priority,
                    voiceTranscription,
                    confidence
            );

        } catch (Exception e) {
            log.error("Failed to parse Gemini response: {}", e.getMessage());
            return IssueAnalysisResponse.failure("Failed to parse analysis result");
        }
    }

    /**
     * Extract a field value from the response
     */
    private String extractField(String response, String fieldName) {
        Pattern pattern = Pattern.compile(fieldName + ":\\s*(.+?)(?=\\n[A-Z]+:|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * Check if category is valid
     */
    private boolean isValidCategory(String category) {
        if (category == null) return false;
        return Arrays.stream(Issue.IssueCategory.values())
                .anyMatch(c -> c.name().equalsIgnoreCase(category));
    }

    /**
     * Check if priority is valid
     */
    private boolean isValidPriority(String priority) {
        if (priority == null) return false;
        return Arrays.stream(Issue.IssuePriority.values())
                .anyMatch(p -> p.name().equalsIgnoreCase(priority));
    }
}
