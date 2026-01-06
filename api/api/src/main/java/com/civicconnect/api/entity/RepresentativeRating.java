package com.civicconnect.api.entity;

import com.civicconnect.api.entity.analytics.AppUser;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity to store user ratings for elected representatives
 * Supports ratings for MLA, MP, Ward Councillor, Mayor, CM, PM
 */
@Entity
@Table(name = "representative_ratings",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"user_id", "representative_type", "representative_id"},
        name = "uk_user_representative_rating"
    ),
    indexes = {
        @Index(name = "idx_rating_rep_type_id", columnList = "representative_type, representative_id"),
        @Index(name = "idx_rating_user", columnList = "user_id"),
        @Index(name = "idx_rating_created", columnList = "created_at DESC")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepresentativeRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    /**
     * Type of representative being rated
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "representative_type", nullable = false)
    private RepresentativeType representativeType;

    /**
     * ID of the representative (from respective table)
     * For MLA: mla_id, For MP: mp_id, For Councillor: councillor_id
     * For Mayor/CM/PM: we use a special ID (e.g., state code for CM)
     */
    @Column(name = "representative_id", nullable = false)
    private Long representativeId;

    /**
     * Name of the representative (denormalized for quick display)
     */
    @Column(name = "representative_name")
    private String representativeName;

    /**
     * Party affiliation (denormalized)
     */
    @Column(name = "party_name")
    private String partyName;

    /**
     * Constituency/Ward name (denormalized)
     */
    @Column(name = "constituency_name")
    private String constituencyName;

    /**
     * Rating from 1-5 stars
     */
    @Column(nullable = false)
    private Integer rating;

    /**
     * Optional comment/feedback
     */
    @Column(columnDefinition = "TEXT")
    private String comment;

    /**
     * Specific aspects rated (JSON or comma-separated)
     * e.g., "accessibility:4,responsiveness:3,effectiveness:5"
     */
    @Column(name = "aspect_ratings")
    private String aspectRatings;

    /**
     * Is this rating anonymous?
     */
    @Column(name = "is_anonymous")
    @Builder.Default
    private Boolean isAnonymous = false;

    /**
     * Has this rating been verified (e.g., user is from that constituency)?
     */
    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Types of representatives that can be rated
     */
    public enum RepresentativeType {
        WARD_COUNCILLOR,    // Municipal ward councillor
        MAYOR,              // City mayor
        MLA,                // Member of Legislative Assembly
        CHIEF_MINISTER,     // State CM
        MP,                 // Member of Parliament
        PRIME_MINISTER      // PM
    }
}
