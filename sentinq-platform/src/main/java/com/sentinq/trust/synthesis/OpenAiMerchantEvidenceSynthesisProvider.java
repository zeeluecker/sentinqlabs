package com.sentinq.trust.synthesis;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.StructuredResponse;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.sentinq.trust.TrustContext;
import com.sentinq.trust.TrustEvidence;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OpenAiMerchantEvidenceSynthesisProvider
        implements MerchantEvidenceSynthesisProvider {

    private static final String SYNTHESIS_MODEL = "gpt-5";
    private static final String REFINEMENT_MODEL = "gpt-5.4-mini";

    private final OpenAIClient openAIClient;

    public OpenAiMerchantEvidenceSynthesisProvider() {
        this.openAIClient =
                OpenAIOkHttpClient.fromEnv();
    }

    @Override
    public String providerId() {
        return "openai";
    }

    @Override
    public MerchantEvidenceSynthesis synthesizeEvidence(
            String merchantId,
            String merchantName,
            List<TrustEvidence> evidence,
            TrustContext context
    ) {
        validateInputs(
                merchantId,
                merchantName,
                evidence,
                context
        );

        String prompt =
                buildEvidenceSynthesisPrompt(
                        merchantId,
                        merchantName,
                        evidence,
                        context
                );

        System.out.println(
                "[Synthesis] Prompt chars: "
                        + prompt.length()
        );

        StructuredResponseCreateParams<MerchantEvidenceSynthesis> params =
                ResponseCreateParams.builder()
                        .model(SYNTHESIS_MODEL)
                        .input(prompt)
                        .text(
                                MerchantEvidenceSynthesis.class
                        )
                        .build();

        StructuredResponse<MerchantEvidenceSynthesis> response =
                openAIClient.responses()
                        .create(params);

        return response.output()
                .stream()
                .flatMap(outputItem ->
                        outputItem.message().stream()
                )
                .flatMap(message ->
                        message.content().stream()
                )
                .flatMap(content ->
                        content.outputText().stream()
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "OpenAI returned no structured merchant evidence synthesis."
                        )
                );
    }

    private void validateInputs(
            String merchantId,
            String merchantName,
            List<TrustEvidence> evidence,
            TrustContext context
    ) {
        if (merchantId == null || merchantId.isBlank()) {
            throw new IllegalArgumentException(
                    "Merchant ID is required."
            );
        }

        if (merchantName == null || merchantName.isBlank()) {
            throw new IllegalArgumentException(
                    "Merchant name is required."
            );
        }

        if (evidence == null || evidence.isEmpty()) {
            throw new IllegalArgumentException(
                    "Evidence is required."
            );
        }

        if (context == null) {
            throw new IllegalArgumentException(
                    "Trust context is required."
            );
        }
    }

    private String buildEvidenceSynthesisPrompt(
            String merchantId,
            String merchantName,
            List<TrustEvidence> evidence,
            TrustContext context
    ) {
        String formattedContext =
                formatTrustContext(
                        context
                );

        List<TrustEvidence> relevantEvidence =
                evidence.stream()
                        .filter(item ->
                                context.importantDimensions()
                                        .contains(
                                                item.proposedDimension()
                                        )
                        )
                        .toList();

        String formattedEvidence =
                formatEvidenceForSynthesis(
                        relevantEvidence
                );

        return """
            You are synthesizing a merchant Trust Map for Sentinq.

            Merchant ID:
            %s

            Merchant name:
            %s

            Consumer context:
            %s

            Evidence:
            %s

            Understand the evidence landscape across all supplied
            evidence and return a bounded merchant-level synthesis.

            Rules:

            1. Group related evidence into a small number of meaningful
               themes. Do not create one theme per evidence item.

            2. Preserve disagreement and uncertainty. Conflicting
               evidence is part of the Trust Map.

            3. Evidence volume is not proof. Repeated similar claims
               should not automatically increase confidence.

            4. Merchant first-party evidence establishes what the
               merchant claims, promises, or describes. It is not
               independent proof of performance.

            5. Independent customer, community, complaint, and expert
               evidence may corroborate, contradict, qualify, or
               contextualize merchant claims.

            6. Evaluate only the TrustDimensions marked important in
               the supplied TrustContext. Other evidence may remain in
               the record but must not create themes or research scope.

            7. A MaterialTrustQuestion must represent meaningful
               conflict, ambiguity, or uncertainty whose resolution
               could plausibly change this consumer's trust assessment.

            8. Do not create a MaterialTrustQuestion merely because a
               merchant claim lacks independent verification or because
               additional information might theoretically be useful.

            9. Prefer the smallest set of decision-relevant questions.
               Combine overlapping questions and normally return no
               more than one question per important TrustDimension.
               Return zero when further research is unlikely to matter.

            10. Evidence IDs in themes and supportingEvidenceIds must
                come only from supplied evidence. Never invent IDs.

            11. Do not produce an overall merchant recommendation or
                decide whether the consumer should buy.

            12. confidence represents confidence in this synthesis of
                the evidence landscape, not merchant trust overall.

            Return only MerchantEvidenceSynthesis.

            Preserve merchantId and merchantName exactly.
            """.formatted(
                merchantId,
                merchantName,
                formattedContext,
                formattedEvidence
        );
    }

    private String formatEvidenceForSynthesis(
            List<TrustEvidence> evidence
    ) {
        return evidence.stream()
                .map(item -> """
                Evidence ID: %s
                Dimension: %s
                Source: %s
                Source type: %s
                Independence: %s
                Expertise: %s
                Channel: %s
                Horizon: %s
                Claim: %s
                """.formatted(
                        item.evidenceId(),
                        item.proposedDimension(),
                        item.source().name(),
                        item.source().type(),
                        item.source().independence(),
                        item.source().expertise(),
                        item.channel(),
                        item.evidenceHorizon(),
                        item.rawClaim()
                ))
                .collect(
                        java.util.stream.Collectors.joining(
                                "\n---\n"
                        )
                );
    }

    private String formatTrustContext(
            TrustContext context
    ) {
        return """
            Goal: %s
            Product category: %s
            Product type: %s
            Product attributes: %s
            Transaction value: %s
            Delivery urgency: %s
            Merchant familiarity: %s
            Important TrustDimensions: %s
            """.formatted(
                context.goal(),
                context.productCategory(),
                context.productType(),
                context.productAttributes(),
                context.transactionValue(),
                context.deliveryUrgency(),
                context.merchantFamiliarity(),
                context.importantDimensions()
        );
    }

    @Override
    public MerchantEvidenceSynthesis refineSynthesis(
            String merchantId,
            String merchantName,
            MerchantEvidenceSynthesis initialSynthesis,
            List<TrustEvidence> researchedEvidence,
            TrustContext context
    ) {
        if (initialSynthesis == null) {
            throw new IllegalArgumentException(
                    "Initial synthesis is required."
            );
        }

        if (researchedEvidence == null) {
            throw new IllegalArgumentException(
                    "Researched evidence is required."
            );
        }
        String prompt =
                buildRefinementPrompt(
                        merchantId,
                        merchantName,
                        initialSynthesis,
                        researchedEvidence,
                        context
                );

        System.out.println(
                "[Refinement] Prompt chars: "
                        + prompt.length()
        );

        StructuredResponseCreateParams<MerchantEvidenceSynthesis> params =
                ResponseCreateParams.builder()
                        .model(REFINEMENT_MODEL)
                        .input(prompt).text(
                                MerchantEvidenceSynthesis.class
                        )
                        .build();

        StructuredResponse<MerchantEvidenceSynthesis> response =
                openAIClient.responses()
                        .create(params);

        return response.output()
                .stream()
                .flatMap(outputItem ->
                        outputItem.message().stream()
                )
                .flatMap(message ->
                        message.content().stream()
                )
                .flatMap(content ->
                        content.outputText().stream()
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "OpenAI returned no structured refined merchant synthesis."
                        )
                );
    }

    private String buildRefinementPrompt(
            String merchantId,
            String merchantName,
            MerchantEvidenceSynthesis initialSynthesis,
            List<TrustEvidence> researchedEvidence,
            TrustContext context
    ) {
        String formattedContext =
                formatTrustContext(
                        context
                );

        String formattedInitialSynthesis =
                formatSynthesisForRefinement(
                        initialSynthesis
                );

        List<TrustEvidence> relevantResearch =
                researchedEvidence.stream()
                        .filter(item ->
                                context.importantDimensions()
                                        .contains(
                                                item.proposedDimension()
                                        )
                        )
                        .toList();

        String formattedResearch =
                formatEvidenceForSynthesis(
                        relevantResearch
                );

        return """
            You are refining an existing Sentinq merchant Trust Map
            after one bounded targeted research round.

            Merchant ID:
            %s

            Merchant:
            %s

            Consumer context:
            %s

            Existing synthesis:
            %s

            Newly researched evidence:
            %s

            Refinement rules:

            1. Treat the existing synthesis as the established
               interpretation of the original evidence landscape.

            2. Do NOT reconstruct the Trust Map from scratch.

            3. Evaluate only how the newly researched evidence
               changes, strengthens, weakens, qualifies, or preserves
               the existing themes and signals.

            4. Preserve unaffected themes.

            5. Update a theme only when the new evidence materially
               affects its interpretation.

            6. Resolve a MaterialTrustQuestion when the new evidence
               materially answers it.

            7. If uncertainty remains, preserve the question or
               uncertainty. Do not create another research loop.

            8. Do not expand into new TrustDimensions merely because
               the research surfaced unrelated information.

            9. New information may be incorporated when it directly
               answers one of the supplied MaterialTrustQuestions.

            10. Preserve original evidence IDs that remain relevant
                to a refined theme, and add researched evidence IDs
                when they materially support the updated conclusion.

            11. Do not invent evidence IDs.

            12. confidence represents confidence in the refined
                synthesis, not merchant trust overall.

            13. Do not make an overall merchant recommendation.

            14. Do not decide whether the consumer should buy.

            Return only MerchantEvidenceSynthesis.

            Preserve merchantId and merchantName exactly.
            """.formatted(
                merchantId,
                merchantName,
                formattedContext,
                formattedInitialSynthesis,
                formattedResearch
        );
    }

    private String formatSynthesisForRefinement(
            MerchantEvidenceSynthesis synthesis
    ) {
        String themes =
                synthesis.themes()
                        .stream()
                        .map(theme -> """
                            Dimension: %s
                            Signal: %s
                            Theme: %s
                            Evidence IDs: %s
                            """.formatted(
                                theme.dimension(),
                                theme.signal(),
                                theme.theme(),
                                theme.evidenceIds()
                        ))
                        .collect(
                                java.util.stream.Collectors.joining(
                                        "\n---\n"
                                )
                        );

        String questions =
                synthesis.materialQuestions()
                        .stream()
                        .map(question -> """
                            Dimension: %s
                            Context type: %s
                            Question: %s
                            Reason: %s
                            """.formatted(
                                question.dimension(),
                                question.contextType(),
                                question.question(),
                                question.reason()
                        ))
                        .collect(
                                java.util.stream.Collectors.joining(
                                        "\n---\n"
                                )
                        );

        return """
            Themes:
            %s

            Material questions:
            %s

            Confidence:
            %s
            """.formatted(
                themes,
                questions,
                synthesis.confidence()
        );
    }
}
