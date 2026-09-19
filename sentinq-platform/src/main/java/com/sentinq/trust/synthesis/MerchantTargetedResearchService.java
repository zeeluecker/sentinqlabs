package com.sentinq.trust.synthesis;

import com.sentinq.evaluation.*;
import com.sentinq.trust.TrustContext;
import com.sentinq.trust.TrustEvidence;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

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
            UUID executionId,
            String merchantId,
            String merchantName,
            List<TrustEvidence> existingEvidence,
            MerchantEvidenceSynthesis synthesis,
            TrustContext context,
            ModelAssignment assignment
    ) {
        MerchantTargetedResearchProvider researchProvider =
                providerRegistry.getProvider(
                        assignment.provider()
                );

        CapabilityIdentity capabilityIdentity =
                new CapabilityIdentity(
                        executionId,
                        UUID.randomUUID(),
                        Capability.MERCHANT_TARGETED_RESEARCH
                );

        return llmInvocationObserver.execute(
                capabilityIdentity,
                () ->
                        researchProvider.research(
                                merchantId,
                                merchantName,
                                existingEvidence,
                                synthesis,
                                context,
                                assignment.model()
                        )
        );
    }
}