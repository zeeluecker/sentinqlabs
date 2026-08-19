package com.sentinq.trust.synthesis;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class ClaudeMerchantEvidenceSynthesisProvider
        implements MerchantEvidenceSynthesisProvider {

    /*
     * Start with the same strong Claude model for both stages.
     *
     * Once provider equivalence is proven, we can route refinement
     * to a faster Claude model just as we did with OpenAI.
     */
    private static final String MODEL =
            Model.CLAUDE_SONNET_5.toString();

    private final AnthropicClient anthropicClient;
    private final ObjectMapper objectMapper;

    public ClaudeMerchantEvidenceSynthesisProvider(
            ObjectMapper objectMapper
    ) {
        this.anthropicClient =
                AnthropicOkHttpClient.fromEnv();

        this.objectMapper =
                objectMapper;
    }

    @Override
    public String providerId() {
        return "claude";
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
                "[Claude Synthesis] Prompt chars: "
                        + prompt.length()
        );

        MessageCreateParams params =
                MessageCreateParams.builder()
                        .model(MODEL)
                        .maxTokens(8192L)
                        .addUserMessage(prompt)
                        .build();

        Message response =
                anthropicClient.messages()
                        .create(params);

        String responseText =
                extractText(response);

        return deserializeSynthesis(
                responseText
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
                "[Claude Refinement] Prompt chars: "
                        + prompt.length()
        );

        MessageCreateParams params =
                MessageCreateParams.builder()
                        .model(MODEL)
                        .maxTokens(8192L)
                        .addUserMessage(prompt)
                        .build();

        Message response =
                anthropicClient.messages()
                        .create(params);

        String responseText =
                extractText(response);

        return deserializeSynthesis(
                responseText
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

    Correct:
    "confidence": 0.75

    Incorrect:
    "confidence": "HIGH"

    themes must always be a JSON array.

    materialQuestions must always be a JSON array.

    supportingEvidenceIds must always be a JSON array.

    theme.evidenceIds must always be a JSON array.

    If there are no material questions, return:

    "materialQuestions": []

    If there are no supporting evidence IDs, return:

    "supportingEvidenceIds": []

    merchantId and merchantName must match the supplied values exactly.

    Understand the evidence landscape across all supplied
    evidence and return a bounded merchant-level synthesis.

    Synthesis rules:

    1. Group related evidence into a small number of meaningful
       recurring themes.

    2. Do not create one theme per evidence item.

    3. Preserve disagreement and uncertainty.
       Conflicting evidence is part of the Trust Map.

    4. Evidence volume is not proof.
       Repeated similar claims should not automatically increase
       confidence.

    5. Merchant first-party evidence establishes what the merchant
       claims, promises, or describes.

    6. Merchant first-party evidence is not independent proof
       that the merchant consistently performs as claimed.

    7. Independent customer, community, complaint, and expert
       evidence may corroborate, contradict, qualify, or
       contextualize merchant claims.

    8. Evaluate only TrustDimensions identified as important
       in the supplied consumer context.

    9. Do not create themes or MaterialTrustQuestions for
       TrustDimensions outside that scope.

    10. A MaterialTrustQuestion must represent meaningful conflict,
        ambiguity, or uncertainty whose resolution could plausibly
        change the trust assessment for this consumer or purchase.

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

    17. theme.evidenceIds may contain only evidence IDs supplied
        in this request.

    18. supportingEvidenceIds may contain only evidence IDs
        supplied in this request.

    19. Never invent evidence IDs.

    20. supportingEvidenceIds should contain the evidence IDs
        that materially support the overall synthesis.
        It does not need to contain every supplied evidence ID.

    21. Preserve uncertainty when the supplied evidence does not
        justify a stronger conclusion.

    22. Do not produce an overall merchant recommendation.

    23. Do not decide whether the consumer should buy.

    24. confidence represents confidence in the synthesis of
        the evidence landscape, not confidence in the merchant.

    25. Preserve merchantId and merchantName exactly as supplied.

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

    Do not rename fields or introduce aliases.

    Do not return fields such as:
    - trustDimension
    - summary
    - evidenceSummary
    - impact
    - unresolvedQuestions
    - resolvedQuestions
    - changedThemes
    - refinementNotes
    - researchSummary
    - notes
    - assessment
    - overallSignal
    - recommendation
    - delta
    - patch

    confidence must be a JSON number between 0.0 and 1.0.

    Correct:
    "confidence": 0.75

    Incorrect:
    "confidence": "HIGH"

    themes must always be a JSON array.

    theme.evidenceIds must always be a JSON array.

    materialQuestions must always be a JSON array.

    supportingEvidenceIds must always be a JSON array.

    If there are no material questions, return:

    "materialQuestions": []

    If there are no supporting evidence IDs, return:

    "supportingEvidenceIds": []

    merchantId and merchantName must match the supplied values exactly.

    The refined response must use the exact same
    MerchantEvidenceSynthesis JSON structure shown above.

    Do not return a diff, patch, delta, or refinement-specific
    response structure.

    Apply the refinement internally and return the complete final
    MerchantEvidenceSynthesis object.

    Refinement rules:

    1. Treat the existing synthesis as the established
       interpretation of the original evidence landscape.

    2. Do not reconstruct the Trust Map from scratch.

    3. Evaluate only how the newly researched evidence changes,
       strengthens, weakens, qualifies, or preserves existing
       themes and signals.

    4. Preserve themes that are unaffected by the new research.

    5. Update a theme only when the newly researched evidence
       materially affects its interpretation.

    6. A theme signal may strengthen, weaken, or remain unchanged
       when justified by the new evidence.

    7. Resolve a MaterialTrustQuestion when the newly researched
       evidence materially answers it.

    8. If material uncertainty remains after this bounded research
       round, preserve the question in materialQuestions.

    9. Do not create another research loop.

    10. Do not create new MaterialTrustQuestions merely because
        additional information could theoretically be useful.

    11. Do not expand into new TrustDimensions merely because
        targeted research surfaced unrelated information.

    12. Information discovered during targeted research may be
        incorporated only when it directly informs one of the
        supplied MaterialTrustQuestions or materially changes an
        existing in-scope theme.

    13. If an existing theme remains valid, preserve its existing
        evidenceIds.

    14. If newly researched evidence materially supports, qualifies,
        contradicts, or changes a theme, add the relevant researched
        evidence IDs to that theme's evidenceIds.

    15. theme.evidenceIds may contain only:
        - evidence IDs already present in the existing synthesis, or
        - evidence IDs present in the newly researched evidence.

    16. supportingEvidenceIds may contain only:
        - evidence IDs already present in the existing synthesis, or
        - evidence IDs present in the newly researched evidence.

    17. Never invent evidence IDs.

    18. Preserve disagreement and uncertainty when the new evidence
        does not justify a stronger conclusion.

    19. Do not treat the number of similar findings as proof.

    20. Merchant first-party evidence may establish what the merchant
        says, promises, or describes, but is not independent proof
        of performance.

    21. confidence represents confidence in the refined synthesis
        of the evidence landscape, not confidence in the merchant
        overall.

    22. Do not make an overall merchant recommendation.

    23. Do not decide whether the consumer should buy.

    24. Preserve merchantId and merchantName exactly as supplied.

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
        String cleanedResponse =
                removeMarkdownCodeFence(
                        responseText
                );

        try {
            return objectMapper.readValue(
                    cleanedResponse,
                    MerchantEvidenceSynthesis.class
            );

        } catch (JsonProcessingException exception) {

            throw new IllegalStateException(
                    "Claude returned invalid merchant-synthesis JSON: "
                            + responseText,
                    exception
            );
        }
    }

    private String extractText(
            Message response
    ) {
        return response.content()
                .stream()
                .flatMap(contentBlock ->
                        contentBlock.text().stream()
                )
                .map(textBlock ->
                        textBlock.text()
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Claude returned no merchant-synthesis text."
                        )
                );
    }

    private String removeMarkdownCodeFence(
            String responseText
    ) {
        if (responseText == null ||
                responseText.isBlank()) {

            throw new IllegalArgumentException(
                    "Claude response text is required."
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