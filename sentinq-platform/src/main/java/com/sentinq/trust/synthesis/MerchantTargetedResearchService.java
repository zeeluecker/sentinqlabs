package com.sentinq.trust.synthesis;

import com.sentinq.trust.TrustContext;
import com.sentinq.trust.TrustEvidence;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MerchantTargetedResearchService {

    private final MerchantTargetedResearchProviderRegistry providerRegistry;

    public MerchantTargetedResearchService(
            MerchantTargetedResearchProviderRegistry providerRegistry
    ) {
        this.providerRegistry = providerRegistry;
    }

    public MerchantTargetedResearchDecision research(
            String provider,
            String merchantId,
            String merchantName,
            List<TrustEvidence> existingEvidence,
            MerchantEvidenceSynthesis synthesis,
            TrustContext context
    ) {
        return providerRegistry
                .getProvider(provider)
                .research(
                        merchantId,
                        merchantName,
                        existingEvidence,
                        synthesis,
                        context
                );
    }
}
