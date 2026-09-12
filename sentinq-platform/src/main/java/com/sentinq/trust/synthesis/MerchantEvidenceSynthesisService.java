package com.sentinq.trust.synthesis;

import com.sentinq.evaluation.LlmInvocationObserver;

import com.sentinq.trust.TrustContext;
import com.sentinq.trust.TrustEvidence;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MerchantEvidenceSynthesisService {

    private final MerchantEvidenceSynthesisProviderRegistry providerRegistry;
    private final LlmInvocationObserver llmInvocationObserver;

    public MerchantEvidenceSynthesisService(
            MerchantEvidenceSynthesisProviderRegistry providerRegistry,
            LlmInvocationObserver llmInvocationObserver
    ) {
        this.providerRegistry = providerRegistry;
        this.llmInvocationObserver = llmInvocationObserver;
    }

    public MerchantEvidenceSynthesis synthesize(
            String provider,
            String merchantId,
            String merchantName,
            List<TrustEvidence> evidence,
            TrustContext context
    ) {
        MerchantEvidenceSynthesisProvider synthesisProvider =
                providerRegistry.getProvider(
                        provider
                );

        return llmInvocationObserver.execute(
                () ->
                        synthesisProvider.synthesizeEvidence(
                                merchantId,
                                merchantName,
                                evidence,
                                context
                        )
        );
    }

    public MerchantEvidenceSynthesis refine(
            String provider,
            String merchantId,
            String merchantName,
            MerchantEvidenceSynthesis initialSynthesis,
            List<TrustEvidence> researchedEvidence,
            TrustContext context
    ) {
        MerchantEvidenceSynthesisProvider synthesisProvider =
                providerRegistry.getProvider(
                        provider
                );

        return synthesisProvider.refineSynthesis(
                merchantId,
                merchantName,
                initialSynthesis,
                researchedEvidence,
                context
        );
    }
}
