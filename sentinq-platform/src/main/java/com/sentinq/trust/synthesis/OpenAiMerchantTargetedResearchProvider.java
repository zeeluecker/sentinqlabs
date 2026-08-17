package com.sentinq.trust.synthesis;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.StructuredResponse;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.openai.models.responses.WebSearchTool;
import com.sentinq.trust.TrustContext;
import com.sentinq.trust.TrustEvidence;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OpenAiMerchantTargetedResearchProvider
        implements MerchantTargetedResearchProvider {

    private static final String MODEL = "gpt-5";

    private final OpenAIClient openAIClient;

    public OpenAiMerchantTargetedResearchProvider() {
        this.openAIClient =
                OpenAIOkHttpClient.fromEnv();
    }

    @Override
    public String providerId() {
        return "openai";
    }

    @Override
    public MerchantTargetedResearchDecision research(
            String merchantId,
            String merchantName,
            List<TrustEvidence> existingEvidence,
            MerchantEvidenceSynthesis synthesis,
            TrustContext context
    ) {
        validateInputs(
                merchantId,
                merchantName,
                existingEvidence,
                synthesis,
                context
        );

        StructuredResponseCreateParams<MerchantTargetedResearchDecision> params =
                ResponseCreateParams.builder()
                        .model(MODEL)
                        .input(
                                buildTargetedResearchPrompt(
                                        merchantId,
                                        merchantName,
                                        existingEvidence,
                                        synthesis,
                                        context
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
                                MerchantTargetedResearchDecision.class
                        )
                        .build();

        StructuredResponse<MerchantTargetedResearchDecision> response =
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
                                "OpenAI returned no structured targeted research decision."
                        )
                );
    }

    private void validateInputs(
            String merchantId,
            String merchantName,
            List<TrustEvidence> existingEvidence,
            MerchantEvidenceSynthesis synthesis,
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

        if (existingEvidence == null || existingEvidence.isEmpty()) {
            throw new IllegalArgumentException(
                    "Existing evidence is required."
            );
        }

        if (synthesis == null) {
            throw new IllegalArgumentException(
                    "Merchant evidence synthesis is required."
            );
        }

        if (synthesis.materialQuestions() == null
                || synthesis.materialQuestions().isEmpty()) {

            throw new IllegalArgumentException(
                    "At least one material trust question is required."
            );
        }

        if (context == null) {
            throw new IllegalArgumentException(
                    "Trust context is required."
            );
        }
    }

    private String buildTargetedResearchPrompt(
            String merchantId,
            String merchantName,
            List<TrustEvidence> existingEvidence,
            MerchantEvidenceSynthesis synthesis,
            TrustContext context
    ) {
        return """
                You are performing ONE bounded targeted research round
                for a Sentinq merchant Trust Map.

                Merchant ID:
                %s

                Merchant:
                %s

                Consumer context:
                %s

                Important TrustDimensions:
                %s

                Existing evidence:
                %s

                Current evidence synthesis:
                %s

                Material questions to resolve:
                %s

                Research rules:

                1. Research ONLY the supplied MaterialTrustQuestions.

                2. Do not invent additional research questions.

                3. Do not expand into other TrustDimensions.

                4. Prefer evidence that directly resolves, contradicts,
                   qualifies, or materially clarifies the supplied questions.

                5. Prefer independent expert, community, review, complaint,
                   or specialist evidence when independent verification matters.

                6. Merchant first-party sources may establish merchant
                   claims, policies, product specifications, or promises,
                   but must not be treated as independent proof of performance.

                7. Do not collect repetitive evidence merely to increase
                   evidence volume.

                8. Return a small number of high-information findings.

                9. Every finding must identify the TrustDimension and
                   ContextType of the material question it helps answer.

                10. Preserve uncertainty when research does not resolve
                    a material question.

                11. Do not make an overall merchant recommendation.

                12. Do not decide whether the consumer should buy.

                13. Findings must remain within the Important TrustDimensions
                    supplied above.

                14. A finding should answer or materially inform a supplied
                    MaterialTrustQuestion. Do not return unrelated discoveries.

                15. Prefer the smallest evidence set necessary to resolve
                    the material questions.

                16. If a material question cannot be resolved reliably,
                    preserve that uncertainty rather than filling the gap
                    with weak evidence.

                Return only MerchantTargetedResearchDecision.
                """.formatted(
                merchantId,
                merchantName,
                context,
                context.importantDimensions(),
                existingEvidence,
                synthesis,
                synthesis.materialQuestions()
        );
    }
}