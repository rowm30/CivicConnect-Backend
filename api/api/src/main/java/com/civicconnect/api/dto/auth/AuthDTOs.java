package com.civicconnect.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AuthDTOs {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GoogleSignInRequest {
        private String idToken;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AuthResponse {
        private boolean success;
        private String message;
        private JwtResponse data;
        private String error;

        public static AuthResponse success(String message, JwtResponse data) {
            return AuthResponse.builder()
                    .success(true)
                    .message(message)
                    .data(data)
                    .build();
        }

        public static AuthResponse error(String message, String error) {
            return AuthResponse.builder()
                    .success(false)
                    .message(message)
                    .error(error)
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JwtResponse {
        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("token_type")
        private String tokenType;

        @JsonProperty("expires_in")
        private Long expiresIn;

        @JsonProperty("refresh_token")
        private String refreshToken;

        private UserDto user;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDto {
        private String id;
        private String email;
        private String name;
    }
}
