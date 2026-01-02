package com.civicconnect.api.service.auth;

import com.civicconnect.api.dto.auth.AuthDTOs.*;
import com.civicconnect.api.entity.analytics.AppUser;
import com.civicconnect.api.repository.analytics.AppUserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleAuthService {

    private final AppUserRepository appUserRepository;

    @Value("${google.client-id:135755435543-h985hivmurvo17rrkatsat61i5cpi207.apps.googleusercontent.com}")
    private String googleClientId;

    @Transactional
    public AuthResponse authenticateWithGoogle(String idTokenString) {
        try {
            log.info("Authenticating with Google ID token");

            // Verify the Google ID token
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                log.error("Invalid Google ID token");
                return AuthResponse.error("Invalid ID token", "INVALID_ID_TOKEN");
            }

            Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String picture = (String) payload.get("picture");
            String googleId = payload.getSubject(); // Google UID

            log.info("Google auth successful for: {}", email);

            // Find or create user
            AppUser user = appUserRepository.findByGoogleId(googleId)
                    .orElseGet(() -> {
                        AppUser newUser = new AppUser();
                        newUser.setGoogleId(googleId);
                        newUser.setEmail(email);
                        newUser.setName(name);
                        newUser.setPhotoUrl(picture);
                        newUser.setPlatform(AppUser.Platform.ANDROID);
                        return appUserRepository.save(newUser);
                    });

            // Update user info
            user.setName(name);
            user.setPhotoUrl(picture);
            user.setLastLoginAt(LocalDateTime.now());
            appUserRepository.save(user);

            // Generate a simple JWT-like token (for demo - in production use proper JWT)
            String accessToken = generateSimpleToken(user);
            String refreshToken = generateSimpleToken(user);

            JwtResponse jwt = JwtResponse.builder()
                    .accessToken(accessToken)
                    .tokenType("Bearer")
                    .expiresIn(86400L) // 24 hours
                    .refreshToken(refreshToken)
                    .user(UserDto.builder()
                            .id(user.getId().toString())
                            .email(user.getEmail())
                            .name(user.getName())
                            .build())
                    .build();

            return AuthResponse.success("Authenticated successfully", jwt);

        } catch (Exception e) {
            log.error("Google authentication failed", e);
            return AuthResponse.error("Authentication failed", e.getMessage());
        }
    }

    private String generateSimpleToken(AppUser user) {
        // Simple token: base64(userId:email:timestamp:random)
        String tokenData = String.format("%d:%s:%d:%s",
                user.getId(),
                user.getEmail(),
                System.currentTimeMillis(),
                UUID.randomUUID().toString());
        return Base64.getEncoder().encodeToString(tokenData.getBytes());
    }
}
