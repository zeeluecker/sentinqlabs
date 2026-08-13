package com.sentinq.trust;

import com.sentinq.resolution.CandidateOffer;
import com.sentinq.trust.interpretation.EvidenceInterpretationService;
import com.sentinq.trust.observations.MerchantEvidenceCollectionService;
import com.sentinq.trust.research.ContextResearchDecision;
import com.sentinq.trust.research.ContextResearchEvidenceFactory;
import com.sentinq.trust.research.ContextResearchFinding;
import com.sentinq.trust.research.ContextResearchService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TrustMapOrchestrationService {

    private static final int MAX_RESEARCH_ROUNDS = 2;

    private final EvidenceInterpretationService interpretationService;
    private final ContextResearchService researchService;
    private final ContextResearchEvidenceFactory evidenceFactory;
    private final MerchantEvidenceCollectionService evidenceCollectionService;

    public TrustMapOrchestrationService(
            EvidenceInterpretationService interpretationService,
            ContextResearchService researchService,
            ContextResearchEvidenceFactory evidenceFactory,
            MerchantEvidenceCollectionService evidenceCollectionService
    ) {
        this.interpretationService =
                interpretationService;

        this.researchService =
                researchService;

        this.evidenceFactory =
                evidenceFactory;

        this.evidenceCollectionService =
                evidenceCollectionService;
    }

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

    public MerchantTrustAssessment assessMerchant(
            String provider,
            CandidateOffer offer,
            TrustContext context
    ) {
        if (offer == null) {
            throw new IllegalArgumentException(
                    "Candidate offer is required."
            );
        }

        List<TrustEvidence> evidence =
                evidenceCollectionService.collectEvidence(
                        provider,
                        offer.getMerchantId(),
                        offer.getMerchantName(),
                        offer,
                        context
                );

        List<TrustEvidenceAssessment> assessments =
                evidence.stream()
                        .map(item ->
                                assessEvidence(
                                        provider,
                                        offer.getMerchantId(),
                                        offer.getMerchantName(),
                                        item,
                                        context
                                )
                        )
                        .toList();

        return new MerchantTrustAssessment(
                offer.getMerchantId(),
                offer.getMerchantName(),
                assessments
        );
    }
}