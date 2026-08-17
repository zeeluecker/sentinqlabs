package com.sentinq.trust.synthesis;

import com.sentinq.trust.TrustDimension;
import com.sentinq.trust.TrustSignal;

import java.util.List;

public record TrustEvidenceTheme(
        TrustDimension dimension,
        String theme,
        TrustSignal signal,
        List<String> evidenceIds
) {
}
