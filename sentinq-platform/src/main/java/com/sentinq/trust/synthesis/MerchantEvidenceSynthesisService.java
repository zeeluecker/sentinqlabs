package com.sentinq.trust.synthesis;

import com.sentinq.trust.TrustContext;
import com.sentinq.trust.TrustEvidence;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MerchantEvidenceSynthesisService {

    private final MerchantEvidenceSynthesisProviderRegistry providerRegistry;

    public MerchantEvidenceSynthesisService(
            MerchantEvidenceSynthesisProviderRegistry providerRegistry
    ) {
        this.providerRegistry = providerRegistry;
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

        return synthesisProvider.synthesizeEvidence(
                merchantId,
                merchantName,
                evidence,
                context
        );
    }
}
