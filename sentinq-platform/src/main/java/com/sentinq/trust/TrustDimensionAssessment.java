package com.sentinq.trust;

// TrustDimensionAssessment.java

import java.util.List;

public record TrustDimensionAssessment(
        TrustDimension dimension,
        TrustRating rating,
        double confidence,
        List<String> positivePatterns,
        List<String> negativePatterns,
        List<String> contextualNuances,
        List<String> supportingInterpretationIds
) {
}
