package com.sentinq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinq.ai.provider.ClaudeProvider;
import com.sentinq.ai.provider.GeminiProvider;
import com.sentinq.ai.provider.OpenAiProvider;
import com.sentinq.resolution.CandidateOffer;
import com.sentinq.trust.*;
import com.sentinq.trust.interpretation.EvidenceInterpretationProviderRegistry;
import com.sentinq.trust.interpretation.EvidenceInterpretationService;
import com.sentinq.trust.observations.MerchantEvidenceCollectionProviderRegistry;
import com.sentinq.trust.observations.MerchantEvidenceCollectionService;
import com.sentinq.trust.observations.MerchantEvidenceFactory;
import com.sentinq.trust.research.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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

       /* EvidenceInterpretation result =
                service.interpret(
                        "openai",
                        evidence,
                        context
                );

        EvidenceInterpretation result =
                service.interpret(
                        "claude",
                        evidence,
                        context
                );*/

        EvidenceInterpretation result =
                service.interpret(
                        "gemini",
                        evidence,
                        context
                );

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
                            "Source type: " + finding.sourceType()
                    );
                    System.out.println(
                            "Source: " + finding.sourceName()
                    );
                    System.out.println(
                            "Independence: " + finding.sourceIndependence()
                    );
                    System.out.println(
                            "Expertise: " + finding.sourceExpertise()
                    );
                    System.out.println(
                            "Channel: " + finding.channel()
                    );
                    System.out.println(
                            "Horizon: " + finding.evidenceHorizon()
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

        ContextResearchEvidenceFactory evidenceFactory =
                new ContextResearchEvidenceFactory();

        List<TrustEvidence> researchedEvidence =
                new ArrayList<>();

        List<ContextFinding> contextFindings =
                new ArrayList<>();

        for (ContextResearchFinding finding :
                research.findings()) {

            TrustEvidence researched =
                    evidenceFactory.createTrustEvidence(
                            evidence,
                            finding
                    );

            researchedEvidence.add(
                    researched
            );

            ContextFinding contextFinding =
                    evidenceFactory.createContextFinding(
                            finding,
                            researched
                    );

            contextFindings.add(
                    contextFinding
            );
        }

        EvidenceInterpretation reinterpreted =
                service.reinterpret(
                        "openai",
                        evidence,
                        context,
                        researchedEvidence,
                        contextFindings
                );

        System.out.println(
                "REINTERPRETATION"
        );

        System.out.println(
                "Status: "
                        + reinterpreted.status()
        );

        System.out.println(
                "Apparent meaning: "
                        + reinterpreted.apparentMeaning()
        );

        System.out.println(
                "Context requirements: "
                        + reinterpreted.contextRequirements()
        );

        System.out.println(
                "Contextual meaning: "
                        + reinterpreted.contextualMeaning()
        );

        System.out.println(
                "Signal: "
                        + reinterpreted.signal()
        );

        System.out.println(
                "Confidence: "
                        + reinterpreted.confidence()
        );

        System.out.println(
                "Supporting evidence: "
                        + reinterpreted.supportingEvidenceIds()
        );

        System.out.println(
                "Contradicting evidence: "
                        + reinterpreted.contradictingEvidenceIds()
        );

        assertEquals(
                TrustSignal.NO_INFERENCE,
                result.signal()
        );

        assertFalse(
                result.contextRequirements().isEmpty()
        );
    }


    @Test
    void researchedContextShouldBecomeTraceableTrustEvidence() {

        TrustEvidence originalEvidence = new TrustEvidence(
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

        ContextResearchFinding researchFinding =
                new ContextResearchFinding(
                        ContextType.CATEGORY_NORM,
                        "Own-root roses may initially be smaller and less robust than grafted roses.",
                        EvidenceSourceType.EXPERT_OR_SPECIALIST,
                        "University horticultural extension",
                        EvidenceIndependence.INDEPENDENT,
                        EvidenceExpertise.DOMAIN_EXPERT,
                        EvidenceChannel.UNKNOWN,
                        EvidenceHorizon.MEDIUM_TERM,
                        "https://example.com/own-root-roses",
                        "Own-root roses may initially be smaller.",
                        0.86
                );

        ContextResearchEvidenceFactory factory =
                new ContextResearchEvidenceFactory();

        TrustEvidence researchedEvidence =
                factory.createTrustEvidence(
                        originalEvidence,
                        researchFinding
                );

        ContextFinding contextFinding =
                factory.createContextFinding(
                        researchFinding,
                        researchedEvidence
                );

        assertEquals(
                TrustDimension.PRODUCT_QUALITY,
                researchedEvidence.proposedDimension()
        );

        assertEquals(
                EvidenceSourceType.EXPERT_OR_SPECIALIST,
                researchedEvidence.source().type()
        );

        assertEquals(
                EvidenceIndependence.INDEPENDENT,
                researchedEvidence.source().independence()
        );

        assertEquals(
                EvidenceExpertise.DOMAIN_EXPERT,
                researchedEvidence.source().expertise()
        );

        assertEquals(
                researchedEvidence.evidenceId(),
                contextFinding.sourceEvidenceId()
        );

        assertEquals(
                ContextType.CATEGORY_NORM,
                contextFinding.type()
        );
    }

/*S
    @Test
    void trustMapShouldOrchestrateAmbiguousEvidenceThroughResearchAndReinterpretation() {

        OpenAiProvider openAiProvider =
                new OpenAiProvider();

        EvidenceInterpretationProviderRegistry interpretationRegistry =
                new EvidenceInterpretationProviderRegistry(
                        List.of(openAiProvider)
                );

        EvidenceInterpretationService interpretationService =
                new EvidenceInterpretationService(
                        interpretationRegistry
                );

        ContextResearchProviderRegistry researchRegistry =
                new ContextResearchProviderRegistry(
                        List.of(openAiProvider)
                );

        ContextResearchService researchService =
                new ContextResearchService(
                        researchRegistry
                );

        ContextResearchEvidenceFactory evidenceFactory =
                new ContextResearchEvidenceFactory();

        TrustMapOrchestrationService orchestrationService =
                new TrustMapOrchestrationService(
                        interpretationService,
                        researchService,
                        evidenceFactory
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
                                "cultivar", "Desdemona",
                                "propagationMethod", "OWN_ROOT"
                        ),
                        new BigDecimal("65.00"),
                        DeliveryUrgency.NORMAL,
                        MerchantFamiliarity.UNKNOWN,
                        List.of(
                                TrustDimension.PRODUCT_QUALITY,
                                TrustDimension.PRODUCT_DURABILITY
                        )
                );

        TrustEvidenceAssessment assessment =
                orchestrationService.assessEvidence(
                        "openai",
                        "heirloom-roses",
                        "Heirloom Roses",
                        evidence,
                        context
                );

        System.out.println(
                "FINAL TRUST MAP ASSESSMENT"
        );

        System.out.println(
                "Status: "
                        + assessment.interpretation().status()
        );

        System.out.println(
                "Signal: "
                        + assessment.interpretation().signal()
        );

        System.out.println(
                "Contextual meaning: "
                        + assessment.interpretation().contextualMeaning()
        );

        System.out.println(
                "Confidence: "
                        + assessment.interpretation().confidence()
        );

        System.out.println(
                "Research rounds: "
                        + assessment.researchRounds()
        );

        System.out.println(
                "Researched evidence count: "
                        + assessment.researchedEvidence().size()
        );

        System.out.println(
                "Context findings count: "
                        + assessment.contextFindings().size()
        );

        assertNotNull(
                assessment.interpretation()
        );

        assertFalse(
                assessment.researchedEvidence().isEmpty()
        );

        assertFalse(
                assessment.contextFindings().isEmpty()
        );

        assertTrue(
                assessment.researchRounds() > 0
        );

        assertTrue(
                assessment.researchRounds() <= 2
        );
    }
*/
    @Test
    void shouldCollectMerchantEvidenceForCandidateOffer() {

        OpenAiProvider openAiProvider =
                new OpenAiProvider();

        MerchantEvidenceCollectionProviderRegistry providerRegistry =
                new MerchantEvidenceCollectionProviderRegistry(
                        List.of(openAiProvider)
                );

        MerchantEvidenceFactory evidenceFactory =
                new MerchantEvidenceFactory();

        MerchantEvidenceCollectionService service =
                new MerchantEvidenceCollectionService(
                        providerRegistry,
                        evidenceFactory
                );

        CandidateOffer offer =
                new CandidateOffer();

        offer.setOfferId("offer-001");
        offer.setMerchantId("heirloom-roses");
        offer.setMerchantName("Heirloom Roses");
        offer.setProductName("Desdemona Rose");
        offer.setProductPriceCents(6500);

        TrustContext context =
                new TrustContext(
                        "Find me a Desdemona rose for long-term garden planting",
                        "GARDEN_PLANTS",
                        "ROSE_BUSH",
                        Map.of(
                                "cultivar", "Desdemona",
                                "propagationMethod", "OWN_ROOT"
                        ),
                        new BigDecimal("65.00"),
                        DeliveryUrgency.NORMAL,
                        MerchantFamiliarity.UNKNOWN,
                        List.of(
                                TrustDimension.PRODUCT_QUALITY,
                                TrustDimension.PRODUCT_DURABILITY
                        )
                );

        List<TrustEvidence> evidence =
                service.collectEvidence(
                        "openai",
                        offer.getMerchantId(),
                        offer.getMerchantName(),
                        offer,
                        context
                );

        System.out.println("OBSERVED MERCHANT EVIDENCE");
        System.out.println("Evidence count: " + evidence.size());
        System.out.println();

        evidence.forEach(item -> {
            System.out.println(
                    "Evidence ID: " + item.evidenceId()
            );
            System.out.println(
                    "Dimension: " + item.proposedDimension()
            );
            System.out.println(
                    "Source type: " + item.source().type()
            );
            System.out.println(
                    "Source: " + item.source().name()
            );
            System.out.println(
                    "Independence: " + item.source().independence()
            );
            System.out.println(
                    "Expertise: " + item.source().expertise()
            );
            System.out.println(
                    "Channel: " + item.channel()
            );
            System.out.println(
                    "Claim: " + item.rawClaim()
            );

            System.out.println(
                    "Raw content: " + item.sourceExcerpt()
            );
            System.out.println(
                    "Horizon: " + item.evidenceHorizon()
            );
            System.out.println(
                    "URL: " + item.sourceUrl()
            );
            System.out.println();
        });

        assertFalse(
                evidence.isEmpty()
        );

        evidence.forEach(item ->
                assertNotNull(
                        item.evidenceId()
                )
        );
    }

    @Test
    void shouldAssessMerchantFromObservedEvidence() {

        OpenAiProvider openAiProvider =
                new OpenAiProvider();

        EvidenceInterpretationProviderRegistry interpretationRegistry =
                new EvidenceInterpretationProviderRegistry(
                        List.of(openAiProvider)
                );

        ContextResearchProviderRegistry researchRegistry =
                new ContextResearchProviderRegistry(
                        List.of(openAiProvider)
                );

        MerchantEvidenceCollectionProviderRegistry observationRegistry =
                new MerchantEvidenceCollectionProviderRegistry(
                        List.of(openAiProvider)
                );

        EvidenceInterpretationService interpretationService =
                new EvidenceInterpretationService(
                        interpretationRegistry
                );

        ContextResearchService researchService =
                new ContextResearchService(
                        researchRegistry
                );

        ContextResearchEvidenceFactory researchEvidenceFactory =
                new ContextResearchEvidenceFactory();

        MerchantEvidenceFactory merchantEvidenceFactory =
                new MerchantEvidenceFactory();

        MerchantEvidenceCollectionService evidenceCollectionService =
                new MerchantEvidenceCollectionService(
                        observationRegistry,
                        merchantEvidenceFactory
                );

        TrustMapOrchestrationService trustMapService =
                new TrustMapOrchestrationService(
                        interpretationService,
                        researchService,
                        researchEvidenceFactory,
                        evidenceCollectionService
                );

        CandidateOffer offer =
                new CandidateOffer();

        offer.setOfferId("offer-001");
        offer.setMerchantId("heirloom-roses");
        offer.setMerchantName("Heirloom Roses");
        offer.setProductName("Desdemona Rose");
        offer.setProductPriceCents(6500);

        TrustContext context =
                new TrustContext(
                        "Find me a Desdemona rose for long-term garden planting",
                        "GARDEN_PLANTS",
                        "ROSE_BUSH",
                        Map.of(
                                "cultivar", "Desdemona",
                                "propagationMethod", "OWN_ROOT"
                        ),
                        new BigDecimal("65.00"),
                        DeliveryUrgency.NORMAL,
                        MerchantFamiliarity.UNKNOWN,
                        List.of(
                                TrustDimension.PRODUCT_QUALITY,
                                TrustDimension.PRODUCT_DURABILITY
                        )
                );

        MerchantTrustAssessment result =
                trustMapService.assessMerchant(
                        "openai",
                        offer,
                        context
                );

        System.out.println();
        System.out.println("MERCHANT TRUST ASSESSMENT");
        System.out.println("Merchant: " + result.merchantName());
        System.out.println(
                "Evidence assessments: "
                        + result.evidenceAssessments().size()
        );
        System.out.println();

        result.evidenceAssessments().forEach(
                assessment -> {
                    System.out.println(
                            "Original evidence: "
                                    + assessment.originalEvidence().rawClaim()
                    );

                    System.out.println(
                            "Dimension: "
                                    + assessment.originalEvidence()
                                    .proposedDimension()
                    );

                    System.out.println(
                            "Final interpretation status: "
                                    + assessment.interpretation().status()
                    );

                    System.out.println(
                            "Final signal: "
                                    + assessment.interpretation().signal()
                    );

                    System.out.println(
                            "Apparent meaning: "
                                    + assessment.interpretation().apparentMeaning()
                    );

                    System.out.println(
                            "Contextual meaning: "
                                    + assessment.interpretation().contextualMeaning()
                    );

                    System.out.println(
                            "Confidence: "
                                    + assessment.interpretation().confidence()
                    );

                    System.out.println(
                            "Research rounds: "
                                    + assessment.researchRounds()
                    );

                    System.out.println(
                            "Researched evidence count: "
                                    + assessment.researchedEvidence().size()
                    );

                    System.out.println(
                            "Context findings count: "
                                    + assessment.contextFindings().size()
                    );

                    System.out.println();
                    System.out.println("--------------------------");
                    System.out.println();
                }
        );

        assertEquals(
                "heirloom-roses",
                result.merchantId()
        );

        assertFalse(
                result.evidenceAssessments().isEmpty()
        );
    }
}