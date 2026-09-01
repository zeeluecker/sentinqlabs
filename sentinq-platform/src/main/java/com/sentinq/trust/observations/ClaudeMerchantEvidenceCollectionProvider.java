package com.sentinq.trust.observations;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.WebSearchTool20250305;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinq.resolution.CandidateOffer;
import com.sentinq.trust.TrustContext;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class ClaudeMerchantEvidenceCollectionProvider
        implements MerchantEvidenceCollectionProvider {

    private static final String MODEL =
            Model.CLAUDE_SONNET_5.toString();

    private final AnthropicClient anthropicClient;
    private final ObjectMapper objectMapper;

    public ClaudeMerchantEvidenceCollectionProvider(
            ObjectMapper objectMapper
    ) {
        this.anthropicClient =
                AnthropicOkHttpClient.fromEnv();

        this.objectMapper =
                objectMapper;
    }

    @Override
    public String getProviderId() {
        return "claude";
    }

    @Override
    public MerchantEvidenceCollectionDecision collectMerchantEvidence(
            String merchantId,
            String merchantName,
            CandidateOffer offer,
            TrustContext context
    ) {
        validateInputs(
                merchantId,
                merchantName,
                offer,
                context
        );

        WebSearchTool20250305 webSearchTool =
                WebSearchTool20250305.builder()
                        .maxUses(4L)
                        .build();

        MessageCreateParams params =
                MessageCreateParams.builder()
                        .model(MODEL)
                        .maxTokens(8192L)
                        .addTool(webSearchTool)
                        .addUserMessage(
                                buildPrompt(
                                        merchantId,
                                        merchantName,
                                        offer,
                                        context
                                )
                        )
                        .build();

        Message response =
                anthropicClient.messages()
                        .create(params);

        System.out.println(
                "Claude observation stop reason: "
                        + response.stopReason()
        );

        String responseText =
                extractText(response);
        System.out.println(
                "Claude merchant evidence response received. blocks="
                        + response.content().size()
        );

        System.out.println(
                "Claude merchant evidence extracted text length="
                        + responseText.length()
        );
        System.out.println(
                "Claude merchant evidence text prefix: "
                        + responseText.substring(0, Math.min(500, responseText.length()))
        );
        try {
            return deserialize(
                    responseText
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private String buildPrompt(
            String merchantId,
            String merchantName,
            CandidateOffer offer,
            TrustContext context
    ) {
        return """
        You are collecting representative raw trust evidence
        for a Sentinq merchant Trust Map.

        This step is OBSERVATION, not trust assessment.

        Return only a valid JSON object.
        Do not include markdown, commentary, citations outside
        the JSON, or a code fence.
        Do not include introductory text
        Do not include any text before or after the JSON object.

        Merchant:
        - Merchant ID: %s
        - Merchant name: %s

        Candidate offer:
        - Product: %s
        - Price cents: %s

        Consumer context:
        - Goal: %s
        - Product category: %s
        - Product type: %s
        - Product attributes: %s
        - Transaction value: %s
        - Delivery urgency: %s
        - Merchant familiarity: %s
        - Important TrustDimensions: %s

        Search the public web for representative evidence relevant
        to the Important TrustDimensions for this purchase.

        The top-level JSON object must contain exactly one field:
        "observations".

        Do not include merchantId, merchantName, candidateOffer,
        observationType, notes, or any other top-level field.

        Use exactly this JSON structure:

        {
          "observations": [
            {
              "proposedDimension": "PRODUCT_QUALITY",
              "claim": "string",
              "rawContent": "string",
              "sourceName": "string",
              "sourceUrl": "https://example.com",
              "sourceType": "MERCHANT_FIRST_PARTY",
              "sourceIndependence": "FIRST_PARTY",
              "sourceExpertise": "UNKNOWN",
              "channel": "DIRECT_MERCHANT",
              "evidenceHorizon": "PRE_PURCHASE"
            }
          ]
        }

        Field names must match this structure exactly.

        Do not rename fields or introduce aliases.

        Do not return these field names:
        - trustDimension
        - summary
        - evidence
        - source
        - independence
        - expertise
        - horizon

        Use these field names instead:
        - proposedDimension
        - claim
        - rawContent
        - sourceName
        - sourceUrl
        - sourceType
        - sourceIndependence
        - sourceExpertise
        - channel
        - evidenceHorizon

        sourceUrl must contain only the plain source URL.

        Do not return sourceUrl as Markdown.
        For example, return:

        "sourceUrl": "https://example.com/page"

        Do not return:

        "sourceUrl": "[https://example.com/page](https://example.com/page)"

        rawContent should contain a concise excerpt or description
        of the source evidence relevant to the claim.

        Do not include provider-specific citation markup such as
        <cite> tags inside rawContent.

        Observation rules:

        1. Collect observations only. Do not decide whether the
           merchant is trustworthy.

        2. Do not recommend or reject the merchant.

        3. Do not assign an overall merchant trust score.

        4. Every observation must be attributable to a real source
           found through web search.

        5. Preserve the distinction between:
           - merchant claims and policies
           - customer experiences
           - independent community evidence
           - expert or specialist evidence

        6. Merchant first-party sources establish what the merchant
           claims, promises, describes, or guarantees. They are not
           independent proof of merchant performance.

        7. Customer reviews hosted by the merchant remain customer
           experience evidence. Their channel is DIRECT_MERCHANT.

        8. Prefer representative evidence diversity over evidence
           volume.

        9. Avoid repetitive observations that establish substantially
           the same thing.

        10. Collect evidence only for the Important TrustDimensions
            supplied above.

        11. Each observation must propose the TrustDimension it most
            directly informs.

        12. Preserve ambiguity. Do not resolve ambiguous evidence
            during observation.

        13. Do not invent sources, URLs, reviews, policies, facts,
            or source content.

        14. If reliable evidence cannot be found for a dimension,
            do not manufacture evidence.

        15. Normally return approximately 5-8 high-information,
            representative observations total rather than performing
            exhaustive research.

        Use only Sentinq's existing enum values.

        EvidenceSourceType:
        - MERCHANT_FIRST_PARTY
        - CUSTOMER_REVIEW
        - DOMAIN_COMMUNITY
        - EXPERT_OR_SPECIALIST
        - INDEPENDENT_REVIEW_PLATFORM
        - COMPLAINT_BODY
        - REGULATORY_SOURCE
        - CONSUMER_HISTORY

        EvidenceIndependence:
        - FIRST_PARTY
        - THIRD_PARTY
        - INDEPENDENT
        - UNKNOWN

        EvidenceExpertise:
        - GENERAL_CONSUMER
        - EXPERIENCED_CONSUMER
        - DOMAIN_ENTHUSIAST
        - DOMAIN_EXPERT
        - UNKNOWN

        EvidenceChannel:
        - DIRECT_MERCHANT
        - THIRD_PARTY_RETAILER
        - MARKETPLACE
        - UNKNOWN

        EvidenceHorizon:
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

    private MerchantEvidenceCollectionDecision deserialize(String responseText)
            throws JsonProcessingException {

        String json = extractJson(responseText);

        return objectMapper.readValue(
                json,
                MerchantEvidenceCollectionDecision.class
        );
    }

    private String extractJson(String responseText) {
        if (responseText == null || responseText.isBlank()) {
            throw new IllegalArgumentException(
                    "Claude response text is required."
            );
        }

        String text = responseText.trim();

        // Claude may return:
        //
        // Based on the gathered evidence...
        // ```json
        // { ... }
        // ```
        int jsonFenceStart = text.indexOf("```json");

        if (jsonFenceStart >= 0) {
            int jsonStart =
                    jsonFenceStart + "```json".length();

            int fenceEnd =
                    text.indexOf("```", jsonStart);

            if (fenceEnd > jsonStart) {
                return text.substring(
                        jsonStart,
                        fenceEnd
                ).trim();
            }
        }

        // Tolerate an unlabeled Markdown code fence too.
        int fenceStart = text.indexOf("```");

        if (fenceStart >= 0) {
            int jsonStart =
                    fenceStart + "```".length();

            int fenceEnd =
                    text.indexOf("```", jsonStart);

            if (fenceEnd > jsonStart) {
                return text.substring(
                        jsonStart,
                        fenceEnd
                ).trim();
            }
        }

        // Claude followed instructions and returned raw JSON.
        return text;
    }

    private String extractText(Message response) {
        return response.content().stream()
                .filter(contentBlock -> contentBlock.isText())
                .map(contentBlock -> contentBlock.asText().text())
                .collect(Collectors.joining("\n"));
    }

    private String removeMarkdownCodeFence(
            String responseText
    ) {
        if (responseText == null
                || responseText.isBlank()) {
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

    private void validateInputs(
            String merchantId,
            String merchantName,
            CandidateOffer offer,
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

        if (offer == null) {
            throw new IllegalArgumentException(
                    "Candidate offer is required."
            );
        }

        if (context == null) {
            throw new IllegalArgumentException(
                    "Trust context is required."
            );
        }
    }
}
