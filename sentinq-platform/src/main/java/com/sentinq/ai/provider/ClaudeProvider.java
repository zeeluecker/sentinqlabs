package com.sentinq.ai.provider;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinq.ai.InterpretedShoppingGoal;
import org.springframework.stereotype.Component;
import com.anthropic.models.messages.WebSearchTool20250305;
import com.sentinq.ai.ProductSearchResult;
import com.sentinq.goal.Goal;
import com.sentinq.preference.ConsumerPreferences;
import com.sentinq.trust.TrustContext;
import com.sentinq.trust.TrustEvidence;
import com.sentinq.trust.interpretation.EvidenceInterpretationDecision;
import com.sentinq.trust.interpretation.EvidenceInterpretationProvider;


@Component
public class ClaudeProvider
        implements LlmProvider,
        ProductSearchProvider,
        EvidenceInterpretationProvider {

    private final AnthropicClient anthropicClient;
    private final ObjectMapper objectMapper;
    public static final String MODEL =
            Model.CLAUDE_SONNET_5.toString();

    public ClaudeProvider(
            ObjectMapper objectMapper
    ) {
        this.anthropicClient =
                AnthropicOkHttpClient.fromEnv();

        this.objectMapper = objectMapper;
    }

    @Override
    public String getProviderId() {
        return "claude";
    }

    @Override
    public InterpretedShoppingGoal interpretShoppingGoal(
            String rawGoalText
    ) {
        validateGoalText(rawGoalText);

        MessageCreateParams params =
                MessageCreateParams.builder()
                        .model(MODEL)
                        .maxTokens(1024L)
                        .addUserMessage(
                                buildPrompt(rawGoalText)
                        )
                        .build();

        Message response =
                anthropicClient.messages()
                        .create(params);

        String responseText =
                extractText(response);

        return deserializeInterpretation(
                responseText
        );
    }

    @Override
    public EvidenceInterpretationDecision interpretEvidence(
            TrustEvidence evidence,
            TrustContext context
    ) {
        validateEvidenceInterpretationInputs(
                evidence,
                context
        );

        MessageCreateParams params =
                MessageCreateParams.builder()
                        .model(MODEL)
                        .maxTokens(2048L)
                        .addUserMessage(
                                buildEvidenceInterpretationPrompt(
                                        evidence,
                                        context
                                )
                        )
                        .build();

        Message response =
                anthropicClient.messages()
                        .create(params);

        String responseText =
                extractText(response);

        return deserializeEvidenceInterpretation(
                responseText
        );
    }

    private EvidenceInterpretationDecision deserializeEvidenceInterpretation(
            String responseText
    ) {
        String cleanedResponse =
                removeMarkdownCodeFence(
                        responseText
                );

        try {
            return objectMapper.readValue(
                    cleanedResponse,
                    EvidenceInterpretationDecision.class
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Claude returned invalid evidence-interpretation JSON: "
                            + responseText,
                    exception
            );
        }
    }

    private String buildEvidenceInterpretationPrompt(
            TrustEvidence evidence,
            TrustContext context
    ) {
        return """
            You are interpreting trust evidence for Sentinq Trust Maps.

            Return only a valid JSON object.
            Do not include markdown, commentary, or a code fence.
                You must use only the following Sentinq enum values.
                
                            Allowed InterpretationStatus values:
                            - DIRECT_SIGNAL
                            - CONTEXT_REQUIRED
                            - CONTEXT_RESOLVED
                            - AMBIGUOUS
                            - MISLEADING_WITHOUT_CONTEXT
                            - INSUFFICIENT_EVIDENCE
                            - NOT_RELEVANT
                
                            Allowed ContextType values:
                            - CONSUMER_OBJECTIVE
                            - TRANSACTION_CHANNEL
                            - MERCHANT_PROMISE
                            - TIME_HORIZON
                            - PRODUCT_ATTRIBUTE
                            - CATEGORY_NORM
                            - COMMUNITY_NORM
                            - USAGE_CONDITION
                
                            Allowed TrustSignal values:
                            - STRONGLY_SUPPORTIVE
                            - SUPPORTIVE
                            - NEUTRAL
                            - MIXED
                            - CONCERNING
                            - STRONGLY_CONCERNING
                            - NO_INFERENCE
                
                            Do not invent or return any enum value that is not
                            listed above.

            Use exactly this structure:

            {
              "status": "CONTEXT_REQUIRED",
              "apparentMeaning": "string",
              "contextRequirements": [
                {
                  "type": "PRODUCT_ATTRIBUTE",
                  "question": "string",
                  "required": true
                }
              ],
              "contextualMeaning": null,
              "signal": "NO_INFERENCE",
              "confidence": 0.0,
              "supportingEvidenceIds": [],
              "contradictingEvidenceIds": []
            }

            Raw evidence:
            - Evidence ID: %s
            - Proposed trust dimension: %s
            - Raw claim: %s
            - Source excerpt: %s
            - Evidence source type: %s
            - Source independence: %s
            - Source expertise: %s
            - Transaction channel: %s
            - Evidence horizon: %s
            - Published at: %s

            Consumer shopping context:
            - Goal: %s
            - Product category: %s
            - Product type: %s
            - Product attributes: %s
            - Transaction value: %s
            - Delivery urgency: %s
            - Merchant familiarity: %s
            - Important trust dimensions: %s

            Interpretation rules:

            1. Preserve the distinction between evidence and conclusion.
               Consumer dissatisfaction is evidence, but is not automatically
               evidence of merchant untrustworthiness.

            2. Do not assume that a negative-sounding observation is a
               negative trust signal.

            3. Interpret evidence in the context of the consumer's goal,
               product, transaction, and relevant trust dimension.

            4. Distinguish immediate perception from longer-term outcomes.
               Evidence at one time horizon must not automatically establish
               conclusions about another time horizon.

            5. Distinguish merchant behavior from transaction-channel behavior.
               Evidence arising from a marketplace or third-party retailer
               must not automatically be attributed to the merchant.

            6. Do not manufacture missing facts.

            7. If important context is missing, use CONTEXT_REQUIRED and
               identify the specific missing information in
               contextRequirements.

            8. When context is required and no reliable trust inference can
               yet be made, use NO_INFERENCE.

            9. apparentMeaning describes what the evidence appears to suggest
               before missing context is resolved.

            10. contextualMeaning must be null when required context has not
                yet been established.

            11. supportingEvidenceIds and contradictingEvidenceIds may contain
                only evidence IDs supplied in this request. Never invent IDs.

            12. confidence represents confidence in this interpretation,
                not an overall merchant trust score.

            13. Do not produce an overall merchant trust score or merchant
                recommendation.

            14. Use NOT_RELEVANT when the evidence does not meaningfully
                inform the proposed trust dimension in this context.

            15. Use INSUFFICIENT_EVIDENCE when the evidence is too weak for
                meaningful interpretation and additional context would not
                reasonably resolve that weakness.

            Return only the JSON object.

            """.formatted(
                evidence.evidenceId(),
                evidence.proposedDimension(),
                evidence.rawClaim(),
                evidence.sourceExcerpt(),
                evidence.source().type(),
                evidence.source().independence(),
                evidence.source().expertise(),
                evidence.channel(),
                evidence.evidenceHorizon(),
                evidence.publishedAt(),

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

    private void validateEvidenceInterpretationInputs(
            TrustEvidence evidence,
            TrustContext context
    ) {
        if (evidence == null) {
            throw new IllegalArgumentException(
                    "Trust evidence is required for interpretation."
            );
        }

        if (context == null) {
            throw new IllegalArgumentException(
                    "Trust context is required for interpretation."
            );
        }
    }




    @Override
    public ProductSearchResult searchProducts(
            Goal goal,
            ConsumerPreferences preferences
    ) {
        validateSearchInputs(
                goal,
                preferences
        );

        WebSearchTool20250305 webSearchTool =
                WebSearchTool20250305.builder()
                        .maxUses(5L)
                        .build();

        MessageCreateParams params =
                MessageCreateParams.builder()
                        .model(MODEL)
                        .maxTokens(8192L)
                        .addTool(webSearchTool)
                        .addUserMessage(
                                buildProductSearchPrompt(
                                        goal,
                                        preferences
                                )
                        )
                        .build();

        Message response =
                anthropicClient.messages()
                        .create(params);

        System.out.println(
                "Claude stop reason: "
                        + response.stopReason()
        );

        System.out.println(
                "Claude response content: "
                        + response.content()
        );


        String responseText =
                extractText(response);

        return deserializeSearchResult(
                responseText
        );
    }

    private String buildProductSearchPrompt(
            Goal goal,
            ConsumerPreferences preferences
    ) {
        return """
            You are performing real-time product discovery for Sentinq.

            Use web search to find real, currently listed products.

            Return only a valid JSON object.
            Do not include markdown, commentary, citations outside the JSON,
            or a code fence.

            Use exactly this structure:

            {
              "offers": [
                {
                  "merchantName": "string",
                  "productName": "string",
                  "productUrl": "string",
                  "productPriceCents": 0,
                  "inventoryAvailable": true,
                  "matchReason": "string"
                }
              ]
            }

            Shopping goal:
            - Product: %s
            - Original request: %s
            - Maximum total budget: %d cents
            - Delivery deadline: %s
            - Substitutions allowed: %s

            Consumer merchant preferences:
            - Preferred merchants: %s
            - Avoided merchants: %s
            - Preferred merchant types: %s
            - Minimum fulfillment score: %s
            - Minimum review score: %s
            - Ask before using a new merchant: %s

            Rules:
            - Return no more than five offers.
            - Search the current public web before answering.
            - Prefer explicitly preferred merchants when they sell
              a relevant product.
            - Never return merchants listed as avoided.
            - Use only real product pages found during search.
            - Do not invent merchants, URLs, prices, or inventory.
            - productPriceCents must contain the listed item price only.
            - If the listed price cannot be verified, omit that offer.
            - Shipping, taxes, and final delivery feasibility will be
              resolved separately by Sentinq.
            - Include a concise matchReason for each offer.

            """.formatted(
                goal.getProductName(),
                goal.getOriginalRequest(),
                goal.getMaximumTotalCents(),
                goal.getDeliveryDeadline(),
                goal.isSubstitutionsAllowed(),
                preferences.getPreferredMerchants(),
                preferences.getAvoidedMerchants(),
                preferences.getPreferredMerchantTypes(),
                preferences.getPreferredMinimumFulfillmentScore(),
                preferences.getPreferredMinimumReviewScore(),
                preferences.isAskBeforeUsingNewMerchant()
        );
    }

    private ProductSearchResult deserializeSearchResult(
            String responseText
    ) {
        String cleanedResponse =
                removeMarkdownCodeFence(
                        responseText
                );

        try {
            return objectMapper.readValue(
                    cleanedResponse,
                    ProductSearchResult.class
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Claude returned invalid product-search JSON: "
                            + responseText,
                    exception
            );
        }
    }

    private void validateSearchInputs(
            Goal goal,
            ConsumerPreferences preferences
    ) {
        if (goal == null) {
            throw new IllegalArgumentException(
                    "Goal is required for product search."
            );
        }

        if (preferences == null) {
            throw new IllegalArgumentException(
                    "Consumer preferences are required for product search."
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
                                "Claude returned no text response."
                        )
                );
    }

    private InterpretedShoppingGoal deserializeInterpretation(
            String responseText
    ) {
        String cleanedResponse =
                removeMarkdownCodeFence(
                        responseText
                );

        try {
            return objectMapper.readValue(
                    cleanedResponse,
                    InterpretedShoppingGoal.class
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Claude returned an invalid goal interpretation: "
                            + responseText,
                    exception
            );
        }
    }

    private String buildPrompt(
            String rawGoalText
    ) {
        return """
                You are interpreting a consumer shopping goal for Sentinq.

                Return only a valid JSON object.
                Do not include markdown, commentary, or a code fence.

                Use exactly this structure:

                {
                  "productName": "string",
                  "maximumTotalCents": 0,
                  "deliveryDeadline": "YYYY-MM-DD",
                  "substitutionsAllowed": false,
                  "productRequirements": [],
                  "clarificationQuestions": []
                }

                Rules:
                - productName must be concise and useful for product discovery.
                - maximumTotalCents must represent the total budget in cents.
                - deliveryDeadline must be formatted as YYYY-MM-DD.
                - Do not invent a budget or deadline.
                - If a critical value is missing, add a clarification question.
                - productRequirements should capture relevant appearance,
                  size, compatibility, usage, or delivery requirements.
                - If substitutions are not mentioned, set
                  substitutionsAllowed to false.
                - clarificationQuestions must be an empty list when none
                  are required.

                Consumer goal:
                %s
                """.formatted(rawGoalText);
    }

    private void validateGoalText(
            String rawGoalText
    ) {
        if (rawGoalText == null ||
                rawGoalText.isBlank()) {
            throw new IllegalArgumentException(
                    "Goal text is required."
            );
        }
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
}