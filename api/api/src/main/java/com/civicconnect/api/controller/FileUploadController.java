package com.civicconnect.api.controller;

import com.civicconnect.api.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for handling file uploads
 * Provides endpoints for uploading issue images, profile pictures, etc.
 */
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class FileUploadController {

    private final FileStorageService fileStorageService;

    /**
     * Upload an image for an issue
     * Returns the URL of the uploaded image
     */
    @PostMapping(value = "/issue-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadIssueImage(
            @RequestParam("file") MultipartFile file
    ) {
        log.info("Uploading issue image: {}, size: {} bytes",
                file.getOriginalFilename(), file.getSize());

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Only image files are allowed");
            return ResponseEntity.badRequest().body(error);
        }

        // Validate file size (max 10MB)
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (file.getSize() > maxSize) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "File size exceeds maximum limit of 10MB");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            String imageUrl = fileStorageService.storeIssueImage(file);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("imageUrl", imageUrl);
            response.put("fileName", file.getOriginalFilename());
            response.put("fileSize", file.getSize());

            log.info("Issue image uploaded successfully: {}", imageUrl);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to upload issue image", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to upload image: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Upload a profile picture
     * Returns the URL of the uploaded image
     */
    @PostMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadProfileImage(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        log.info("Uploading profile image for user: {}", userId);

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Only image files are allowed");
            return ResponseEntity.badRequest().body(error);
        }

        // Validate file size (max 5MB for profile pics)
        long maxSize = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > maxSize) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "File size exceeds maximum limit of 5MB");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            String imageUrl = fileStorageService.storeProfileImage(file);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("imageUrl", imageUrl);

            log.info("Profile image uploaded successfully: {}", imageUrl);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to upload profile image", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to upload image: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Generic file upload endpoint
     */
    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "general") String folder
    ) {
        log.info("Uploading file to folder: {}", folder);

        // Validate file size (max 20MB)
        long maxSize = 20 * 1024 * 1024; // 20MB
        if (file.getSize() > maxSize) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "File size exceeds maximum limit of 20MB");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            String fileUrl = fileStorageService.storeFile(file, folder);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("fileUrl", fileUrl);
            response.put("fileName", file.getOriginalFilename());
            response.put("fileSize", file.getSize());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to upload file", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to upload file: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
