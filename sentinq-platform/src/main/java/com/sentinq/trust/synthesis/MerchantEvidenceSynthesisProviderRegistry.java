package com.sentinq.trust.synthesis;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class MerchantEvidenceSynthesisProviderRegistry {

    private final Map<String, MerchantEvidenceSynthesisProvider> providers;

    public MerchantEvidenceSynthesisProviderRegistry(
            List<MerchantEvidenceSynthesisProvider> providers
    ) {
        this.providers =
                providers.stream()
                        .collect(
                                Collectors.toMap(
                                        MerchantEvidenceSynthesisProvider::providerId,
                                        Function.identity()
                                )
                        );
    }

    public MerchantEvidenceSynthesisProvider getProvider(
            String providerId
    ) {
        MerchantEvidenceSynthesisProvider provider =
                providers.get(providerId);

        if (provider == null) {
            throw new IllegalArgumentException(
                    "Unsupported evidence synthesis provider: "
                            + providerId
            );
        }

        return provider;
    }
}
