package com.sentinq.trust.synthesis;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.StructuredResponse;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.sentinq.trust.TrustContext;
import com.sentinq.trust.TrustEvidence;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OpenAiMerchantEvidenceSynthesisProvider
        implements MerchantEvidenceSynthesisProvider {

    private static final String MODEL = "gpt-5";

    private final OpenAIClient openAIClient;

    public OpenAiMerchantEvidenceSynthesisProvider() {
        this.openAIClient =
                OpenAIOkHttpClient.fromEnv();
    }

    @Override
    public String providerId() {
        return "openai";
    }

    @Override
    public MerchantEvidenceSynthesis synthesizeEvidence(
            String merchantId,
            String merchantName,
            List<TrustEvidence> evidence,
            TrustContext context
    ) {
        validateInputs(
                merchantId,
                merchantName,
                evidence,
                context
        );

        StructuredResponseCreateParams<MerchantEvidenceSynthesis> params =
                ResponseCreateParams.builder()
                        .model(MODEL)
                        .input(
                                buildEvidenceSynthesisPrompt(
                                        merchantId,
                                        merchantName,
                                        evidence,
                                        context
                                )
                        )
                        .text(
                                MerchantEvidenceSynthesis.class
                        )
                        .build();

        StructuredResponse<MerchantEvidenceSynthesis> response =
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
                                "OpenAI returned no structured merchant evidence synthesis."
                        )
                );
    }

    private void validateInputs(
            String merchantId,
            String merchantName,
            List<TrustEvidence> evidence,
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

        if (evidence == null || evidence.isEmpty()) {
            throw new IllegalArgumentException(
                    "Evidence is required."
            );
        }

        if (context == null) {
            throw new IllegalArgumentException(
                    "Trust context is required."
            );
        }
    }

    private String buildEvidenceSynthesisPrompt(
            String merchantId,
            String merchantName,
            List<TrustEvidence> evidence,
            TrustContext context
    ) {
        return """
            You are synthesizing a merchant Trust Map for Sentinq.

            Merchant ID:
            %s

            Merchant name:
            %s

            Consumer context:
            %s

            Evidence:
            %s

            Your task is to understand the evidence landscape
            across all supplied evidence.

            Synthesis rules:

            1. Group related evidence into a small number of meaningful
               recurring themes.

            2. Do not create one theme per evidence item.

            3. Preserve disagreement between sources.
               Conflicting evidence is part of the Trust Map.

            4. Do not treat the number of similar claims as proof
               that those claims are true.

            5. Merchant first-party evidence may establish what the
               merchant says, promises, or describes.
               It must not be treated as independent proof that the
               merchant consistently performs as claimed.

            6. Independent customer, community, complaint, and expert
               evidence may corroborate, contradict, qualify, or add
               context to merchant claims.

            7. Evaluate evidence only in relation to the supplied
               consumer context and relevant TrustDimensions.

            8. Preserve uncertainty when the evidence does not support
               a confident conclusion.

            9. A MaterialTrustQuestion must represent an unresolved
               question whose answer could materially change the trust
               assessment for this consumer or this purchase.

            10. Do not generate additional research questions merely
                because more information could theoretically be useful.

            11. If the evidence is already sufficiently coherent,
                return zero MaterialTrustQuestions.

            12. supportingEvidenceIds may contain only evidence IDs
                present in the supplied evidence.

            13. Theme evidenceIds may contain only evidence IDs
                present in the supplied evidence.

            14. Do not invent evidence IDs.

            15. Do not produce an overall merchant recommendation.

            16. Do not decide whether the consumer should buy.

            17. confidence represents confidence in the synthesis of
                this evidence landscape, not confidence in the merchant
                overall.
                
            18. Create themes only for TrustDimensions identified as important
                    in the supplied TrustContext.
                
            19. Evidence relating to other TrustDimensions may remain part of the
                    underlying evidence record, but must not create additional themes
                    or MaterialTrustQuestions in this synthesis.
                
            20. A MaterialTrustQuestion may only be created for one of the
                    important TrustDimensions in the supplied TrustContext.
                
            21. Do not expand the scope of the Trust Map simply because supplied
                    evidence contains interesting information about another dimension.
                    
            22. The absence of independent verification for a merchant claim
                    does not by itself create a MaterialTrustQuestion.
                
            23. Create a MaterialTrustQuestion only when the existing evidence
                    contains meaningful conflict, ambiguity, or uncertainty that
                    could plausibly change the current trust assessment.
                
            24. Prefer the smallest set of questions necessary to resolve
                    decision-relevant uncertainty.
                
            25. When multiple questions substantially overlap, combine them.
                
            26. Normally return no more than one MaterialTrustQuestion per
                 important TrustDimension. Return zero when no further research
                  is decision-relevant.

            Return only the structured MerchantEvidenceSynthesis.

            MerchantEvidenceSynthesis must preserve the supplied
            merchantId and merchantName exactly.
            """.formatted(
                merchantId,
                merchantName,
                context,
                evidence
        );
    }
}
