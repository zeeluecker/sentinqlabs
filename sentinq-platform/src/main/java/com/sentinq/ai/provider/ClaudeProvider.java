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

@Component
public class ClaudeProvider
        implements LlmProvider, ProductSearchProvider {

    private final AnthropicClient anthropicClient;
    private final ObjectMapper objectMapper;

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
                        .model(Model.CLAUDE_SONNET_5)
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
                        .model(Model.CLAUDE_SONNET_5)
                        .maxTokens(4096L)
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