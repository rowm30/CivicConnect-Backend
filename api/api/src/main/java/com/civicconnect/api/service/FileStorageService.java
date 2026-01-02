package com.civicconnect.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Service for handling file uploads and storage
 * Stores files on local filesystem and returns accessible URLs
 */
@Service
@Slf4j
public class FileStorageService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${file.base-url:http://localhost:8080}")
    private String baseUrl;

    private Path uploadPath;

    @PostConstruct
    public void init() {
        try {
            uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);
            log.info("File storage initialized at: {}", uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    /**
     * Store an uploaded file and return its URL
     *
     * @param file The multipart file to store
     * @param subfolder Optional subfolder (e.g., "issues", "profiles")
     * @return The URL to access the stored file
     */
    public String storeFile(MultipartFile file, String subfolder) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty file");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());

        // Validate filename
        if (originalFilename.contains("..")) {
            throw new IllegalArgumentException("Filename contains invalid path sequence: " + originalFilename);
        }

        // Generate unique filename
        String extension = getFileExtension(originalFilename);
        String newFilename = UUID.randomUUID().toString() + extension;

        try {
            // Create subfolder if specified
            Path targetDir = uploadPath;
            if (subfolder != null && !subfolder.isEmpty()) {
                targetDir = uploadPath.resolve(subfolder);
                Files.createDirectories(targetDir);
            }

            // Copy file to target location
            Path targetPath = targetDir.resolve(newFilename);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Stored file: {} at {}", originalFilename, targetPath);

            // Return the accessible URL
            String relativePath = subfolder != null ? subfolder + "/" + newFilename : newFilename;
            return baseUrl + "/uploads/" + relativePath;

        } catch (IOException e) {
            log.error("Failed to store file: {}", originalFilename, e);
            throw new RuntimeException("Failed to store file: " + originalFilename, e);
        }
    }

    /**
     * Store file specifically for issues
     */
    public String storeIssueImage(MultipartFile file) {
        return storeFile(file, "issues");
    }

    /**
     * Store file specifically for user profiles
     */
    public String storeProfileImage(MultipartFile file) {
        return storeFile(file, "profiles");
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg"; // Default extension
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    /**
     * Delete a file by its URL
     */
    public boolean deleteFile(String fileUrl) {
        if (fileUrl == null || !fileUrl.contains("/uploads/")) {
            return false;
        }

        try {
            String relativePath = fileUrl.substring(fileUrl.indexOf("/uploads/") + 9);
            Path filePath = uploadPath.resolve(relativePath);
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("Failed to delete file: {}", fileUrl, e);
            return false;
        }
    }

    /**
     * Get the upload path for serving static files
     */
    public Path getUploadPath() {
        return uploadPath;
    }
}
