package com.sentinq.ai.provider;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.StructuredResponse;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.sentinq.ai.InterpretedShoppingGoal;
import com.sentinq.resolution.CandidateOffer;
import com.sentinq.trust.TrustContext;
import com.sentinq.trust.TrustEvidence;
import com.sentinq.trust.interpretation.EvidenceInterpretationDecision;
import com.sentinq.trust.interpretation.EvidenceInterpretationProvider;
import com.sentinq.trust.observations.MerchantEvidenceCollectionProvider;
import org.springframework.stereotype.Component;
import com.openai.models.responses.WebSearchTool;
import com.sentinq.ai.ProductSearchResult;
import com.sentinq.goal.Goal;
import com.sentinq.preference.ConsumerPreferences;
import com.sentinq.trust.ContextRequirement;
import com.sentinq.trust.research.ContextResearchDecision;
import com.sentinq.trust.research.ContextResearchProvider;
import java.util.List;
import com.sentinq.trust.ContextFinding;
import java.util.List;
import com.sentinq.trust.observations.MerchantEvidenceCollectionDecision;
import com.sentinq.trust.observations.MerchantEvidenceCollectionProvider;

@Component
public class OpenAiProvider
        implements LlmProvider,
        ProductSearchProvider,
        EvidenceInterpretationProvider,
        ContextResearchProvider,
        MerchantEvidenceCollectionProvider {

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

        if (researchedEvidence == null ||
                researchedEvidence.isEmpty()) {
            throw new IllegalArgumentException(
                    "Researched evidence is required for reinterpretation."
            );
        }

        if (contextFindings == null ||
                contextFindings.isEmpty()) {
            throw new IllegalArgumentException(
                    "Context findings are required for reinterpretation."
            );
        }

        StructuredResponseCreateParams<EvidenceInterpretationDecision> params =
                ResponseCreateParams.builder()
                        .model(MODEL)
                        .input(
                                buildEvidenceReinterpretationPrompt(
                                        evidence,
                                        context,
                                        researchedEvidence,
                                        contextFindings
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
                                "OpenAI returned no structured evidence reinterpretation."
                        )
                );
    }

    @Override
    public MerchantEvidenceCollectionDecision collectMerchantEvidence(
            String merchantId,
            String merchantName,
            CandidateOffer offer,
            TrustContext context
    ) {
        StructuredResponseCreateParams<MerchantEvidenceCollectionDecision> params =
                ResponseCreateParams.builder()
                        .model(MODEL)
                        .input(
                                buildMerchantEvidenceCollectionPrompt(
                                        merchantId,
                                        merchantName,
                                        offer,
                                        context
                                )
                        )
                        .addTool(
                                WebSearchTool.builder().type(WebSearchTool.Type.WEB_SEARCH)
                                        .build()
                        )
                        .text(
                                MerchantEvidenceCollectionDecision.class
                        )
                        .build();

        StructuredResponse<MerchantEvidenceCollectionDecision> response =
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
                                "OpenAI returned no structured merchant evidence collection."
                        )
                );
    }

    private String buildMerchantEvidenceCollectionPrompt(
            String merchantId,
            String merchantName,
            CandidateOffer offer,
            TrustContext context
    ) {
        return """
            You are collecting raw trust evidence for Sentinq.

            Your task is OBSERVATION, not trust assessment.

            Merchant being evaluated:
            - Merchant ID: %s
            - Merchant name: %s

            Candidate offer:
            - Product: %s
            - Price cents: %s

            Consumer trust context:
            - Goal: %s
            - Product category: %s
            - Product type: %s
            - Product attributes: %s
            - Transaction value: %s
            - Delivery urgency: %s
            - Merchant familiarity: %s
            - Important trust dimensions: %s

            Search the web for relevant evidence that could help Sentinq
            understand this merchant for this specific purchase.

            Evidence collection rules:

            1. Collect observations. Do not decide whether the merchant
               is trustworthy.

            2. Do not recommend or reject the merchant.

            3. Do not assign a TrustSignal or overall trust score.

            4. Every observation must be attributable to a real source
               found during web research.

            5. Preserve the difference between:
               - what the merchant claims or promises,
               - what customers report experiencing,
               - what independent communities report,
               - and what experts or specialists establish.

            6. Merchant first-party sources may establish the merchant's
               own claims, promises, policies, product descriptions,
               shipping practices, guarantees, or return terms.

            7. Customer reviews hosted on the merchant's website may be
               collected as customer experience evidence, but the channel
               must remain DIRECT_MERCHANT. Do not treat merchant-hosted
               customer reviews as merchant claims.

            8. Where relevant, look for independent customer or community
               evidence outside the merchant's own website.

            9. Where relevant, look for expert or specialist evidence that
               helps establish category-specific expectations.

            10. Do not collect evidence merely to fill every source category.
                Collect evidence because it is relevant to one of the
                important TrustDimensions for this purchase.

            11. Prefer a small, useful, diverse evidence set over a large
                collection of repetitive evidence.

            12. When several sources repeat substantially the same claim,
                avoid returning unnecessary duplicates.

            13. Each observation must propose the TrustDimension to which
                the evidence appears most relevant. This is only a proposed
                dimension; interpretation happens later.

            14. claim should be a concise description of what was observed.

            15. rawContent should preserve enough of the underlying source
                content for Sentinq to interpret the evidence later.

            16. Do not interpret ambiguous evidence during collection.
                Preserve the ambiguity for the interpretation layer.

            17. Do not invent facts, reviews, source names, URLs, policies,
                customer experiences, or source content.

            18. If reliable evidence cannot be found for a particular
                TrustDimension, do not manufacture evidence to fill the gap.

            Return only the structured MerchantEvidenceCollectionDecision.
            """
                .formatted(
                        merchantId,
                        merchantName,
                        offer.getProductName(),
                        offer.getProductPriceCents(),

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
                
            Return only the structured EvidenceInterpretationDecision.

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
                
            20. For every finding, classify the source using Sentinq's source model.
                
                    Allowed EvidenceSourceType values:
                    - MERCHANT_FIRST_PARTY
                    - CUSTOMER_REVIEW
                    - DOMAIN_COMMUNITY
                    - EXPERT_OR_SPECIALIST
                    - INDEPENDENT_REVIEW_PLATFORM
                    - COMPLAINT_BODY
                    - REGULATORY_SOURCE
                    - CONSUMER_HISTORY
                
                    Allowed EvidenceIndependence values:
                    - FIRST_PARTY
                    - THIRD_PARTY
                    - INDEPENDENT
                    - UNKNOWN
                
                    Allowed EvidenceExpertise values:
                    - GENERAL_CONSUMER
                    - EXPERIENCED_CONSUMER
                    - DOMAIN_ENTHUSIAST
                    - DOMAIN_EXPERT
                    - UNKNOWN
                
                    Allowed EvidenceChannel values:
                    - DIRECT_MERCHANT
                    - THIRD_PARTY_RETAILER
                    - MARKETPLACE
                    - UNKNOWN
                
                    Allowed EvidenceHorizon values:
                    - PRE_PURCHASE
                    - IMMEDIATE
                    - SHORT_TERM
                    - MEDIUM_TERM
                    - LONG_TERM
                    - UNKNOWN
                
                    Rules for classification:
                    - Merchant-owned product pages, help centers, and policy pages are
                      MERCHANT_FIRST_PARTY and FIRST_PARTY.
                    - Independent expert or educational sources may be
                      EXPERT_OR_SPECIALIST and INDEPENDENT when appropriate.
                    - Community discussions should be DOMAIN_COMMUNITY.
                    - Review platforms should be classified based on what the finding
                      actually comes from; do not treat a merchant response on a review
                      site as independent evidence of the merchant's performance.
                    - Use UNKNOWN when the source does not support a stronger classification.
                    - Do not guess expertise.
                
                EvidenceHorizon describes what time period the finding informs,
                not when the webpage was published.
                
                For example:
                - arrival condition → IMMEDIATE
                - first few weeks → SHORT_TERM
                - later establishment/growth → MEDIUM_TERM or LONG_TERM
                - if no time horizon is supported → UNKNOWN
                
               For customer reviews, independence describes the reviewer,
                    not the website hosting the review.
                
                    A customer review displayed on a merchant's own site should normally be:
                    - sourceType = CUSTOMER_REVIEW
                    - sourceIndependence = THIRD_PARTY
                    - channel = DIRECT_MERCHANT

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