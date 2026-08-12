package com.sentinq.trust.research;

import com.sentinq.trust.ContextRequirement;
import com.sentinq.trust.TrustContext;
import com.sentinq.trust.TrustEvidence;

import java.util.List;

public interface ContextResearchProvider {

    String getProviderId();

    ContextResearchDecision researchContext(
            String merchantId,
            String merchantName,
            TrustEvidence originalEvidence,
            TrustContext context,
            List<ContextRequirement> requirements
    );
}
