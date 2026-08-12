package com.sentinq.ai.provider;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.StructuredResponse;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.sentinq.ai.InterpretedShoppingGoal;
import com.sentinq.trust.TrustContext;
import com.sentinq.trust.TrustEvidence;
import com.sentinq.trust.interpretation.EvidenceInterpretationDecision;
import com.sentinq.trust.interpretation.EvidenceInterpretationProvider;
import org.springframework.stereotype.Component;
import com.openai.models.responses.WebSearchTool;
import com.sentinq.ai.ProductSearchResult;
import com.sentinq.goal.Goal;
import com.sentinq.preference.ConsumerPreferences;
import com.sentinq.trust.ContextRequirement;
import com.sentinq.trust.research.ContextResearchDecision;
import com.sentinq.trust.research.ContextResearchProvider;
import java.util.List;

@Component
public class OpenAiProvider
        implements LlmProvider,
        ProductSearchProvider,
        EvidenceInterpretationProvider,
        ContextResearchProvider{

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
    /**
     * Interprets a piece of raw trust evidence within the consumer's
     * current shopping context.
     *
     * This operation performs interpretation only. It does not search
     * for additional evidence or resolve missing context. When the
     * evidence cannot support a trustworthy conclusion on its own,
     * the provider should return CONTEXT_REQUIRED and identify the
     * context that Sentinq should research next.
     */
    @Override
    public EvidenceInterpretationDecision interpretEvidence(
            TrustEvidence evidence,
            TrustContext context
    ) {
        validateEvidenceInterpretationInputs(
                evidence,
                context
        );

        StructuredResponseCreateParams<EvidenceInterpretationDecision> params =
                ResponseCreateParams.builder()
                        .model(MODEL)
                        .input(
                                buildEvidenceInterpretationPrompt(
                                        evidence,
                                        context
                                )
                        )
                        .text(
                                EvidenceInterpretationDecision.class
                        )
                        .build();

        StructuredResponse<EvidenceInterpretationDecision> response =
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
                                "OpenAI returned no structured evidence interpretation."
                        )
                );
    }

    @Override
    public ContextResearchDecision researchContext(
            String merchantId,
            String merchantName,
            TrustEvidence originalEvidence,
            TrustContext context,
            List<ContextRequirement> requirements
    ) {
        validateContextResearchInputs(
                originalEvidence,
                context,
                requirements
        );

        StructuredResponseCreateParams<ContextResearchDecision> params =
                ResponseCreateParams.builder()
                        .model(MODEL)
                        .input(
                                buildContextResearchPrompt(
                                        merchantId,
                                        merchantName,
                                        originalEvidence,
                                        context,
                                        requirements
                                )
                        )
                        .addTool(
                                WebSearchTool.builder()
                                        .type(
                                                WebSearchTool.Type.WEB_SEARCH
                                        )
                                        .build()
                        )
                        .text(
                                ContextResearchDecision.class
                        )
                        .build();

        StructuredResponse<ContextResearchDecision> response =
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
                                "OpenAI returned no structured context research."
                        )
                );
    }

    private String buildContextResearchPrompt(
            String merchantId,
            String merchantName,
            TrustEvidence originalEvidence,
            TrustContext context,
            List<ContextRequirement> requirements
    ) {
        return """
            You are researching missing context for Sentinq Trust Maps.

            Sentinq has encountered trust evidence that cannot yet be
            interpreted reliably. Another reasoning step identified specific
            context requirements that must be investigated.
            
            Merchant being evaluated:
                       - Merchant ID: %s
                       - Merchant name: %s
  
            Your task is to search the public web for reliable evidence that
            helps resolve those requirements.

            Original trust evidence:
            - Evidence ID: %s
            - Proposed trust dimension: %s
            - Raw claim: %s
            - Source excerpt: %s
            - Evidence horizon: %s

            Shopping context:
            - Goal: %s
            - Product category: %s
            - Product type: %s
            - Product attributes: %s
            - Transaction value: %s
            - Delivery urgency: %s
            - Merchant familiarity: %s
            - Important trust dimensions: %s

            Context requirements to investigate:
            %s

            Research rules:

            1. Search the public web for evidence relevant to the supplied
               context requirements.

            2. Research the questions. Do not assume the original customer's
               claim is either correct or incorrect.

            3. Prefer authoritative or knowledgeable sources appropriate to
               the question being investigated.
               
            4. Distinguish merchant-specific facts from general category knowledge.
                
            5. For MERCHANT_PROMISE findings, use only evidence attributable
                   to the merchant being evaluated.
                
            6. Do not use another merchant's advertised specification as evidence
                   of what this merchant promised.
                
            7. Other merchants may be used only to establish CATEGORY_NORM or
                   other genuinely cross-merchant contextual evidence.
                
            8. Clearly distinguish merchant-specific findings from category norms.

            9. Do not manufacture facts, sources, URLs, or quotations.

            10. Every finding must be supported by a real source discovered
               through web search.

            11. sourceUrl must contain the URL of the source supporting the
               finding.

            12. sourceName must identify that source clearly.

            13. sourceExcerpt should contain only a concise excerpt or
               description of the evidence relevant to the finding.

            14. The ContextType of each finding must correspond to the
                question the finding helps resolve.

            15. Use only these ContextType values:
                - CONSUMER_OBJECTIVE
                - TRANSACTION_CHANNEL
                - MERCHANT_PROMISE
                - TIME_HORIZON
                - PRODUCT_ATTRIBUTE
                - CATEGORY_NORM
                - COMMUNITY_NORM
                - USAGE_CONDITION

            16. Do not make an overall merchant trust judgment.

            17. Do not assign a TrustSignal.

            18. Do not reinterpret the original evidence yet. Your job is
                only to gather contextual evidence.

            19. Confidence represents confidence that the finding is
                accurately supported by the cited source.

            Return only the structured ContextResearchDecision.

            """.formatted(
                merchantId,
                merchantName,
                originalEvidence.evidenceId(),
                originalEvidence.proposedDimension(),
                originalEvidence.rawClaim(),
                originalEvidence.sourceExcerpt(),
                originalEvidence.evidenceHorizon(),

                context.goal(),
                context.productCategory(),
                context.productType(),
                context.productAttributes(),
                context.transactionValue(),
                context.deliveryUrgency(),
                context.merchantFamiliarity(),
                context.importantDimensions(),

                requirements
        );
    }

    private void validateContextResearchInputs(
            TrustEvidence originalEvidence,
            TrustContext context,
            List<ContextRequirement> requirements
    ) {
        if (originalEvidence == null) {
            throw new IllegalArgumentException(
                    "Original trust evidence is required for context research."
            );
        }

        if (context == null) {
            throw new IllegalArgumentException(
                    "Trust context is required for context research."
            );
        }

        if (requirements == null ||
                requirements.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one context requirement is required for research."
            );
        }
    }


    /**
     * Builds the reasoning prompt used to interpret raw trust evidence.
     *
     * The prompt explicitly prevents the reasoning provider from
     * collapsing consumer dissatisfaction or ambiguous observations
     * directly into merchant trust failures.
     */
    private String buildEvidenceInterpretationPrompt(
            TrustEvidence evidence,
            TrustContext context
    ) {
        return """
            You are interpreting trust evidence for Sentinq Trust Maps.

            Your job is to determine what this evidence can reasonably
            tell us about merchant trust in the supplied shopping context.

            Raw evidence:
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
               A customer's dissatisfaction is evidence, but is not
               automatically evidence of merchant untrustworthiness.

            2. Do not assume that a negative-sounding observation is a
               negative trust signal.

            3. Interpret the evidence in the context of the consumer's
               goal, product, transaction, and relevant trust dimension.

            4. Distinguish immediate perception from longer-term outcomes.
               An observation at one time horizon must not automatically
               establish conclusions about another time horizon.

            5. Distinguish the merchant from the transaction channel.
               Evidence from a marketplace or third-party retailer must
               not automatically be attributed to the merchant itself.

            6. Do not manufacture missing facts.

            7. If important context is missing, return:
               status = CONTEXT_REQUIRED

               Identify the specific missing context through
               contextRequirements.

            8. When context is required and no reliable trust inference
               can yet be made, use:
               signal = NO_INFERENCE

            9. apparentMeaning should describe what the evidence appears
               to suggest before additional context is resolved.

            10. contextualMeaning should be null when the required context
                has not yet been established.

            11. supportingEvidenceIds and contradictingEvidenceIds should
                contain only evidence IDs actually supplied to you.
                Do not invent evidence IDs.

            12. Confidence represents confidence in this interpretation,
                not an overall merchant trust score.

            13. Do not produce an overall merchant trust score or merchant
                recommendation.

            14. Use NOT_RELEVANT when the evidence does not meaningfully
                inform the proposed trust dimension in this context.

            15. Use INSUFFICIENT_EVIDENCE when the evidence is too weak
                to support meaningful interpretation and additional
                context would not reasonably resolve that weakness.

            Return only the structured EvidenceInterpretationDecision.

            """.formatted(
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

    /**
     * Validates the inputs required for contextual trust-evidence
     * interpretation before invoking the reasoning provider.
     */
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