package com.sentinq.ai.provider;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.StructuredResponse;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.sentinq.ai.InterpretedShoppingGoal;
import org.springframework.stereotype.Component;
import com.openai.models.responses.WebSearchTool;
import com.sentinq.ai.ProductSearchResult;
import com.sentinq.goal.Goal;
import com.sentinq.preference.ConsumerPreferences;

@Component
public class OpenAiProvider
        implements LlmProvider, ProductSearchProvider {

    private final OpenAIClient openAIClient;
    public static final String MODEL = ChatModel.GPT_5_2.toString();

    public OpenAiProvider() {
        this.openAIClient =
                OpenAIOkHttpClient.fromEnv();
    }

    @Override
    public String getProviderId() {
        return "openai";
    }

    @Override
    public InterpretedShoppingGoal interpretShoppingGoal(
            String rawGoalText
    ) {
        StructuredResponseCreateParams<InterpretedShoppingGoal> params =
                ResponseCreateParams.builder()
                        .model(MODEL)
                        .input(buildPrompt(rawGoalText))
                        .text(InterpretedShoppingGoal.class)
                        .build();

        StructuredResponse<InterpretedShoppingGoal> response =
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
                                "OpenAI returned no structured goal interpretation."
                        )
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

        StructuredResponseCreateParams<ProductSearchResult> params =
                ResponseCreateParams.builder()
                        .model(MODEL)
                        .input(
                                buildProductSearchPrompt(
                                        goal,
                                        preferences
                                )
                        )
                        .addTool(
                                WebSearchTool.builder()
                                        .type(
                                                WebSearchTool.Type.WEB_SEARCH
                                        )
                                        .build()
                        )
                        .text(ProductSearchResult.class)
                        .build();

        StructuredResponse<ProductSearchResult> response =
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
                                "OpenAI returned no product-search results."
                        )
                );
    }

    private String buildProductSearchPrompt(
            Goal goal,
            ConsumerPreferences preferences
    ) {
        return """
            You are performing product discovery for Sentinq.

            Search the public web for real, currently listed products.

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
            - Prefer explicitly preferred merchants where relevant.
            - Do not return avoided merchants.
            - Return only real products discovered through web search.
            - Do not invent merchants, URLs, prices, or availability.
            - productPriceCents must contain the listed item price only.
            - Shipping, tax, and delivery feasibility will be resolved
              separately by Sentinq.
            - Include a concise explanation of why each product matches.

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

    private String buildPrompt(
            String rawGoalText
    ) {
        return """
                You are interpreting a consumer shopping goal for Sentinq.

                Extract only information supported by the request.

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
}