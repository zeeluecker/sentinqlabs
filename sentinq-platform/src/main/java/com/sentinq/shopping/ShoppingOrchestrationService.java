package com.sentinq.shopping;

import com.sentinq.goal.Goal;
import com.sentinq.mandate.MandateBuilder;
import com.sentinq.mandate.MandateEnvelope;
import com.sentinq.preference.ConsumerPreferences;
import com.sentinq.resolution.CandidateOffer;
import com.sentinq.resolution.LateBindingResolutionService;
import com.sentinq.resolution.ResolutionResult;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class ShoppingOrchestrationService {

    private final MandateBuilder mandateBuilder;
    private final LateBindingResolutionService resolutionService;
    private final MockMerchantSearchService merchantSearchService;

    public ShoppingOrchestrationService(
            MandateBuilder mandateBuilder,
            LateBindingResolutionService resolutionService,
            MockMerchantSearchService merchantSearchService
    ) {
        this.mandateBuilder = mandateBuilder;
        this.resolutionService = resolutionService;
        this.merchantSearchService = merchantSearchService;
    }

    public ShoppingOrchestrationResult orchestrate(
            Goal goal,
            ConsumerPreferences preferences
    ) {
        MandateEnvelope mandate =
                mandateBuilder.build(goal, preferences);

        List<CandidateOffer> candidates =
                merchantSearchService.search(goal);

        List<ResolvedCandidate> resolvedCandidates = candidates.stream()
                .map(candidate -> new ResolvedCandidate(
                        candidate,
                        resolutionService.resolve(candidate, mandate)
                ))
                .toList();

        Optional<ResolvedCandidate> selectedCandidate =
                resolvedCandidates.stream()
                        .filter(candidate ->
                                candidate.resolution().isExecutable())
                        .min(Comparator.comparing(
                                candidate ->
                                        candidate.resolution()
                                                .getResolvedTotalCents()
                        ));

        return new ShoppingOrchestrationResult(
                mandate,
                resolvedCandidates,
                selectedCandidate.orElse(null)
        );
    }
}