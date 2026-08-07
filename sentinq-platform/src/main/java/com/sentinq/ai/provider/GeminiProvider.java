package com.sentinq.ai.provider;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.sentinq.ai.InterpretedShoppingGoal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GoogleSearch;
import com.google.genai.types.Tool;
import com.sentinq.ai.ProductSearchResult;
import com.sentinq.goal.Goal;
import com.sentinq.preference.ConsumerPreferences;
import java.util.List;

/**
 * Gemini implementation of Sentinq's reasoning-provider contract.
 *
 * Provider-specific Gemini API behavior is encapsulated here so
 * the orchestration layer remains independent of the underlying
 * reasoning engine.
 */
@Component
public class GeminiProvider
        implements LlmProvider, ProductSearchProvider {

    private final Client geminiClient;
    private final ObjectMapper objectMapper;
    public static final String MODEL =
            "gemini-3.1-pro-preview";

    public GeminiProvider(
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
    public String getProviderId() {
        return "gemini";
    }

    /**
     * Interprets a natural-language shopping goal using Gemini
     * and converts the response into Sentinq's structured
     * shopping-goal representation.
     */
    @Override
    public InterpretedShoppingGoal interpretShoppingGoal(
            String rawGoalText
    ) {
        validateGoalText(
                rawGoalText
        );

        GenerateContentResponse response =
                geminiClient.models.generateContent(MODEL,
                        buildPrompt(
                                rawGoalText
                        ),
                        null
                );

        String responseText =
                response.text();

        return deserializeInterpretation(
                responseText
        );
    }

    /**
     * Converts Gemini's JSON response into Sentinq's
     * InterpretedShoppingGoal domain object.
     */
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
        } catch (
                JsonProcessingException exception
        ) {
            throw new IllegalStateException(
                    "Gemini returned an invalid goal interpretation: "
                            + responseText,
                    exception
            );
        }
    }

    /**
     * Builds the provider-specific prompt Gemini uses to convert
     * a consumer shopping request into Sentinq's structured goal.
     */
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
                """.formatted(
                rawGoalText
        );
    }

    /**
     * Removes optional Markdown code fences when Gemini wraps
     * an otherwise valid JSON response in formatting markup.
     */
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

    /**
     * Validates that the consumer supplied a non-empty shopping goal
     * before the request is sent to Gemini.
     */
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

    /**
     * Searches for current merchant offers using Gemini with
     * Google Search grounding.
     *
     * The provider returns normalized product-search data so the
     * rest of Sentinq remains independent of Gemini-specific
     * grounding behavior.
     */
    @Override
    public ProductSearchResult searchProducts(
            Goal goal,
            ConsumerPreferences preferences
    ) {
        validateSearchInputs(
                goal,
                preferences
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

        GenerateContentResponse response =
                geminiClient.models.generateContent(
                        MODEL,
                        buildProductSearchPrompt(
                                goal,
                                preferences
                        ),
                        config
                );

        String responseText =
                response.text();

        return deserializeSearchResult(
                responseText
        );
    }

    /**
     * Builds the product-discovery prompt sent to Gemini.
     *
     * The prompt includes the consumer's governed shopping
     * preferences so merchant discovery is influenced by the
     * same Sentinq context used by other reasoning providers.
     */
    private String buildProductSearchPrompt(
            Goal goal,
            ConsumerPreferences preferences
    ) {
        return """
            You are performing real-time product discovery for Sentinq.

            Use Google Search to find real, currently listed products.

            Return only a valid JSON object.
            Do not include markdown, commentary, or a code fence.

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
            - Prefer explicitly preferred merchants when relevant.
            - Never return merchants listed as avoided.
            - Use only real product pages found during search.
            - Do not invent merchants, URLs, prices, or inventory.
            - If a listed price cannot be verified, omit that offer.
            - Shipping, taxes, and final delivery feasibility will
              be resolved separately by Sentinq.
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

    /**
     * Converts Gemini's product-search JSON into Sentinq's
     * provider-neutral ProductSearchResult domain object.
     */
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
                    "Gemini returned invalid product-search JSON: "
                            + responseText,
                    exception
            );
        }
    }

    /**
     * Validates that product discovery has the domain context
     * required before a provider call is made.
     */
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

}