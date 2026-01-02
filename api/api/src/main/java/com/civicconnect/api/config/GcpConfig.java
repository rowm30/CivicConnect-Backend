package com.civicconnect.api.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Google Cloud Platform configuration for ML services
 * Handles credentials loading and provides GCP settings
 */
@Configuration
@Slf4j
@Getter
public class GcpConfig {

    @Value("${gcp.project-id}")
    private String projectId;

    @Value("${gcp.location}")
    private String location;

    @Value("${gcp.credentials-path}")
    private String credentialsPath;

    @Value("${gcp.speech.language-code}")
    private String speechLanguageCode;

    @Value("${gcp.gemini.model}")
    private String geminiModel;

    private GoogleCredentials credentials;
    private boolean credentialsLoaded = false;

    @PostConstruct
    public void init() {
        loadCredentials();
    }

    // OAuth scopes required for GCP services
    private static final String CLOUD_PLATFORM_SCOPE = "https://www.googleapis.com/auth/cloud-platform";

    private void loadCredentials() {
        try {
            Path path = Paths.get(credentialsPath);

            if (Files.exists(path)) {
                // Load credentials and add required OAuth scopes
                GoogleCredentials baseCredentials = ServiceAccountCredentials.fromStream(new FileInputStream(path.toFile()));
                credentials = baseCredentials.createScoped(CLOUD_PLATFORM_SCOPE);
                credentialsLoaded = true;
                log.info("GCP credentials loaded successfully from: {} with cloud-platform scope", credentialsPath);
            } else {
                // Try to use Application Default Credentials (ADC) with scopes
                GoogleCredentials baseCredentials = GoogleCredentials.getApplicationDefault();
                credentials = baseCredentials.createScoped(CLOUD_PLATFORM_SCOPE);
                credentialsLoaded = true;
                log.info("GCP credentials loaded from Application Default Credentials with cloud-platform scope");
            }
        } catch (IOException e) {
            log.warn("Failed to load GCP credentials: {}. AI features will be disabled.", e.getMessage());
            credentialsLoaded = false;
        }
    }

    @Bean
    public GoogleCredentials googleCredentials() {
        return credentials;
    }

    /**
     * Check if GCP services are available
     */
    public boolean isAvailable() {
        return credentialsLoaded && credentials != null;
    }

    /**
     * Get the full Gemini model path for Vertex AI
     */
    public String getGeminiModelPath() {
        return String.format("projects/%s/locations/%s/publishers/google/models/%s",
                projectId, location, geminiModel);
    }
}
