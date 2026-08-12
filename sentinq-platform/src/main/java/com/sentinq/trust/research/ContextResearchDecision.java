package com.sentinq.trust.research;

import java.util.List;

public record ContextResearchDecision(
        List<ContextResearchFinding> findings
) {
}
