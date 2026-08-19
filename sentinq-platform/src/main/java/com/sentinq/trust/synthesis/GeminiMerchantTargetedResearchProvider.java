package com.sentinq.trust.synthesis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.GoogleSearch;
import com.google.genai.types.Tool;
import com.sentinq.trust.TrustContext;
import com.sentinq.trust.TrustEvidence;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class GeminiMerchantTargetedResearchProvider
        implements MerchantTargetedResearchProvider {

    private static final String MODEL =
            "gemini-3.1-pro-preview";

    private final Client geminiClient;
    private final ObjectMapper objectMapper;

    public GeminiMerchantTargetedResearchProvider(
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
    public MerchantTargetedResearchDecision research(
            String merchantId,
            String merchantName,
            List<TrustEvidence> existingEvidence,
            MerchantEvidenceSynthesis synthesis,
            TrustContext context
    ) {
        validateInputs(
                merchantId,
                merchantName,
                existingEvidence,
                synthesis,
                context
        );

        Tool googleSearchTool =
                Tool.builder()
                        .googleSearch(
                                GoogleSearch.builder()
                                        .build()
                        )
                        .build();

        GenerateContentConfig config =
                GenerateContentConfig.builder()
                        .tools(
                                List.of(
                                        googleSearchTool
                                )
                        )
                        .build();

        String prompt =
                buildPrompt(
                        merchantId,
                        merchantName,
                        synthesis,
                        context
                );

        GenerateContentResponse response =
                geminiClient.models.generateContent(
                        MODEL,
                        prompt,
                        config
                );

        MerchantTargetedResearchDecision decision =
                deserialize(
                        response.text()
                );

        return new MerchantTargetedResearchDecision(
                decision.findings()
                        .stream()
                        .limit(3)
                        .toList()
        );
    }

    private String buildPrompt(
            String merchantId,
            String merchantName,
            MerchantEvidenceSynthesis synthesis,
            TrustContext context
    ) {
        String formattedThemes =
                formatThemes(
                        synthesis
                );

        String formattedQuestions =
                formatQuestions(
                        synthesis
                );

        return """
            You are performing ONE bounded targeted research round
            for a Sentinq merchant Trust Map.

            Return only a valid JSON object.
            Do not include markdown, commentary, citations outside
            the JSON, or a code fence.

            The top-level JSON object must contain exactly one field:

            - findings

            Do not add any other top-level fields.

            Use exactly this JSON structure:

            {
              "findings": [
                {
                  "dimension": "PRODUCT_QUALITY",
                  "contextType": "PRODUCT_ATTRIBUTE",
                  "finding": "string",
                  "sourceType": "EXPERT_OR_SPECIALIST",
                  "sourceName": "string",
                  "sourceIndependence": "INDEPENDENT",
                  "sourceExpertise": "DOMAIN_EXPERT",
                  "channel": "UNKNOWN",
                  "evidenceHorizon": "MEDIUM_TERM",
                  "sourceUrl": "https://example.com/page",
                  "sourceExcerpt": "string",
                  "confidence": 0.75
                }
              ]
            }

            Field names must match this structure exactly.

            Do not rename fields or introduce aliases.

            Do not return fields such as:
            - question
            - evidenceSummary
            - impact
            - unresolvedQuestions
            - resolvedQuestions
            - researchRound
            - merchantId
            - merchantName
            - notes
            - recommendation

            confidence must be a JSON number between 0.0 and 1.0.

            Correct:
            "confidence": 0.75

            Incorrect:
            "confidence": "MEDIUM"

            sourceUrl must contain only a plain URL string.

            Correct:
            "sourceUrl": "https://example.com/page"

            Incorrect:
            "sourceUrl": "[https://example.com/page](https://example.com/page)"

            sourceExcerpt should contain concise source evidence relevant
            to the finding.

            Do not include provider-specific citation markup inside
            sourceExcerpt.

            Merchant ID:
            %s

            Merchant:
            %s

            Consumer context:
            - Goal: %s
            - Product category: %s
            - Product type: %s
            - Product attributes: %s
            - Transaction value: %s
            - Delivery urgency: %s
            - Merchant familiarity: %s
            - Important TrustDimensions: %s

            Current Trust Map themes:
            %s

            Material questions:
            %s

            The existing themes already represent Sentinq's synthesis
            of the observation evidence.

            Your job is NOT to broadly research this merchant.

            Your job is to find the minimum additional evidence
            necessary to materially clarify the supplied questions.

            Research rules:

            1. Research ONLY the supplied MaterialTrustQuestions.

            2. Do not create or investigate additional questions.

            3. Do not expand into TrustDimensions outside the Important
               TrustDimensions supplied above.

            4. Search specifically for evidence capable of resolving,
               changing, strengthening, weakening, or qualifying the
               uncertainty represented by a supplied question.

            5. Prefer independent expert, community, review, complaint,
               regulatory, or specialist evidence when independent
               verification is relevant.

            6. Merchant first-party sources may clarify policies,
               specifications, promises, or claims but are not
               independent proof of performance.

            7. Research is decision-bounded, not exhaustive.

            8. Stop once additional searching is unlikely to materially
               change the answer to a supplied question.

            9. Do not gather multiple findings that establish
               substantially the same fact.

            10. Return no more than 3 findings total.

            11. Normally return no more than 2 findings for any one
                MaterialTrustQuestion.

            12. Every finding must directly answer, contradict, qualify,
                or materially clarify a supplied MaterialTrustQuestion.

            13. If reliable evidence cannot resolve a question,
                preserve the uncertainty by returning fewer findings
                rather than filling the gap with weak evidence.

            14. Do not make an overall merchant recommendation.

            15. Do not decide whether the consumer should buy.

            16. Do not invent facts, sources, URLs, reviews, policies,
                or source content.

            Use only Sentinq's existing enum values.

            ContextType values:
            - CONSUMER_OBJECTIVE
            - TRANSACTION_CHANNEL
            - MERCHANT_PROMISE
            - TIME_HORIZON
            - PRODUCT_ATTRIBUTE
            - CATEGORY_NORM
            - COMMUNITY_NORM
            - USAGE_CONDITION

            EvidenceSourceType values:
            - MERCHANT_FIRST_PARTY
            - CUSTOMER_REVIEW
            - DOMAIN_COMMUNITY
            - EXPERT_OR_SPECIALIST
            - INDEPENDENT_REVIEW_PLATFORM
            - COMPLAINT_BODY
            - REGULATORY_SOURCE
            - CONSUMER_HISTORY

            EvidenceIndependence values:
            - FIRST_PARTY
            - THIRD_PARTY
            - INDEPENDENT
            - UNKNOWN

            EvidenceExpertise values:
            - GENERAL_CONSUMER
            - EXPERIENCED_CONSUMER
            - DOMAIN_ENTHUSIAST
            - DOMAIN_EXPERT
            - UNKNOWN

            EvidenceChannel values:
            - DIRECT_MERCHANT
            - THIRD_PARTY_RETAILER
            - MARKETPLACE
            - UNKNOWN

            EvidenceHorizon values:
            - PRE_PURCHASE
            - IMMEDIATE
            - SHORT_TERM
            - MEDIUM_TERM
            - LONG_TERM
            - UNKNOWN

            Return only the complete JSON object.
            """.formatted(
                merchantId,
                merchantName,
                context.goal(),
                context.productCategory(),
                context.productType(),
                context.productAttributes(),
                context.transactionValue(),
                context.deliveryUrgency(),
                context.merchantFamiliarity(),
                context.importantDimensions(),
                formattedThemes,
                formattedQuestions
        );
    }

    private String formatThemes(
            MerchantEvidenceSynthesis synthesis
    ) {
        return synthesis.themes()
                .stream()
                .map(theme -> """
                    Dimension: %s
                    Signal: %s
                    Theme: %s
                    """.formatted(
                        theme.dimension(),
                        theme.signal(),
                        theme.theme()
                ))
                .collect(
                        Collectors.joining(
                                "\n---\n"
                        )
                );
    }

    private String formatQuestions(
            MerchantEvidenceSynthesis synthesis
    ) {
        return synthesis.materialQuestions()
                .stream()
                .map(question -> """
                    Dimension: %s
                    Context type: %s
                    Question: %s
                    Why it matters: %s
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
    }

    private MerchantTargetedResearchDecision deserialize(
            String responseText
    ) {
        String cleaned =
                removeMarkdownCodeFence(
                        responseText
                );

        try {
            return objectMapper.readValue(
                    cleaned,
                    MerchantTargetedResearchDecision.class
            );

        } catch (JsonProcessingException exception) {

            throw new IllegalStateException(
                    "Gemini returned invalid targeted research JSON: "
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

    private void validateInputs(
            String merchantId,
            String merchantName,
            List<TrustEvidence> existingEvidence,
            MerchantEvidenceSynthesis synthesis,
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

        if (existingEvidence == null ||
                existingEvidence.isEmpty()) {

            throw new IllegalArgumentException(
                    "Existing evidence is required."
            );
        }

        if (synthesis == null) {

            throw new IllegalArgumentException(
                    "Merchant evidence synthesis is required."
            );
        }

        if (synthesis.materialQuestions() == null ||
                synthesis.materialQuestions().isEmpty()) {

            throw new IllegalArgumentException(
                    "At least one material trust question is required."
            );
        }

        if (context == null) {

            throw new IllegalArgumentException(
                    "Trust context is required."
            );
        }
    }
}