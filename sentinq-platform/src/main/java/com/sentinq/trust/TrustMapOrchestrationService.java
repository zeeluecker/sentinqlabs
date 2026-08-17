package com.sentinq.trust;

import com.sentinq.resolution.CandidateOffer;
import com.sentinq.trust.interpretation.EvidenceInterpretationService;
import com.sentinq.trust.observations.MerchantEvidenceCollectionService;
import com.sentinq.trust.research.ContextResearchDecision;
import com.sentinq.trust.research.ContextResearchEvidenceFactory;
import com.sentinq.trust.research.ContextResearchFinding;
import com.sentinq.trust.research.ContextResearchService;
import com.sentinq.trust.synthesis.MerchantEvidenceSynthesis;
import com.sentinq.trust.synthesis.MerchantEvidenceSynthesisService;
import com.sentinq.trust.synthesis.MerchantTargetedResearchDecision;
import com.sentinq.trust.synthesis.MerchantTargetedResearchEvidenceFactory;
import com.sentinq.trust.synthesis.MerchantTargetedResearchService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TrustMapOrchestrationService {

    private static final int MAX_RESEARCH_ROUNDS = 2;

    /*
     * OLD evidence-level pipeline.
     *
     * Kept temporarily because existing tests and
     * debugging flows still use assessEvidence().
     */
    private final EvidenceInterpretationService interpretationService;
    private final ContextResearchService researchService;
    private final ContextResearchEvidenceFactory evidenceFactory;

    /*
     * Merchant-level observation.
     */
    private final MerchantEvidenceCollectionService evidenceCollectionService;

    /*
     * NEW optimized Trust Maps pipeline.
     */
    private final MerchantEvidenceSynthesisService synthesisService;
    private final MerchantTargetedResearchService targetedResearchService;
    private final MerchantTargetedResearchEvidenceFactory targetedResearchEvidenceFactory;

    public TrustMapOrchestrationService(
            EvidenceInterpretationService interpretationService,
            ContextResearchService researchService,
            ContextResearchEvidenceFactory evidenceFactory,
            MerchantEvidenceCollectionService evidenceCollectionService,
            MerchantEvidenceSynthesisService synthesisService,
            MerchantTargetedResearchService targetedResearchService,
            MerchantTargetedResearchEvidenceFactory targetedResearchEvidenceFactory) {
        this.interpretationService =
                interpretationService;

        this.researchService =
                researchService;

        this.evidenceFactory =
                evidenceFactory;

        this.evidenceCollectionService =
                evidenceCollectionService;

        this.synthesisService =
                synthesisService;

        this.targetedResearchService =
                targetedResearchService;

        this.targetedResearchEvidenceFactory =
                targetedResearchEvidenceFactory;
    }

    /*
     * -------------------------------------------------------------------------
     * OLD EVIDENCE-LEVEL PIPELINE
     * -------------------------------------------------------------------------
     *
     * Keep this for now.
     *
     * This method represents the original:
     *
     * INTERPRET
     * → RESEARCH
     * → REINTERPRET
     *
     * loop for one evidence item.
     *
     * assessMerchant() no longer uses it.
     */
    public TrustEvidenceAssessment assessEvidence(
            String provider,
            String merchantId,
            String merchantName,
            TrustEvidence evidence,
            TrustContext context
    ) {
        validateInputs(
                provider,
                merchantId,
                merchantName,
                evidence,
                context
        );

        EvidenceInterpretation interpretation =
                interpretationService.interpret(
                        provider,
                        evidence,
                        context
                );

        List<TrustEvidence> researchedEvidence =
                new ArrayList<>();

        List<ContextFinding> contextFindings =
                new ArrayList<>();

        int researchRounds = 0;

        while (requiresMoreContext(
                interpretation,
                researchRounds
        )) {

            ContextResearchDecision research =
                    researchService.research(
                            provider,
                            merchantId,
                            merchantName,
                            evidence,
                            context,
                            interpretation.contextRequirements()
                    );

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

            researchRounds++;

            interpretation =
                    interpretationService.reinterpret(
                            provider,
                            evidence,
                            context,
                            researchedEvidence,
                            contextFindings
                    );
        }

        return new TrustEvidenceAssessment(
                evidence,
                interpretation,
                List.copyOf(
                        researchedEvidence
                ),
                List.copyOf(
                        contextFindings
                ),
                researchRounds
        );
    }

    private boolean requiresMoreContext(
            EvidenceInterpretation interpretation,
            int researchRounds
    ) {
        return interpretation.status()
                == InterpretationStatus.CONTEXT_REQUIRED
                &&
                !interpretation.contextRequirements()
                        .isEmpty()
                &&
                researchRounds
                        < MAX_RESEARCH_ROUNDS;
    }

    private void validateInputs(
            String provider,
            String merchantId,
            String merchantName,
            TrustEvidence evidence,
            TrustContext context
    ) {
        if (provider == null ||
                provider.isBlank()) {
            throw new IllegalArgumentException(
                    "Provider is required."
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

        if (evidence == null) {
            throw new IllegalArgumentException(
                    "Trust evidence is required."
            );
        }

        if (context == null) {
            throw new IllegalArgumentException(
                    "Trust context is required."
            );
        }
    }

    /*
     * -------------------------------------------------------------------------
     * NEW MERCHANT-LEVEL OPTIMIZED PIPELINE
     * -------------------------------------------------------------------------
     */
    public MerchantTrustAssessment assessMerchant(
            String provider,
            CandidateOffer offer,
            TrustContext context
    ) {
        validateMerchantInputs(
                provider,
                offer,
                context
        );

        long totalStart =
                System.currentTimeMillis();

        /*
         * STEP 1 — OBSERVE
         *
         * Collect representative merchant evidence.
         */
        long observationStart =
                System.currentTimeMillis();

        List<TrustEvidence> observedEvidence =
                evidenceCollectionService.collectEvidence(
                        provider,
                        offer.getMerchantId(),
                        offer.getMerchantName(),
                        offer,
                        context
                );

        long observationDuration =
                System.currentTimeMillis()
                        - observationStart;

        /*
         * STEP 2 — INITIAL SYNTHESIS
         *
         * Understand the evidence landscape before
         * deciding what deserves deeper research.
         */
        long initialSynthesisStart =
                System.currentTimeMillis();

        MerchantEvidenceSynthesis initialSynthesis =
                synthesisService.synthesize(
                        provider,
                        offer.getMerchantId(),
                        offer.getMerchantName(),
                        observedEvidence,
                        context
                );

        long initialSynthesisDuration =
                System.currentTimeMillis()
                        - initialSynthesisStart;

        /*
         * STEP 3 — ONE BOUNDED TARGETED RESEARCH ROUND
         *
         * Research all material questions together.
         *
         * Zero questions = zero research.
         */
        List<TrustEvidence> researchedEvidence =
                new ArrayList<>();

        long targetedResearchDuration =
                0L;

        if (initialSynthesis.materialQuestions() != null
                &&
                !initialSynthesis.materialQuestions().isEmpty()) {

            long targetedResearchStart =
                    System.currentTimeMillis();

            MerchantTargetedResearchDecision researchDecision =
                    targetedResearchService.research(
                            provider,
                            offer.getMerchantId(),
                            offer.getMerchantName(),
                            observedEvidence,
                            initialSynthesis,
                            context
                    );

            if (researchDecision != null
                    &&
                    researchDecision.findings() != null) {

                researchDecision.findings()
                        .stream()
                        .map(
                                targetedResearchEvidenceFactory::create
                        )
                        .forEach(
                                researchedEvidence::add
                        );
            }

            targetedResearchDuration =
                    System.currentTimeMillis()
                            - targetedResearchStart;
        }

        /*
         * STEP 4 — COMBINE ORIGINAL + RESEARCHED EVIDENCE
         */
        List<TrustEvidence> allEvidence =
                new ArrayList<>(
                        observedEvidence
                );

        allEvidence.addAll(
                researchedEvidence
        );

        /*
         * STEP 5 — FINAL SYNTHESIS
         *
         * This is the final reasoning pass.
         *
         * Remaining uncertainty is preserved.
         * It does NOT trigger another research loop.
         */
        long finalSynthesisStart =
                System.currentTimeMillis();

        MerchantEvidenceSynthesis finalSynthesis =
                synthesisService.synthesize(
                        provider,
                        offer.getMerchantId(),
                        offer.getMerchantName(),
                        allEvidence,
                        context
                );

        long finalSynthesisDuration =
                System.currentTimeMillis()
                        - finalSynthesisStart;

        long totalDuration =
                System.currentTimeMillis()
                        - totalStart;

        /*
         * Temporary instrumentation.
         *
         * Once we've stabilized performance,
         * move this into proper tracing/audit metrics.
         */
        System.out.println();
        System.out.println(
                "========================================"
        );

        System.out.println(
                "OPTIMIZED TRUST MAP TIMING"
        );

        System.out.println(
                "Merchant: "
                        + offer.getMerchantName()
        );

        System.out.println(
                "Observed evidence: "
                        + observedEvidence.size()
        );

        System.out.println(
                "Initial material questions: "
                        + initialSynthesis
                        .materialQuestions()
                        .size()
        );

        System.out.println(
                "Researched evidence: "
                        + researchedEvidence.size()
        );

        System.out.println(
                "Observation: "
                        + observationDuration
                        + " ms"
        );

        System.out.println(
                "Initial synthesis: "
                        + initialSynthesisDuration
                        + " ms"
        );

        System.out.println(
                "Targeted research: "
                        + targetedResearchDuration
                        + " ms"
        );

        System.out.println(
                "Final synthesis: "
                        + finalSynthesisDuration
                        + " ms"
        );

        System.out.println(
                "TOTAL MERCHANT TRUST MAP: "
                        + totalDuration
                        + " ms"
        );

        System.out.println(
                "========================================"
        );

        System.out.println();

        return new MerchantTrustAssessment(
                offer.getMerchantId(),
                offer.getMerchantName(),
                List.copyOf(
                        observedEvidence
                ),
                List.copyOf(
                        researchedEvidence
                ),
                finalSynthesis
        );
    }

    private void validateMerchantInputs(
            String provider,
            CandidateOffer offer,
            TrustContext context
    ) {
        if (provider == null ||
                provider.isBlank()) {

            throw new IllegalArgumentException(
                    "Provider is required."
            );
        }

        if (offer == null) {
            throw new IllegalArgumentException(
                    "Candidate offer is required."
            );
        }

        if (offer.getMerchantId() == null ||
                offer.getMerchantId().isBlank()) {

            throw new IllegalArgumentException(
                    "Merchant ID is required."
            );
        }

        if (offer.getMerchantName() == null ||
                offer.getMerchantName().isBlank()) {

            throw new IllegalArgumentException(
                    "Merchant name is required."
            );
        }

        if (context == null) {
            throw new IllegalArgumentException(
                    "Trust context is required."
            );
        }
    }
}