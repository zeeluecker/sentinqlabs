package com.sentinq;

import com.sentinq.trust.*;
import com.sentinq.trust.interpretation.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EvidenceInterpretationServiceTest {

    /**
     * Verifies that EvidenceInterpretationService delegates evidence
     * reasoning to the provider selected by Sentinq and converts the
     * structured provider decision into an immutable domain interpretation.

    @Test
    void shouldCreateEvidenceInterpretationFromProviderDecision() {

        EvidenceInterpretationProvider fakeProvider =
                new EvidenceInterpretationProvider() {

                    @Override
                    public String getProviderId() {
                        return "test-provider";
                    }

                    @Override
                    public EvidenceInterpretationDecision interpretEvidence(
                            TrustEvidence evidence,
                            TrustContext context
                    ) {
                        return new EvidenceInterpretationDecision(
                                InterpretationStatus.CONTEXT_REQUIRED,
                                "Small arrival size may indicate poor value or poor product quality.",
                                List.of(
                                        new ContextRequirement(
                                                ContextType.PRODUCT_ATTRIBUTE,
                                                "Is the rose own-root or grafted?",
                                                true
                                        ),
                                        new ContextRequirement(
                                                ContextType.CATEGORY_NORM,
                                                "What arrival size is normal for a young own-root rose?",
                                                true
                                        ),
                                        new ContextRequirement(
                                                ContextType.TIME_HORIZON,
                                                "Does arrival size predict establishment and long-term vigor?",
                                                true
                                        )
                                ),
                                null,
                                TrustSignal.NO_INFERENCE,
                                0.30,
                                List.of(),
                                List.of()
                        );
                    }
                };

        EvidenceInterpretationProviderRegistry registry =
                new EvidenceInterpretationProviderRegistry(
                        List.of(
                                fakeProvider
                        )
                );

        EvidenceInterpretationService service =
                new EvidenceInterpretationService(
                        registry
                );

        TrustEvidence evidence =
                new TrustEvidence(
                        "evidence-001",
                        TrustDimension.PRODUCT_QUALITY,
                        new EvidenceSource(
                                EvidenceSourceType.CUSTOMER_REVIEW,
                                "Independent customer review",
                                EvidenceIndependence.INDEPENDENT,
                                EvidenceExpertise.GENERAL_CONSUMER
                        ),
                        EvidenceChannel.DIRECT_MERCHANT,
                        "The rose arrived tiny for the price.",
                        "The rose arrived tiny for the price.",
                        Instant.now(),
                        EvidenceHorizon.IMMEDIATE,
                        "https://example.com/review"
                );

        TrustContext context =
                new TrustContext(
                        "Find me a Desdemona rose for long-term garden planting",
                        "GARDEN_PLANTS",
                        "ROSE_BUSH",
                        Map.of(
                                "cultivar",
                                "Desdemona",
                                "propagationMethod",
                                "OWN_ROOT"
                        ),
                        new BigDecimal(
                                "65.00"
                        ),
                        DeliveryUrgency.NORMAL,
                        MerchantFamiliarity.UNKNOWN,
                        List.of(
                                TrustDimension.PRODUCT_QUALITY,
                                TrustDimension.PRODUCT_DURABILITY
                        )
                );

        EvidenceInterpretation result =
                service.interpret(
                        "test-provider",
                        evidence,
                        context
                );

        assertEquals(
                evidence.evidenceId(),
                result.evidenceId()
        );

        assertEquals(
                InterpretationStatus.CONTEXT_REQUIRED,
                result.status()
        );

        assertEquals(
                TrustSignal.NO_INFERENCE,
                result.signal()
        );

        assertEquals(
                3,
                result.contextRequirements().size()
        );

        assertTrue(
                result.contextFindings().isEmpty()
        );

        assertNotNull(
                result.interpretationId()
        );
    }*/
}
