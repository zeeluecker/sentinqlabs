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

        MerchantTargetedResearchDecision decision =
                response.output()
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

        return new MerchantTargetedResearchDecision(
                decision.findings()
                        .stream()
                        .limit(3)
                        .toList()
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
        String formattedContext =
                formatTrustContext(
                        context
                );

        String formattedThemes =
                formatThemes(
                        synthesis
                );

        String formattedQuestions =
                formatMaterialQuestions(
                        synthesis
                );

        return """
        You are performing ONE bounded targeted research round
        for a Sentinq merchant Trust Map.

        Merchant ID:
        %s

        Merchant:
        %s

        Consumer context:
        %s

        Current Trust Map themes:
        %s

        Material questions:
        %s

        The current themes are the established synthesis of
        the previously observed evidence.

        Your job is NOT to broadly research this merchant.

        Your job is to find the minimum additional evidence
        necessary to materially clarify the supplied questions.

        Research rules:

        1. Research ONLY the supplied MaterialTrustQuestions.

        2. Do not create or investigate additional questions
           or TrustDimensions.

        3. Do not repeat broad merchant research already represented
           by the current themes.

        4. Look specifically for evidence capable of changing,
           strengthening, weakening, qualifying, or resolving the
           current interpretation.

        5. Prefer independent expert, community, review, complaint,
           or specialist evidence when independent verification
           is relevant.

        6. Merchant first-party sources may clarify merchant
           policies, specifications, or promises, but are not
           independent proof of performance.

        7. Research is decision-bounded, not exhaustive.
           Stop once additional searching is unlikely to materially
           change the answer to a supplied question.

        8. Do not gather multiple sources that establish
           substantially the same fact. Prefer the strongest source.

        9. Return no more than 3 findings total.

        10. Normally return no more than 2 findings for any
            MaterialTrustQuestion.

        11. A finding must directly answer, contradict, qualify,
            or materially clarify a supplied question.

        12. If reliable evidence cannot resolve the question,
            preserve the uncertainty rather than continuing to
            search broadly.

        13. Every finding must identify the TrustDimension and
            ContextType of the question it addresses.

        14. Do not make an overall merchant recommendation.

        15. Do not decide whether the consumer should buy.

        Return only MerchantTargetedResearchDecision.
        """.formatted(
                merchantId,
                merchantName,
                formattedContext,
                formattedThemes,
                formattedQuestions
        );
    }



    private String formatTrustContext(
            TrustContext context
    ) {
        return """
            Goal: %s
            Product category: %s
            Product type: %s
            Product attributes: %s
            Transaction value: %s
            Delivery urgency: %s
            Merchant familiarity: %s
            Important TrustDimensions: %s
            """.formatted(
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

    private String formatThemes(
            MerchantEvidenceSynthesis synthesis
    ) {
        return synthesis.themes()
                .stream()
                .map(theme -> """
                    Dimension: %s
                    Signal: %s
                    Theme: %s
                    """.formatted(
                        theme.dimension(),
                        theme.signal(),
                        theme.theme()
                ))
                .collect(
                        java.util.stream.Collectors.joining(
                                "\n---\n"
                        )
                );
    }

    private String formatMaterialQuestions(
            MerchantEvidenceSynthesis synthesis
    ) {
        return synthesis.materialQuestions()
                .stream()
                .map(question -> """
                    Dimension: %s
                    Context type: %s
                    Question: %s
                    Why it matters: %s
                    """.formatted(
                        question.dimension(),
                        question.contextType(),
                        question.question(),
                        question.reason()
                ))
                .collect(
                        java.util.stream.Collectors.joining(
                                "\n---\n"
                        )
                );
    }
}