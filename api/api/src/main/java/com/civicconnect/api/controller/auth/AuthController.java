package com.civicconnect.api.controller.auth;

import com.civicconnect.api.dto.auth.AuthDTOs.*;
import com.civicconnect.api.service.auth.GoogleAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller for mobile app login.
 * Provides Google OAuth sign-in endpoint compatible with Android app.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AuthController {

    private final GoogleAuthService googleAuthService;

    /**
     * Google Sign-In endpoint for Android app.
     * Receives Google ID token from client and returns JWT tokens.
     */
    @PostMapping("/google/signin")
    public ResponseEntity<AuthResponse> googleSignIn(@RequestBody GoogleSignInRequest request) {
        log.info("Google sign-in request received");

        if (request.getIdToken() == null || request.getIdToken().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(AuthResponse.error("ID token is required", "MISSING_TOKEN"));
        }

        AuthResponse response = googleAuthService.authenticateWithGoogle(request.getIdToken());

        if (response.isSuccess()) {
            log.info("Google sign-in successful");
            return ResponseEntity.ok(response);
        } else {
            log.warn("Google sign-in failed: {}", response.getError());
            return ResponseEntity.status(401).body(response);
        }
    }

    /**
     * Health check for auth service
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth service is running");
    }
}
