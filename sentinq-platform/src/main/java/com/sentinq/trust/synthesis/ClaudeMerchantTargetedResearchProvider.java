package com.sentinq.trust.synthesis;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.WebSearchTool20250305;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinq.trust.TrustContext;
import com.sentinq.trust.TrustEvidence;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ClaudeMerchantTargetedResearchProvider
        implements MerchantTargetedResearchProvider {

    private static final String MODEL =
            Model.CLAUDE_SONNET_5.toString();

    private final AnthropicClient anthropicClient;
    private final ObjectMapper objectMapper;

    public ClaudeMerchantTargetedResearchProvider(
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

        WebSearchTool20250305 webSearchTool =
                WebSearchTool20250305.builder()
                        .maxUses(4L)
                        .build();

        MessageCreateParams params =
                MessageCreateParams.builder()
                        .model(MODEL)
                        .maxTokens(4096L)
                        .addTool(webSearchTool)
                        .addUserMessage(
                                buildPrompt(
                                        merchantId,
                                        merchantName,
                                        synthesis,
                                        context
                                )
                        )
                        .build();

        Message response =
                anthropicClient.messages()
                        .create(params);

        String responseText =
                extractText(response);

        MerchantTargetedResearchDecision decision =
                deserialize(
                        responseText
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
        return """
        You are performing ONE bounded targeted research round
        for a Sentinq merchant Trust Map.

        Return only a valid JSON object.
        Do not include markdown, commentary, citations outside
        the JSON, or a code fence.

        The top-level JSON object must contain exactly one field:
        "findings".

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

        Do not return:
        - question
        - evidenceSummary
        - impact
        - unresolvedQuestions
        - notes
        - merchantId
        - researchRound
        - any other top-level field

        confidence must be a JSON number between 0.0 and 1.0.

        Correct:
        "confidence": 0.75

        Incorrect:
        "confidence": "MEDIUM"

        sourceUrl must be a plain URL string, not Markdown.

        sourceExcerpt should contain concise source evidence relevant
        to the finding and must not contain provider-specific citation
        markup such as <cite> tags.

        Merchant ID:
        %s

        Merchant:
        %s

        Consumer context:
        Goal: %s
        Product category: %s
        Product type: %s
        Product attributes: %s
        Transaction value: %s
        Important TrustDimensions: %s

        Current Trust Map themes:
        %s

        Material questions:
        %s

        The existing themes already represent Sentinq's synthesis
        of the observation evidence.

        Do NOT repeat broad merchant research.

        Your task is to research ONLY the supplied
        MaterialTrustQuestions.

        Research rules:

        1. Research only the supplied MaterialTrustQuestions.

        2. Do not invent additional research questions.

        3. Do not expand into other TrustDimensions.

        4. Search specifically for evidence capable of resolving,
           changing, strengthening, weakening, or qualifying the
           uncertainty represented by the question.

        5. Prefer independent expert, community, review, complaint,
           or specialist evidence when independent verification
           matters.

        6. Merchant first-party sources may establish merchant
           policies, promises, specifications, or claims but are
           not independent proof of performance.

        7. Research is bounded, not exhaustive.

        8. Do not collect repetitive evidence.

        9. Return no more than 3 findings total.

        10. Every finding must directly inform a supplied
            MaterialTrustQuestion.

        11. If reliable evidence cannot resolve a question,
            preserve the uncertainty by returning fewer findings
            rather than inventing weak evidence.

        12. Do not make an overall merchant recommendation.

        13. Do not decide whether the consumer should buy.

        14. Do not invent facts, sources, URLs, or evidence.

        Use only Sentinq's existing enum values.

        TrustDimension values must match the supplied important
        TrustDimensions.

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

        Return only the JSON object.
        """.formatted(
                merchantId,
                merchantName,
                context.goal(),
                context.productCategory(),
                context.productType(),
                context.productAttributes(),
                context.transactionValue(),
                context.importantDimensions(),
                formatThemes(synthesis),
                formatQuestions(synthesis)
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
                        java.util.stream.Collectors.joining(
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
                        java.util.stream.Collectors.joining(
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
                    "Claude returned invalid targeted research JSON: "
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
                .flatMap(content ->
                        content.text().stream()
                )
                .map(text ->
                        text.text()
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Claude returned no targeted research text."
                        )
                );
    }

    private String removeMarkdownCodeFence(
            String responseText
    ) {
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

        if (existingEvidence == null || existingEvidence.isEmpty()) {
            throw new IllegalArgumentException(
                    "Existing evidence is required."
            );
        }

        if (synthesis == null) {
            throw new IllegalArgumentException(
                    "Merchant evidence synthesis is required."
            );
        }

        if (synthesis.materialQuestions() == null
                || synthesis.materialQuestions().isEmpty()) {
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
