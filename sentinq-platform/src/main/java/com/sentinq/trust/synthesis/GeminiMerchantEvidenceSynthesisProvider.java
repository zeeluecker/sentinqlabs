package com.sentinq.trust.synthesis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.sentinq.trust.ContextType;
import com.sentinq.trust.TrustContext;
import com.sentinq.trust.TrustDimension;
import com.sentinq.trust.TrustEvidence;
import com.sentinq.trust.TrustSignal;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GeminiMerchantEvidenceSynthesisProvider
        implements MerchantEvidenceSynthesisProvider {

    private static final String MODEL =
            "gemini-3.1-pro-preview";

    private final Client geminiClient;
    private final ObjectMapper objectMapper;

    public GeminiMerchantEvidenceSynthesisProvider(
            ObjectMapper objectMapper
    ) {
        this.geminiClient =
                Client.builder()
                        .apiKey(
                                System.getenv(
                                        "GEMINI_API_KEY"
                                )
                        )
                        .build();

        this.objectMapper =
                objectMapper;
    }

    @Override
    public String providerId() {
        return "gemini";
    }

    @Override
    public MerchantEvidenceSynthesis synthesizeEvidence(
            String merchantId,
            String merchantName,
            List<TrustEvidence> evidence,
            TrustContext context
    ) {
        validateSynthesisInputs(
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
                "[Gemini Synthesis] Prompt chars: "
                        + prompt.length()
        );

        GenerateContentResponse response =
                geminiClient.models.generateContent(
                        MODEL,
                        prompt,
                        null
                );

        return deserializeSynthesis(
                response.text()
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
        validateRefinementInputs(
                merchantId,
                merchantName,
                initialSynthesis,
                researchedEvidence,
                context
        );

        String prompt =
                buildRefinementPrompt(
                        merchantId,
                        merchantName,
                        initialSynthesis,
                        researchedEvidence,
                        context
                );

        System.out.println(
                "[Gemini Refinement] Prompt chars: "
                        + prompt.length()
        );

        GenerateContentResponse response =
                geminiClient.models.generateContent(
                        MODEL,
                        prompt,
                        null
                );

        return deserializeSynthesis(
                response.text()
        );
    }

    private String buildEvidenceSynthesisPrompt(
            String merchantId,
            String merchantName,
            List<TrustEvidence> evidence,
            TrustContext context
    ) {
        List<TrustEvidence> relevantEvidence =
                evidence.stream()
                        .filter(item ->
                                context.importantDimensions()
                                        .contains(
                                                item.proposedDimension()
                                        )
                        )
                        .toList();

        String formattedContext =
                formatTrustContext(
                        context
                );

        String formattedEvidence =
                formatEvidenceForSynthesis(
                        relevantEvidence
                );

        return """
            You are synthesizing a merchant Trust Map for Sentinq.

            Return only a valid JSON object.
            Do not include markdown, commentary, citations outside
            the JSON, or a code fence.

            Merchant ID:
            %s

            Merchant name:
            %s

            Consumer context:
            %s

            Evidence:
            %s

            Allowed TrustDimension values:
            %s

            Allowed TrustSignal values:
            %s

            Allowed ContextType values:
            %s

            The top-level JSON object must contain exactly these fields:

            - merchantId
            - merchantName
            - themes
            - materialQuestions
            - supportingEvidenceIds
            - confidence

            Do not add any other top-level fields.

            Use exactly this JSON structure:

            {
              "merchantId": "string",
              "merchantName": "string",
              "themes": [
                {
                  "dimension": "PRODUCT_QUALITY",
                  "theme": "string",
                  "signal": "MIXED",
                  "evidenceIds": ["evidence-id"]
                }
              ],
              "materialQuestions": [
                {
                  "dimension": "PRODUCT_QUALITY",
                  "contextType": "PRODUCT_ATTRIBUTE",
                  "question": "string",
                  "reason": "string"
                }
              ],
              "supportingEvidenceIds": [
                "evidence-id"
              ],
              "confidence": 0.75
            }

            Field names must match this structure exactly.

            Do not rename fields or introduce aliases.

            Do not return fields such as:
            - trustDimension
            - summary
            - evidenceSummary
            - impact
            - unresolvedQuestions
            - resolvedQuestions
            - notes
            - assessment
            - overallSignal
            - recommendation
            - researchSummary

            confidence must be a JSON number between 0.0 and 1.0.

            themes must always be a JSON array.

            materialQuestions must always be a JSON array.

            supportingEvidenceIds must always be a JSON array.

            theme.evidenceIds must always be a JSON array.

            If there are no material questions, return:

            "materialQuestions": []

            If there are no supporting evidence IDs, return:

            "supportingEvidenceIds": []

            merchantId and merchantName must match the supplied
            values exactly.

            Understand the evidence landscape across all supplied
            evidence and return a bounded merchant-level synthesis.

            Synthesis rules:

            1. Group related evidence into a small number of meaningful
               recurring themes.

            2. Do not create one theme per evidence item.

            3. Preserve disagreement and uncertainty.
               Conflicting evidence is part of the Trust Map.

            4. Evidence volume is not proof.
               Repeated similar claims should not automatically
               increase confidence.

            5. Merchant first-party evidence establishes what the
               merchant claims, promises, or describes.

            6. Merchant first-party evidence is not independent proof
               that the merchant consistently performs as claimed.

            7. Independent customer, community, complaint, and expert
               evidence may corroborate, contradict, qualify, or
               contextualize merchant claims.

            8. Evaluate only TrustDimensions identified as important
               in the supplied consumer context.

            9. Any TrustDimension not listed in Important TrustDimensions
               must be excluded from themes and materialQuestions.

            10. A MaterialTrustQuestion must represent meaningful
                conflict, ambiguity, or uncertainty whose resolution
                could plausibly change the trust assessment for this
                consumer or purchase.

            11. Do not create a MaterialTrustQuestion merely because
                additional information could theoretically be useful.

            12. Do not create a MaterialTrustQuestion merely because
                a merchant claim lacks independent verification.

            13. Prefer the smallest set of decision-relevant questions.

            14. Combine substantially overlapping questions.

            15. Normally return no more than one MaterialTrustQuestion
                per important TrustDimension.

            16. Return zero MaterialTrustQuestions when further research
                is unlikely to materially change the assessment.

            17. Normally return no more than one theme per important
                TrustDimension unless materially conflicting evidence
                requires separate themes.

            18. Keep each theme concise, normally no more than
                2-3 sentences.

            19. theme.evidenceIds may contain only evidence IDs supplied
                in this request.

            20. supportingEvidenceIds may contain only evidence IDs
                supplied in this request.

            21. Never invent evidence IDs.

            22. supportingEvidenceIds should contain only materially
                relevant evidence IDs, not every supplied evidence ID.

            23. Preserve uncertainty when the evidence does not justify
                a stronger conclusion.

            24. Do not produce an overall merchant recommendation.

            25. Do not decide whether the consumer should buy.

            26. confidence represents confidence in the synthesis of
                the evidence landscape, not confidence in the merchant.

            27. Preserve merchantId and merchantName exactly as supplied.

            Return only the complete JSON object.
            """.formatted(
                merchantId,
                merchantName,
                formattedContext,
                formattedEvidence,
                Arrays.toString(
                        TrustDimension.values()
                ),
                Arrays.toString(
                        TrustSignal.values()
                ),
                Arrays.toString(
                        ContextType.values()
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
        List<TrustEvidence> relevantResearch =
                researchedEvidence.stream()
                        .filter(item ->
                                context.importantDimensions()
                                        .contains(
                                                item.proposedDimension()
                                        )
                        )
                        .toList();

        String formattedContext =
                formatTrustContext(
                        context
                );

        String formattedInitialSynthesis =
                formatSynthesisForRefinement(
                        initialSynthesis
                );

        String formattedResearch =
                formatEvidenceForSynthesis(
                        relevantResearch
                );

        return """
            You are refining an existing Sentinq merchant Trust Map
            after one bounded targeted research round.

            Return only a valid JSON object.
            Do not include markdown, commentary, citations outside
            the JSON, or a code fence.

            Merchant ID:
            %s

            Merchant name:
            %s

            Consumer context:
            %s

            Existing synthesis:
            %s

            Newly researched evidence:
            %s

            Allowed TrustDimension values:
            %s

            Allowed TrustSignal values:
            %s

            Allowed ContextType values:
            %s

            The top-level JSON object must contain exactly these fields:

            - merchantId
            - merchantName
            - themes
            - materialQuestions
            - supportingEvidenceIds
            - confidence

            Do not add any other top-level fields.

            Use exactly this JSON structure:

            {
              "merchantId": "string",
              "merchantName": "string",
              "themes": [
                {
                  "dimension": "PRODUCT_QUALITY",
                  "theme": "string",
                  "signal": "MIXED",
                  "evidenceIds": ["evidence-id"]
                }
              ],
              "materialQuestions": [
                {
                  "dimension": "PRODUCT_QUALITY",
                  "contextType": "PRODUCT_ATTRIBUTE",
                  "question": "string",
                  "reason": "string"
                }
              ],
              "supportingEvidenceIds": [
                "evidence-id"
              ],
              "confidence": 0.75
            }

            Field names must match this structure exactly.

            confidence must be a JSON number between 0.0 and 1.0.

            themes, materialQuestions, supportingEvidenceIds,
            and theme.evidenceIds must always be JSON arrays.

            If there are no material questions, return:

            "materialQuestions": []

            The refined response must use the exact same
            MerchantEvidenceSynthesis structure shown above.

            Do not return:
            - changedThemes
            - resolvedQuestions
            - unresolvedQuestions
            - refinementNotes
            - researchSummary
            - delta
            - patch

            Apply the refinement internally and return the complete
            final MerchantEvidenceSynthesis object.

            Refinement rules:

            1. Treat the existing synthesis as the established
               interpretation of the original evidence landscape.

            2. Do not reconstruct the Trust Map from scratch.

            3. Evaluate only how the newly researched evidence changes,
               strengthens, weakens, qualifies, or preserves existing
               themes and signals.

            4. Preserve themes unaffected by the new research.

            5. Update a theme only when new evidence materially affects
               its interpretation.

            6. Resolve a MaterialTrustQuestion when the new evidence
               materially answers it.

            7. If material uncertainty remains, preserve the question.

            8. Do not create another research loop.

            9. Do not create new MaterialTrustQuestions merely because
               more information could theoretically be useful.

            10. Do not expand into new TrustDimensions.

            11. Any TrustDimension not listed in Important TrustDimensions
                must be excluded from themes and materialQuestions.

            12. Preserve existing evidence IDs when an existing theme
                remains valid.

            13. Add newly researched evidence IDs only when the evidence
                materially supports, qualifies, contradicts, or changes
                a theme.

            14. theme.evidenceIds and supportingEvidenceIds may contain
                only IDs from the existing synthesis or newly researched
                evidence.

            15. Never invent evidence IDs.

            16. Keep themes concise, normally no more than 2-3 sentences.

            17. Preserve disagreement and uncertainty when the new
                evidence does not justify a stronger conclusion.

            18. Merchant first-party evidence establishes what the
                merchant says or promises but is not independent proof
                of performance.

            19. confidence represents confidence in the refined
                synthesis, not confidence in the merchant overall.

            20. Do not make an overall merchant recommendation.

            21. Do not decide whether the consumer should buy.

            22. Preserve merchantId and merchantName exactly as supplied.

            Return only the complete JSON object.
            """.formatted(
                merchantId,
                merchantName,
                formattedContext,
                formattedInitialSynthesis,
                formattedResearch,
                Arrays.toString(
                        TrustDimension.values()
                ),
                Arrays.toString(
                        TrustSignal.values()
                ),
                Arrays.toString(
                        ContextType.values()
                )
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
                        Collectors.joining(
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
                                Collectors.joining(
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
                                Collectors.joining(
                                        "\n---\n"
                                )
                        );

        return """
            Themes:
            %s

            Material questions:
            %s

            Supporting evidence IDs:
            %s

            Confidence:
            %s
            """.formatted(
                themes,
                questions,
                synthesis.supportingEvidenceIds(),
                synthesis.confidence()
        );
    }

    private MerchantEvidenceSynthesis deserializeSynthesis(
            String responseText
    ) {
        String cleaned =
                removeMarkdownCodeFence(
                        responseText
                );

        try {
            return objectMapper.readValue(
                    cleaned,
                    MerchantEvidenceSynthesis.class
            );

        } catch (JsonProcessingException exception) {

            throw new IllegalStateException(
                    "Gemini returned invalid merchant-synthesis JSON: "
                            + responseText,
                    exception
            );
        }
    }

    private String removeMarkdownCodeFence(
            String responseText
    ) {
        if (responseText == null ||
                responseText.isBlank()) {

            throw new IllegalArgumentException(
                    "Gemini response text is required."
            );
        }

        String cleaned =
                responseText.trim();

        if (cleaned.startsWith("```")) {

            int firstLineBreak =
                    cleaned.indexOf('\n');

            if (firstLineBreak >= 0) {
                cleaned =
                        cleaned.substring(
                                firstLineBreak + 1
                        );
            }
        }

        if (cleaned.endsWith("```")) {
            cleaned =
                    cleaned.substring(
                            0,
                            cleaned.length() - 3
                    );
        }

        return cleaned.trim();
    }

    private void validateSynthesisInputs(
            String merchantId,
            String merchantName,
            List<TrustEvidence> evidence,
            TrustContext context
    ) {
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

        if (evidence == null ||
                evidence.isEmpty()) {

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

    private void validateRefinementInputs(
            String merchantId,
            String merchantName,
            MerchantEvidenceSynthesis initialSynthesis,
            List<TrustEvidence> researchedEvidence,
            TrustContext context
    ) {
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

        if (context == null) {

            throw new IllegalArgumentException(
                    "Trust context is required."
            );
        }
    }
}