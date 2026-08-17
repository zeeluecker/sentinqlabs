package com.sentinq.trust.synthesis;

import com.sentinq.trust.ContextType;
import com.sentinq.trust.TrustDimension;

public record MaterialTrustQuestion(
        TrustDimension dimension,
        ContextType contextType,
        String question,
        String reason
) {
}
