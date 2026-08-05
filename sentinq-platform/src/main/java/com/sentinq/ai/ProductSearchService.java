package com.sentinq.ai;

import com.openai.client.OpenAIClient;
import com.openai.models.ChatModel;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.StructuredResponse;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.openai.models.responses.WebSearchTool;
import com.sentinq.goal.Goal;
import com.sentinq.preference.ConsumerPreferences;
import org.springframework.stereotype.Service;

@Service
public class ProductSearchService {

    private final OpenAIClient openAIClient;

    public ProductSearchService(
            OpenAIClient openAIClient
    ) {
        this.openAIClient = openAIClient;
    }

    public ProductSearchResult search(
            Goal goal,
            ConsumerPreferences preferences
    ) {
        StructuredResponseCreateParams<ProductSearchResult> params =
                ResponseCreateParams.builder()
                        .model(ChatModel.GPT_5_2)
                        .input(
                                buildSearchPrompt(
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
                                "GPT web search returned no product candidates."
                        )
                );
    }

    private String buildSearchPrompt(
            Goal goal,
            ConsumerPreferences preferences
    ) {
        return """
                You are performing product discovery for Sentinq.

                Search the public web for real, currently listed products.

                Goal:
                - Product: %s
                - Original request: %s
                - Maximum total budget: %d cents
                - Delivery deadline: %s
                - Substitutions allowed: %s

                Consumer merchant preferences:
                - Preferred merchants: %s
                - Avoided merchants: %s
                - Preferred merchant types: %s
                - Minimum preferred fulfillment score: %s
                - Minimum preferred review score: %s
                - Ask before using a new merchant: %s

                Rules:
                - Return no more than 5 offers.
                - Prefer merchants explicitly listed as preferred.
                - Never return merchants listed as avoided.
                - Use only real product pages discovered through web search.
                - Do not invent merchants, URLs, prices, or inventory.
                - productPriceCents must represent the listed item price only.
                - Shipping, tax, and final delivery feasibility will be resolved later by Sentinq.
                - Include a concise matchReason.
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
}