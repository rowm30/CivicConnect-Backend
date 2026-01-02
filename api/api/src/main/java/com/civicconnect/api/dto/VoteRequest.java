package com.civicconnect.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for voting on an issue
 */
@Data
public class VoteRequest {

    @NotNull(message = "Vote type is required")
    private String voteType; // "UPVOTE" or "DOWNVOTE"
}
