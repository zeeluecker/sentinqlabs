package com.sentinq.resolution;


public record GoalFitCandidate(
        CandidateOffer offer,
        int rank,
        String reasoning
) {
}
