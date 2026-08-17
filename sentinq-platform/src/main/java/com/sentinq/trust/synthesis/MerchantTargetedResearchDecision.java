package com.sentinq.trust.synthesis;

import java.util.List;

public record MerchantTargetedResearchDecision(
        List<MerchantTargetedResearchFinding> findings
) {
}
