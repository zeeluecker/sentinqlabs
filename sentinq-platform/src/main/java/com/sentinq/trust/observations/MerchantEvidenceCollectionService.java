package com.sentinq.trust.observations;

import com.sentinq.evaluation.*;
import com.sentinq.resolution.CandidateOffer;
import com.sentinq.trust.TrustContext;
import com.sentinq.trust.TrustEvidence;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class MerchantEvidenceCollectionService {

    private final MerchantEvidenceCollectionProviderRegistry providerRegistry;
    private final MerchantEvidenceFactory evidenceFactory;
    private final LlmInvocationObserver llmInvocationObserver;

    public MerchantEvidenceCollectionService(
            MerchantEvidenceCollectionProviderRegistry providerRegistry,
            MerchantEvidenceFactory evidenceFactory,
            LlmInvocationObserver llmInvocationObserver
    ) {
        this.providerRegistry =
                providerRegistry;

        this.evidenceFactory =
                evidenceFactory;

        this.llmInvocationObserver =
                llmInvocationObserver;
    }

    public List<TrustEvidence> collectEvidence(
            UUID executionId,
            String merchantId,
            String merchantName,
            CandidateOffer offer,
            TrustContext context,
            ModelAssignment assignment
    ) {
        validateInputs(
                merchantId,
                merchantName,
                offer,
                context
        );

        MerchantEvidenceCollectionProvider provider =
                providerRegistry.getProvider(
                        assignment.provider()
                );

        CapabilityIdentity capabilityIdentity =
                new CapabilityIdentity(
                        executionId,
                        UUID.randomUUID(),
                        Capability.MERCHANT_EVIDENCE_COLLECTION
                );

        MerchantEvidenceCollectionDecision decision =
                llmInvocationObserver.execute(
                        capabilityIdentity,
                        () ->
                                provider.collectMerchantEvidence(
                                        merchantId,
                                        merchantName,
                                        offer,
                                        context,
                                        assignment.model()
                                )
                );

        if (decision == null ||
                decision.observations() == null) {
            return List.of();
        }

        return decision.observations()
                .stream()
                .map(evidenceFactory::create)
                .toList();
    }

    private void validateInputs(
            String merchantId,
            String merchantName,
            CandidateOffer offer,
            TrustContext context
    ) {

        if (merchantId == null ||
                merchantId.isBlank()) {
            throw new IllegalArgumentException(
                    "Merchant ID is required."
            );
        }

        if (merchantName == null ||
                merchantName.isBlank()) {
            throw new IllegalArgumentException(
                    "Merchant name is required."
            );
        }

        if (offer == null) {
            throw new IllegalArgumentException(
                    "Candidate offer is required."
            );
        }

        if (context == null) {
            throw new IllegalArgumentException(
                    "Trust context is required."
            );
        }
    }
}