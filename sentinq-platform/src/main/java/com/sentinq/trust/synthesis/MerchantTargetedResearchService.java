package com.sentinq.trust.synthesis;

import com.sentinq.evaluation.LlmInvocationObserver;
import com.sentinq.trust.TrustContext;
import com.sentinq.trust.TrustEvidence;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MerchantTargetedResearchService {

    private final MerchantTargetedResearchProviderRegistry providerRegistry;
    private final LlmInvocationObserver llmInvocationObserver;

    public MerchantTargetedResearchService(
            MerchantTargetedResearchProviderRegistry providerRegistry,
            LlmInvocationObserver llmInvocationObserver
    ) {
        this.providerRegistry = providerRegistry;
        this.llmInvocationObserver = llmInvocationObserver;
    }

    public MerchantTargetedResearchDecision research(
            String provider,
            String merchantId,
            String merchantName,
            List<TrustEvidence> existingEvidence,
            MerchantEvidenceSynthesis synthesis,
            TrustContext context
    ) {
        return llmInvocationObserver.execute(
                () ->
                        providerRegistry
                                .getProvider(provider)
                                .research(
                                        merchantId,
                                        merchantName,
                                        existingEvidence,
                                        synthesis,
                                        context
                                )
        );
    }
}
