package com.civicconnect.api.service.ai;

import com.civicconnect.api.config.GcpConfig;
import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * Service for transcribing voice recordings using Google Cloud Speech-to-Text API
 * Handles M4A/AAC audio from Android by converting to FLAC format
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class VoiceTranscriptionService {

    private final GcpConfig gcpConfig;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${file.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Transcribe audio from a URL or local file path
     *
     * @param audioUrl URL or relative path to the audio file
     * @return Transcribed text or null if transcription fails
     */
    public String transcribe(String audioUrl) {
        if (audioUrl == null || audioUrl.isEmpty()) {
            log.debug("No audio URL provided for transcription");
            return null;
        }

        if (!gcpConfig.isAvailable()) {
            log.warn("GCP credentials not available, skipping voice transcription");
            return null;
        }

        try {
            byte[] audioBytes = loadAudioBytes(audioUrl);
            if (audioBytes == null || audioBytes.length == 0) {
                log.warn("Failed to load audio from: {}", audioUrl);
                return null;
            }

            // Detect audio format and convert if necessary
            String format = detectAudioFormat(audioUrl);
            log.info("Detected audio format: {} for file: {}", format, audioUrl);

            if ("3gp".equalsIgnoreCase(format) || "amr".equalsIgnoreCase(format)) {
                // 3GP/AMR-WB from Android - directly supported by Speech-to-Text
                return transcribeAmrWbAudio(audioBytes);
            } else if ("m4a".equalsIgnoreCase(format) || "aac".equalsIgnoreCase(format) || "mp4".equalsIgnoreCase(format)) {
                // M4A/AAC needs conversion to FLAC for Speech-to-Text
                log.info("Converting M4A/AAC audio to FLAC format...");
                audioBytes = convertToFlac(audioBytes, format);
                if (audioBytes == null) {
                    log.warn("Audio conversion failed, skipping transcription");
                    return null;
                }
                return transcribeFlacAudio(audioBytes);
            } else if ("flac".equalsIgnoreCase(format)) {
                return transcribeFlacAudio(audioBytes);
            } else if ("mp3".equalsIgnoreCase(format)) {
                return transcribeMp3Audio(audioBytes);
            } else if ("wav".equalsIgnoreCase(format)) {
                return transcribeWavAudio(audioBytes);
            } else {
                // Try with auto-detection as fallback
                log.info("Unknown format '{}', attempting auto-detection...", format);
                return transcribeWithAutoDetection(audioBytes);
            }
        } catch (Exception e) {
            log.error("Failed to transcribe audio: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Detect audio format from URL/filename
     */
    private String detectAudioFormat(String audioUrl) {
        String lower = audioUrl.toLowerCase();
        if (lower.endsWith(".3gp")) return "3gp";
        if (lower.endsWith(".amr")) return "amr";
        if (lower.endsWith(".m4a")) return "m4a";
        if (lower.endsWith(".aac")) return "aac";
        if (lower.endsWith(".mp4")) return "mp4";
        if (lower.endsWith(".mp3")) return "mp3";
        if (lower.endsWith(".flac")) return "flac";
        if (lower.endsWith(".wav")) return "wav";
        if (lower.endsWith(".ogg")) return "ogg";
        if (lower.endsWith(".webm")) return "webm";
        return "unknown";
    }

    /**
     * Convert M4A/AAC audio to FLAC using FFmpeg
     */
    private byte[] convertToFlac(byte[] inputBytes, String inputFormat) {
        Path tempInput = null;
        Path tempOutput = null;

        try {
            // Create temp files
            tempInput = Files.createTempFile("audio_input_", "." + inputFormat);
            tempOutput = Files.createTempFile("audio_output_", ".flac");

            // Write input audio to temp file
            Files.write(tempInput, inputBytes);

            // Run FFmpeg to convert to FLAC (16-bit, mono, 16kHz - optimal for Speech-to-Text)
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-y",                           // Overwrite output
                    "-i", tempInput.toString(),     // Input file
                    "-ar", "16000",                 // Sample rate 16kHz
                    "-ac", "1",                     // Mono
                    "-sample_fmt", "s16",           // 16-bit
                    "-f", "flac",                   // Output format
                    tempOutput.toString()           // Output file
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // Read process output for debugging
            StringBuilder ffmpegOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    ffmpegOutput.append(line).append("\n");
                }
            }

            boolean completed = process.waitFor(30, TimeUnit.SECONDS);
            int exitCode = process.exitValue();

            if (!completed || exitCode != 0) {
                log.error("FFmpeg conversion failed with exit code {}: {}", exitCode, ffmpegOutput);
                return null;
            }

            log.info("Audio conversion successful: {} -> FLAC", inputFormat);
            return Files.readAllBytes(tempOutput);

        } catch (IOException e) {
            log.error("IO error during audio conversion: {}", e.getMessage());
            return null;
        } catch (InterruptedException e) {
            log.error("Audio conversion interrupted");
            Thread.currentThread().interrupt();
            return null;
        } finally {
            // Cleanup temp files
            try {
                if (tempInput != null) Files.deleteIfExists(tempInput);
                if (tempOutput != null) Files.deleteIfExists(tempOutput);
            } catch (IOException e) {
                log.warn("Failed to cleanup temp files: {}", e.getMessage());
            }
        }
    }

    /**
     * Load audio bytes from URL or local file
     */
    private byte[] loadAudioBytes(String audioUrl) throws IOException {
        // Check if it's a relative path (starts with /uploads)
        if (audioUrl.startsWith("/uploads")) {
            // It's a relative path, load from local file system
            String relativePath = audioUrl.substring("/uploads/".length());
            Path filePath = Paths.get(uploadDir, relativePath);
            if (Files.exists(filePath)) {
                log.info("Loading audio from local file: {}", filePath);
                return Files.readAllBytes(filePath);
            } else {
                log.warn("Local audio file not found: {}", filePath);
                return null;
            }
        } else if (audioUrl.startsWith("http://") || audioUrl.startsWith("https://")) {
            // Check if this URL points to our own server - if so, load locally
            // This is more efficient and reliable than HTTP request to self
            if (audioUrl.contains("/uploads/")) {
                String localPath = extractLocalPath(audioUrl);
                if (localPath != null) {
                    Path filePath = Paths.get(uploadDir, localPath);
                    if (Files.exists(filePath)) {
                        log.info("Loading audio from local file (extracted from URL): {}", filePath);
                        return Files.readAllBytes(filePath);
                    }
                }
            }

            // It's a remote URL, download it
            log.info("Downloading audio from URL: {}", audioUrl);
            URL url = URI.create(audioUrl).toURL();
            try (InputStream is = url.openStream()) {
                return is.readAllBytes();
            }
        } else {
            // Try as absolute local path
            Path filePath = Paths.get(audioUrl);
            if (Files.exists(filePath)) {
                log.info("Loading audio from absolute path: {}", filePath);
                return Files.readAllBytes(filePath);
            }
            log.warn("Unknown audio URL format: {}", audioUrl);
            return null;
        }
    }

    /**
     * Extract local path from full URL
     * e.g., "http://192.168.x.x:8080/uploads/issues/audio/file.3gp" -> "issues/audio/file.3gp"
     */
    private String extractLocalPath(String url) {
        int uploadsIndex = url.indexOf("/uploads/");
        if (uploadsIndex != -1) {
            return url.substring(uploadsIndex + "/uploads/".length());
        }
        return null;
    }

    /**
     * Transcribe AMR-WB audio (from Android 3GP files)
     * AMR-WB is recorded at 16kHz - perfect for speech recognition
     */
    private String transcribeAmrWbAudio(byte[] audioBytes) throws IOException {
        log.info("Transcribing AMR-WB audio: {} bytes", audioBytes.length);

        try (SpeechClient speechClient = SpeechClient.create(
                SpeechSettings.newBuilder()
                        .setCredentialsProvider(() -> gcpConfig.getCredentials())
                        .build())) {

            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.AMR_WB)
                    .setSampleRateHertz(16000)  // AMR-WB is always 16kHz
                    .setAudioChannelCount(1)
                    .setLanguageCode(gcpConfig.getSpeechLanguageCode())
                    .addAlternativeLanguageCodes("hi-IN")
                    .setEnableAutomaticPunctuation(true)
                    .setModel("latest_long")
                    .build();

            return performRecognition(speechClient, config, audioBytes);
        }
    }

    /**
     * Transcribe FLAC audio (16-bit, 16kHz, mono)
     */
    private String transcribeFlacAudio(byte[] audioBytes) throws IOException {
        log.info("Transcribing FLAC audio: {} bytes", audioBytes.length);

        try (SpeechClient speechClient = SpeechClient.create(
                SpeechSettings.newBuilder()
                        .setCredentialsProvider(() -> gcpConfig.getCredentials())
                        .build())) {

            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.FLAC)
                    .setSampleRateHertz(16000)
                    .setAudioChannelCount(1)
                    .setLanguageCode(gcpConfig.getSpeechLanguageCode())
                    .addAlternativeLanguageCodes("hi-IN")
                    .setEnableAutomaticPunctuation(true)
                    .setModel("latest_long")
                    .build();

            return performRecognition(speechClient, config, audioBytes);
        }
    }

    /**
     * Transcribe MP3 audio
     */
    private String transcribeMp3Audio(byte[] audioBytes) throws IOException {
        log.info("Transcribing MP3 audio: {} bytes", audioBytes.length);

        try (SpeechClient speechClient = SpeechClient.create(
                SpeechSettings.newBuilder()
                        .setCredentialsProvider(() -> gcpConfig.getCredentials())
                        .build())) {

            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.MP3)
                    .setLanguageCode(gcpConfig.getSpeechLanguageCode())
                    .addAlternativeLanguageCodes("hi-IN")
                    .setEnableAutomaticPunctuation(true)
                    .setModel("latest_long")
                    .build();

            return performRecognition(speechClient, config, audioBytes);
        }
    }

    /**
     * Transcribe WAV audio (LINEAR16)
     */
    private String transcribeWavAudio(byte[] audioBytes) throws IOException {
        log.info("Transcribing WAV audio: {} bytes", audioBytes.length);

        try (SpeechClient speechClient = SpeechClient.create(
                SpeechSettings.newBuilder()
                        .setCredentialsProvider(() -> gcpConfig.getCredentials())
                        .build())) {

            // WAV typically contains header with sample rate, so we use ENCODING_UNSPECIFIED
            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                    .setSampleRateHertz(16000)
                    .setAudioChannelCount(1)
                    .setLanguageCode(gcpConfig.getSpeechLanguageCode())
                    .addAlternativeLanguageCodes("hi-IN")
                    .setEnableAutomaticPunctuation(true)
                    .setModel("latest_long")
                    .build();

            return performRecognition(speechClient, config, audioBytes);
        }
    }

    /**
     * Transcribe with auto-detection (fallback)
     */
    private String transcribeWithAutoDetection(byte[] audioBytes) throws IOException {
        log.info("Transcribing with auto-detection: {} bytes", audioBytes.length);

        try (SpeechClient speechClient = SpeechClient.create(
                SpeechSettings.newBuilder()
                        .setCredentialsProvider(() -> gcpConfig.getCredentials())
                        .build())) {

            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.ENCODING_UNSPECIFIED)
                    .setLanguageCode(gcpConfig.getSpeechLanguageCode())
                    .addAlternativeLanguageCodes("hi-IN")
                    .setEnableAutomaticPunctuation(true)
                    .setModel("latest_long")
                    .build();

            return performRecognition(speechClient, config, audioBytes);
        }
    }

    /**
     * Common recognition logic
     */
    private String performRecognition(SpeechClient speechClient, RecognitionConfig config, byte[] audioBytes) {
        RecognitionAudio audio = RecognitionAudio.newBuilder()
                .setContent(ByteString.copyFrom(audioBytes))
                .build();

        RecognizeResponse response = speechClient.recognize(config, audio);

        StringBuilder transcription = new StringBuilder();
        for (SpeechRecognitionResult result : response.getResultsList()) {
            if (!result.getAlternativesList().isEmpty()) {
                SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
                transcription.append(alternative.getTranscript()).append(" ");
                log.debug("Transcribed segment: {} (confidence: {})",
                        alternative.getTranscript(), alternative.getConfidence());
            }
        }

        String result = transcription.toString().trim();
        log.info("Transcription completed: {} characters", result.length());
        return result.isEmpty() ? null : result;
    }
}
