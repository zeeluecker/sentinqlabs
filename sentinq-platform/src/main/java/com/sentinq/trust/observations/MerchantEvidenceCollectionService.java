package com.sentinq.trust.observations;

import com.sentinq.resolution.CandidateOffer;
import com.sentinq.trust.TrustContext;
import com.sentinq.trust.TrustEvidence;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MerchantEvidenceCollectionService {

    private final MerchantEvidenceCollectionProviderRegistry providerRegistry;
    private final MerchantEvidenceFactory evidenceFactory;

    public MerchantEvidenceCollectionService(
            MerchantEvidenceCollectionProviderRegistry providerRegistry,
            MerchantEvidenceFactory evidenceFactory
    ) {
        this.providerRegistry =
                providerRegistry;

        this.evidenceFactory =
                evidenceFactory;
    }

    public List<TrustEvidence> collectEvidence(
            String providerId,
            String merchantId,
            String merchantName,
            CandidateOffer offer,
            TrustContext context
    ) {
        validateInputs(
                providerId,
                merchantId,
                merchantName,
                offer,
                context
        );

        MerchantEvidenceCollectionProvider provider =
                providerRegistry.getProvider(
                        providerId
                );

        MerchantEvidenceCollectionDecision decision =
                provider.collectMerchantEvidence(
                        merchantId,
                        merchantName,
                        offer,
                        context
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
            String providerId,
            String merchantId,
            String merchantName,
            CandidateOffer offer,
            TrustContext context
    ) {
        if (providerId == null ||
                providerId.isBlank()) {
            throw new IllegalArgumentException(
                    "Provider ID is required."
            );
        }

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