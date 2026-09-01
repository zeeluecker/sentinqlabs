package com.sentinq.ai.provider;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinq.ai.InterpretedShoppingGoal;
import com.sentinq.shopping.GoalFitReasoningProvider;
import com.sentinq.trust.ContextFinding;
import org.springframework.stereotype.Component;
import com.anthropic.models.messages.WebSearchTool20250305;
import com.sentinq.ai.ProductSearchResult;
import com.sentinq.goal.Goal;
import com.sentinq.preference.ConsumerPreferences;
import com.sentinq.trust.TrustContext;
import com.sentinq.trust.TrustEvidence;
import com.sentinq.trust.interpretation.EvidenceInterpretationDecision;
import com.sentinq.trust.interpretation.EvidenceInterpretationProvider;
import com.sentinq.resolution.CandidateOffer;
import com.sentinq.shopping.GoalFitReasoningDecision;
import com.sentinq.shopping.GoalFitReasoningProvider;
import com.sentinq.shopping.RecommendationReasoningDecision;
import com.sentinq.shopping.RecommendationReasoningProvider;
import com.sentinq.shopping.TrustAssessedCandidate;

import java.util.List;


@Component
public class ClaudeProvider
        implements LlmProvider,
        ProductSearchProvider,
        EvidenceInterpretationProvider,
        GoalFitReasoningProvider,
        RecommendationReasoningProvider{

    private final AnthropicClient anthropicClient;
    private final ObjectMapper objectMapper;
    public static final String MODEL =
            Model.CLAUDE_SONNET_5.toString();
    public final long maxtokens = 8192L;

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
                        .maxTokens(maxtokens)
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
                        .maxTokens(maxtokens)
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

    @Override
    public EvidenceInterpretationDecision reinterpretEvidence(
            TrustEvidence evidence,
            TrustContext context,
            List<TrustEvidence> researchedEvidence,
            List<ContextFinding> contextFindings
    ) {
        validateEvidenceInterpretationInputs(
                evidence,
                context
        );

        MessageCreateParams params =
                MessageCreateParams.builder()
                        .model(MODEL)
                        .maxTokens(maxtokens)
                        .addUserMessage(
                                buildEvidenceReinterpretationPrompt(
                                        evidence,
                                        context,
                                        researchedEvidence,
                                        contextFindings
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
                cleanJsonResponse(
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


    private String buildEvidenceReinterpretationPrompt(
            TrustEvidence evidence,
            TrustContext context,
            List<TrustEvidence> researchedEvidence,
            List<ContextFinding> contextFindings
    ) {
        return """
            You are reinterpreting trust evidence for Sentinq Trust Maps.

            An earlier interpretation found that the original evidence
            required more context.

            Sentinq then researched that missing context and created new,
            source-traceable TrustEvidence and ContextFinding records.

            Your job is to reinterpret the ORIGINAL evidence using the newly
            researched context.

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

            Do not invent enum values.

            Use exactly this structure:

            {
              "status": "CONTEXT_RESOLVED",
              "apparentMeaning": "string",
              "contextRequirements": [],
              "contextualMeaning": "string",
              "signal": "NO_INFERENCE",
              "confidence": 0.0,
              "supportingEvidenceIds": [],
              "contradictingEvidenceIds": []
            }

            Original evidence:
            - Evidence ID: %s
            - Proposed trust dimension: %s
            - Raw claim: %s
            - Source excerpt: %s
            - Source type: %s
            - Source independence: %s
            - Source expertise: %s
            - Channel: %s
            - Evidence horizon: %s

            Consumer shopping context:
            - Goal: %s
            - Product category: %s
            - Product type: %s
            - Product attributes: %s
            - Transaction value: %s
            - Delivery urgency: %s
            - Merchant familiarity: %s
            - Important trust dimensions: %s

            Researched evidence:
            %s

            Context findings:
            %s

            Reinterpretation rules:

            1. Reinterpret the original evidence in light of the researched
               context.

            2. Do not change or rewrite the original evidence.

            3. Use the researched evidence only for what its source can
               reasonably establish.

            4. Merchant first-party evidence may establish what the merchant
               says, promises, or describes, but is not independent proof that
               the merchant consistently performs as claimed.

            5. Independent expert evidence may establish category norms or
               domain knowledge when relevant.

            6. Community or customer evidence may describe experiences or
               patterns, but should not automatically override stronger or
               more direct evidence.
            7. Missing information may remain unknown.

            8. A ContextRequirement is required only if the interpretation
            cannot reasonably be completed without it.

            9. Do not create or retain ContextRequirements merely because
            additional information could improve confidence.

            10. After considering the researched evidence, decide whether any
            remaining unknown could materially change the interpretation
            of the ORIGINAL evidence for the CURRENT proposed TrustDimension.

            11. If a remaining unknown could materially change the interpretation:
            - use CONTEXT_REQUIRED
            - return only the ContextRequirements that are necessary to resolve it.

            12. If the remaining unknowns are unlikely to materially change the
            interpretation:
            - use CONTEXT_RESOLVED
            - return an empty contextRequirements list.

            13. CONTEXT_RESOLVED does not mean perfect or complete knowledge.
            It means sufficient context exists to make a reasonable,
            bounded interpretation of the original evidence for the CURRENT
            proposed TrustDimension.

            14. Do not keep a ContextRequirement merely because it would help
            assess a different TrustDimension.

            For example, if the current proposed dimension is PRODUCT_QUALITY,
            an unanswered question about long-term durability should not
            prevent PRODUCT_QUALITY from being context-resolved unless that
            information is actually necessary to understand product quality.
            
            15. contextualMeaning should explain how the researched context
                changes, confirms, limits, or clarifies the apparent meaning
                of the original evidence.
            
            16. supportingEvidenceIds may contain only evidence IDs supplied
                in the researched evidence.
            
            17. contradictingEvidenceIds may contain only evidence IDs supplied
                in the researched evidence.
            
            18. Do not invent evidence IDs.
            
            19. signal should reflect what the ORIGINAL evidence now supports
                after context is considered.
            
            20. Do not create an overall merchant trust score or merchant
                recommendation.
            
            21. confidence represents confidence in this contextual
                interpretation, not confidence in the merchant overall.            
                
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

                context.goal(),
                context.productCategory(),
                context.productType(),
                context.productAttributes(),
                context.transactionValue(),
                context.deliveryUrgency(),
                context.merchantFamiliarity(),
                context.importantDimensions(),

                researchedEvidence,
                contextFindings
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
                        .model(MODEL)
                        .maxTokens(maxtokens)
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

       /* System.out.println(
                "Claude response content: "
                        + response.content()
        );*/


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
                cleanJsonResponse(
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
                cleanJsonResponse(
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

    @Override
    public GoalFitReasoningDecision rank(
            Goal goal,
            List<CandidateOffer> candidates
    ) {
        validateGoalFitInputs(
                goal,
                candidates
        );

        MessageCreateParams params =
                MessageCreateParams.builder()
                        .model(MODEL)
                        .maxTokens(maxtokens)
                        .addUserMessage(
                                buildGoalFitPrompt(
                                        goal,
                                        candidates
                                )
                        )
                        .build();

        Message response =
                anthropicClient.messages()
                        .create(params);

        String responseText =
                extractText(response);

        return deserializeGoalFitDecision(
                responseText
        );
    }

    private GoalFitReasoningDecision deserializeGoalFitDecision(
            String responseText
    ) {
        String cleanedResponse =
                cleanJsonResponse(
                        responseText
                );

        try {
            return objectMapper.readValue(
                    cleanedResponse,
                    GoalFitReasoningDecision.class
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Claude returned invalid goal-fit reasoning JSON: "
                            + responseText,
                    exception
            );
        }
    }

    private String buildGoalFitPrompt(
            Goal goal,
            List<CandidateOffer> candidates
    ) {
        return """
        You are performing lightweight goal-fit reasoning
        for Sentinq shopping orchestration.

        Your task is to rank the supplied candidate offers
        according to how well each candidate appears to
        accomplish the consumer's shopping goal.

        Return only a valid JSON object.
        Do not include markdown, commentary, or a code fence.

        Use exactly this structure:

                {
                   "candidates": [
                     {
                       "offer": {
                         "offerId": "string",
                         "merchantId": "string",
                         "merchantName": "string",
                         "productName": "string",
                         "productDescription": "string",
                         "discoveryMatchReason": "string",
                         "productPriceCents": 0,
                         "fulfillmentScore": 0,
                         "reviewScore": 0
                       },
                       "rank": 1,
                       "reasoning": "string"
                     }
                   ]
                 }

        Consumer goal:
        - Original request: %s
        - Product: %s

        Candidate offers:
        %s

        Rules:

        1. Evaluate only the candidates supplied to you.

        2. Do not search for additional products or evidence.

        3. Preserve the consumer's original objective and context.
           The original request is the primary source for understanding
           what the consumer is trying to accomplish.

        4. Do not reduce the consumer's goal to only hard constraints.
           Consider the qualitative objective, intended use, appearance,
           experience, compatibility, and other strong preferences
           expressed in the original request.

        5. Hard screening has already removed candidates that can be cheaply
           proven to be non-starters. Do not repeat that filtering step.

        6. Rank candidates based on apparent product fit to the consumer's
           original objective, context, and stated preferences.

        7. Do not evaluate, compare, or rank candidates based on merchant
           reputation, merchant reliability, customer reviews, return
           experience, or other merchant trust signals. Merchant trust is
           evaluated separately after this shortlist is produced.

        8. Do not infer or investigate current shipping cost, tax, inventory
           availability, or whether a delivery date can actually be met.
           If those facts are not explicitly present in the supplied candidate
           information, leave them unknown.

        9. Do not decide whether a purchase may be executed on the consumer's
           behalf. Do not reason about permissions, consent, spending authority,
           or whether the agent is allowed to complete the transaction.

        10. Rank every supplied candidate exactly once.

        11. Rank 1 means the strongest apparent fit for this
            consumer goal.

        12. Ranks must be unique and sequential:
            1, 2, 3, and so on.

        13. reasoning should briefly explain why the candidate occupies
            that position relative to the consumer's stated objective.

        14. Do not invent product attributes that are not present
            in the supplied candidate information.

        15. offerId must exactly match the Offer ID of the corresponding
            supplied candidate. Do not create or modify offer IDs.

        Return only the JSON object.
        """
                .formatted(
                        goal.getOriginalRequest(),
                        goal.getProductName(),
                        formatGoalFitCandidates(candidates)
                );
    }

    private String formatGoalFitCandidates(
            List<CandidateOffer> candidates
    ) {
        StringBuilder builder =
                new StringBuilder();

        for (int i = 0; i < candidates.size(); i++) {

            CandidateOffer candidate =
                    candidates.get(i);

            builder.append("Candidate ")
                    .append(i + 1)
                    .append(":\n");

            builder.append("- Offer ID: ")
                    .append(candidate.getOfferId())
                    .append("\n");

            builder.append("- Product: ")
                    .append(candidate.getProductName())
                    .append("\n");

            builder.append("- Merchant: ")
                    .append(candidate.getMerchantName())
                    .append("\n");

            builder.append("- Price cents: ")
                    .append(candidate.getProductPriceCents())
                    .append("\n");

            builder.append("- Product description: ")
                    .append(candidate.getProductDescription())
                    .append("\n");

            builder.append("- Discovery match reason: ")
                    .append(candidate.getDiscoveryMatchReason())
                    .append("\n\n");
        }

        return builder.toString();
    }

    private void validateGoalFitInputs(
            Goal goal,
            List<CandidateOffer> candidates
    ) {
        if (goal == null) {
            throw new IllegalArgumentException(
                    "Goal is required for goal-fit reasoning."
            );
        }

        if (candidates == null ||
                candidates.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one candidate is required for goal-fit reasoning."
            );
        }
    }

    @Override
    public RecommendationReasoningDecision recommend(
            Goal goal,
            List<TrustAssessedCandidate> candidates
    ) {
        validateRecommendationInputs(
                goal,
                candidates
        );

        MessageCreateParams params =
                MessageCreateParams.builder()
                        .model(MODEL)
                        .maxTokens(maxtokens)
                        .addUserMessage(
                                buildRecommendationPrompt(
                                        goal,
                                        candidates
                                )
                        )
                        .build();

        Message response =
                anthropicClient.messages()
                        .create(params);

        String responseText =
                extractText(response);

        return deserializeRecommendationDecision(
                responseText
        );
    }

    private RecommendationReasoningDecision deserializeRecommendationDecision(
            String responseText
    ) {
        String cleanedResponse =
                cleanJsonResponse(
                        responseText
                );

        try {
            return objectMapper.readValue(
                    cleanedResponse,
                    RecommendationReasoningDecision.class
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Claude returned invalid recommendation reasoning JSON: "
                            + responseText,
                    exception
            );
        }
    }

    private String buildRecommendationPrompt(
            Goal goal,
            List<TrustAssessedCandidate> candidates
    ) {
        return """
        You are performing recommendation reasoning
        for Sentinq shopping orchestration.

        Your task is to select the strongest overall candidate
        from the supplied contenders.

        Return only a valid JSON object.
        Do not include markdown, commentary, or a code fence.

        Use exactly this structure:

        {
          "selectedOfferId": "string",
          "reasoning": "string"
        }

        Consumer goal:
        - Original request: %s
        - Product: %s

        Trust-assessed goal-fit contenders:
        %s

        Rules:

        1. Evaluate only the candidates supplied to you.
           Do not search for additional products or evidence.

        2. Consider both:
           - how well each product appears to accomplish the
             consumer's original shopping objective, and
           - what the supplied merchant trust assessment says
             about trusting the merchant for this purchase.

        3. Use the supplied goal-fit rank and reasoning as evidence
           of product fit. Do not redo product discovery.

        4. Use only the supplied trust assessment when reasoning
           about merchant trust. Do not invent merchant reputation,
           reviews, reliability, or other trust evidence.

        5. Do not treat goal-fit rank as automatically decisive.
           A lower-ranked product may be preferred when the supplied
           trust assessment provides a material reason to prefer it.

        6. Do not treat the strongest merchant trust assessment as
           automatically decisive. A trustworthy merchant does not
           compensate for a product that poorly accomplishes the
           consumer's objective.

        7. Weigh tradeoffs contextually rather than using a fixed
           numerical formula between goal fit and merchant trust.

        8. Distinguish a material trust concern from mere uncertainty
           or absence of evidence. Do not penalize a candidate simply
           because its trust assessment contains less evidence.

        9. Do not infer or investigate current shipping cost, tax,
           inventory availability, or whether a delivery date can
           actually be met. Those facts are evaluated separately
           after recommendation.

        10. Do not decide whether the purchase may be executed on
            the consumer's behalf. Do not reason about permissions,
            consent, spending authority, or whether the agent is
            authorized to complete the transaction.

        11. selectedOfferId must exactly match the offerId of one
            of the supplied candidates.

        12. Do not create, modify, reconstruct, or substitute an offer.

        13. reasoning should explain the important tradeoff that led
            to the selection, including why the selected candidate is
            preferable to the strongest alternatives.

        Return only the JSON object.
        """
                .formatted(
                        goal.getOriginalRequest(),
                        goal.getProductName(),
                        formatRecommendationCandidates(
                                candidates
                        )
                );
    }

    private String formatRecommendationCandidates(
            List<TrustAssessedCandidate> candidates
    ) {
        return candidates.stream()
                .map(candidate -> {
                    CandidateOffer offer =
                            candidate.goalFitCandidate().offer();

                    return """
                    Candidate:
                    - Offer ID: %s
                    - Merchant: %s
                    - Product: %s
                    - Product description: %s
                    - Product price cents: %s
                    - Goal-fit rank: %s
                    - Goal-fit reasoning: %s
                    - Merchant trust assessment: %s
                    """
                            .formatted(
                                    offer.getOfferId(),
                                    offer.getMerchantName(),
                                    offer.getProductName(),
                                    offer.getProductDescription(),
                                    offer.getProductPriceCents(),
                                    candidate.goalFitCandidate().rank(),
                                    candidate.goalFitCandidate().reasoning(),
                                    candidate.trustAssessment()
                            );
                })
                .collect(
                        java.util.stream.Collectors.joining(
                                "\n"
                        )
                );
    }

    private void validateRecommendationInputs(
            Goal goal,
            List<TrustAssessedCandidate> candidates
    ) {
        if (goal == null) {
            throw new IllegalArgumentException(
                    "Goal is required for recommendation reasoning."
            );
        }

        if (candidates == null ||
                candidates.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one trust-assessed candidate is required for recommendation reasoning."
            );
        }
    }

    private String cleanJsonResponse(
            String responseText
    ) {
        String cleaned =
                removeMarkdownCodeFence(
                        responseText
                );

        // Remove trailing commas before closing JSON objects or arrays.
        return cleaned.replaceAll(
                ",\\s*([}\\]])",
                "$1"
        );
    }
}