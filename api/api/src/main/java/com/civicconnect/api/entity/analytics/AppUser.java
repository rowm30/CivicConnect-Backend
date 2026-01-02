package com.civicconnect.api.entity.analytics;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "app_users")
@Getter
@Setter
@NoArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "google_id", unique = true, nullable = false)
    private String googleId;

    @Column(unique = true, nullable = false)
    private String email;

    private String name;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "fcm_token")
    private String fcmToken;

    @Column(name = "app_version")
    private String appVersion;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Platform platform = Platform.ANDROID;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserSession> sessions = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ActivityLog> activities = new ArrayList<>();

    public enum Platform {
        ANDROID, IOS, WEB
    }
}
