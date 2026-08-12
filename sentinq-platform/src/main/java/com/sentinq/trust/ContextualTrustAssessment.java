package com.sentinq.trust;
// ContextualTrustAssessment.java

import java.util.List;

public record ContextualTrustAssessment(
        TrustRating rating,
        double confidence,
        List<TrustDimension> decisiveDimensions,
        List<String> strengths,
        List<String> concerns,
        List<String> unresolvedQuestions,
        boolean additionalResearchRequired,
        String rationale
) {
}
