package com.sentinq.trust.research;

import com.sentinq.trust.ContextType;

public record ContextResearchFinding(
        ContextType type,
        String finding,
        String sourceName,
        String sourceUrl,
        String sourceExcerpt,
        double confidence
) {
}
