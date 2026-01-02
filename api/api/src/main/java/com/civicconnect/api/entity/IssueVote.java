package com.civicconnect.api.entity;

import com.civicconnect.api.entity.analytics.AppUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * IssueVote entity for tracking user votes on issues
 * Supports upvote/downvote for heat score calculation
 */
@Entity
@Table(name = "issue_votes",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_issue_vote_user",
        columnNames = {"issue_id", "user_id"}
    ),
    indexes = {
        @Index(name = "idx_vote_issue", columnList = "issue_id"),
        @Index(name = "idx_vote_user", columnList = "user_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class IssueVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VoteType voteType;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public IssueVote(Issue issue, AppUser user, VoteType voteType) {
        this.issue = issue;
        this.user = user;
        this.voteType = voteType;
    }

    public enum VoteType {
        UPVOTE,
        DOWNVOTE
    }
}
