package com.sentinq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinq.ai.provider.ClaudeProvider;
import com.sentinq.ai.provider.GeminiProvider;
import com.sentinq.ai.provider.OpenAiProvider;
import com.sentinq.trust.ContextFinding;
import com.sentinq.trust.ContextRequirement;
import com.sentinq.trust.ContextType;
import com.sentinq.trust.DeliveryUrgency;
import com.sentinq.trust.EvidenceChannel;
import com.sentinq.trust.EvidenceExpertise;
import com.sentinq.trust.EvidenceHorizon;
import com.sentinq.trust.EvidenceIndependence;
import com.sentinq.trust.EvidenceInterpretation;
import com.sentinq.trust.EvidenceSource;
import com.sentinq.trust.EvidenceSourceType;
import com.sentinq.trust.InterpretationStatus;
import com.sentinq.trust.MerchantFamiliarity;
import com.sentinq.trust.TrustContext;
import com.sentinq.trust.TrustDimension;
import com.sentinq.trust.TrustEvidence;
import com.sentinq.trust.TrustSignal;
import com.sentinq.trust.interpretation.EvidenceInterpretationProviderRegistry;
import com.sentinq.trust.interpretation.EvidenceInterpretationService;
import com.sentinq.trust.research.ContextResearchDecision;
import com.sentinq.trust.research.ContextResearchProviderRegistry;
import com.sentinq.trust.research.ContextResearchService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class HeirloomRoseTrustMapTest {

    @Test
    void smallArrivalSizeShouldRequireContextBeforeBecomingAQualitySignal() {

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

        EvidenceInterpretation interpretation =
                new EvidenceInterpretation(
                        "interpretation-001",
                        evidence.evidenceId(),

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

                        List.of(),

                        null,

                        TrustSignal.NO_INFERENCE,
                        0.30,

                        List.of(),
                        List.of()
                );

        assertEquals(
                InterpretationStatus.CONTEXT_REQUIRED,
                interpretation.status()
        );

        assertEquals(
                TrustSignal.NO_INFERENCE,
                interpretation.signal()
        );

        assertEquals(
                3,
                interpretation.contextRequirements().size()
        );
    }

    @Test
    void smallArrivalSizeShouldNotBecomePoorQualityAfterOwnRootContextIsEstablished() {

        EvidenceInterpretation interpretation =
                new EvidenceInterpretation(
                        "interpretation-002",
                        "evidence-001",

                        InterpretationStatus.CONTEXT_RESOLVED,

                        "Small arrival size may indicate poor value or poor product quality.",

                        List.of(),

                        List.of(
                                new ContextFinding(
                                        ContextType.PRODUCT_ATTRIBUTE,
                                        "The merchant sells young own-root roses.",
                                        "evidence-context-001",
                                        0.95
                                ),
                                new ContextFinding(
                                        ContextType.CATEGORY_NORM,
                                        "Young own-root roses may be substantially smaller at shipment than established container-grown roses.",
                                        "evidence-context-002",
                                        0.85
                                ),
                                new ContextFinding(
                                        ContextType.TIME_HORIZON,
                                        "Longitudinal reports describe successful establishment and vigorous subsequent growth despite small arrival size.",
                                        "evidence-context-003",
                                        0.80
                                )
                        ),

                        """
                        Small arrival size is a legitimate concern about immediate
                        perceived value, but does not independently demonstrate poor
                        plant quality. In the context of young own-root roses,
                        establishment and longer-term vigor are more informative
                        quality signals.
                        """,

                        TrustSignal.NO_INFERENCE,
                        0.86,

                        List.of(
                                "evidence-context-001",
                                "evidence-context-002",
                                "evidence-context-003"
                        ),

                        List.of()
                );

        assertEquals(
                InterpretationStatus.CONTEXT_RESOLVED,
                interpretation.status()
        );

        assertNotEquals(
                TrustSignal.CONCERNING,
                interpretation.signal()
        );

        assertNotEquals(
                TrustSignal.STRONGLY_CONCERNING,
                interpretation.signal()
        );
    }

    @Test
    void shouldRequireContextBeforeInterpretingAmbiguousQualityEvidence() {

        OpenAiProvider openAiProvider =
                new OpenAiProvider();

        ClaudeProvider claudeProvider =
                new ClaudeProvider(
                        new ObjectMapper()
                );

        GeminiProvider geminiProvider =
                new GeminiProvider(
                        new ObjectMapper()
                );

        EvidenceInterpretationProviderRegistry providerRegistry =
                new EvidenceInterpretationProviderRegistry(
                        List.of(
                                openAiProvider,
                                claudeProvider,
                                geminiProvider
                        )
                );


        EvidenceInterpretationService service =
                new EvidenceInterpretationService(
                        providerRegistry
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
                        "openai",
                        evidence,
                        context
                );

       /* EvidenceInterpretation result =
                service.interpret(
                        "claude",
                        evidence,
                        context
                );

        EvidenceInterpretation result =
                service.interpret(
                        "gemini",
                        evidence,
                        context
                );
*/
        //temp output
        System.out.println(
                "Status: " + result.status()
        );

        System.out.println(
                "Apparent meaning: " + result.apparentMeaning()
        );

        System.out.println(
                "Context requirements: " + result.contextRequirements()
        );

        System.out.println(
                "Contextual meaning: " + result.contextualMeaning()
        );

        System.out.println(
                "Signal: " + result.signal()
        );

        System.out.println(
                "Confidence: " + result.confidence()
        );

        ContextResearchProviderRegistry researchProviderRegistry =
                new ContextResearchProviderRegistry(
                        List.of(
                                openAiProvider
                        )
                );

        ContextResearchService researchService =
                new ContextResearchService(
                        researchProviderRegistry
                );

        ContextResearchDecision research =
                researchService.research(
                        "openai",
                        "heirloom-roses",
                        "Heirloom Roses",
                        evidence,
                        context,
                        result.contextRequirements()
                );

        assertEquals(
                InterpretationStatus.CONTEXT_REQUIRED,
                result.status()
        );

        System.out.println(
                "Research findings:"
        );

        research.findings().forEach(
                finding -> {
                    System.out.println(
                            "Type: " + finding.type()
                    );
                    System.out.println(
                            "Finding: " + finding.finding()
                    );
                    System.out.println(
                            "Source: " + finding.sourceName()
                    );
                    System.out.println(
                            "URL: " + finding.sourceUrl()
                    );
                    System.out.println(
                            "Excerpt: " + finding.sourceExcerpt()
                    );
                    System.out.println(
                            "Confidence: " + finding.confidence()
                    );
                    System.out.println();
                }
        );


        assertEquals(
                TrustSignal.NO_INFERENCE,
                result.signal()
        );

        assertFalse(
                result.contextRequirements().isEmpty()
        );
    }
}