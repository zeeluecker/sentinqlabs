package com.sentinq.trust.synthesis;

import com.sentinq.evaluation.*;
import com.sentinq.trust.TrustContext;
import com.sentinq.trust.TrustEvidence;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

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
            UUID executionId,
            String merchantId,
            String merchantName,
            List<TrustEvidence> evidence,
            TrustContext context,
            ModelAssignment assignment
    ) {
        MerchantEvidenceSynthesisProvider synthesisProvider =
                providerRegistry.getProvider(
                        assignment.provider()
                );

        CapabilityIdentity capabilityIdentity =
                new CapabilityIdentity(
                        executionId,
                        UUID.randomUUID(),
                        Capability.MERCHANT_EVIDENCE_SYNTHESIS
                );

        return llmInvocationObserver.execute(
                capabilityIdentity,
                () ->
                        synthesisProvider.synthesizeEvidence(
                                merchantId,
                                merchantName,
                                evidence,
                                context,
                                assignment.model()
                        )
        );
    }

    public MerchantEvidenceSynthesis refine(
            UUID executionId,
            String merchantId,
            String merchantName,
            MerchantEvidenceSynthesis initialSynthesis,
            List<TrustEvidence> researchedEvidence,
            TrustContext context,
            ModelAssignment assignment
    ) {
        MerchantEvidenceSynthesisProvider synthesisProvider =
                providerRegistry.getProvider(
                        assignment.provider()
                );

        CapabilityIdentity capabilityIdentity =
                new CapabilityIdentity(
                        executionId,
                        UUID.randomUUID(),
                        Capability.MERCHANT_EVIDENCE_SYNTHESIS
                );

        return llmInvocationObserver.execute(
                capabilityIdentity,
                () ->
                        synthesisProvider.refineSynthesis(
                                merchantId,
                                merchantName,
                                initialSynthesis,
                                researchedEvidence,
                                context,
                                assignment.model()
                        )
        );
    }
}