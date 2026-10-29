
package com.sentinq.ai.provider;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.StructuredResponse;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.sentinq.ai.InterpretedShoppingGoal;
import com.sentinq.evaluation.LlmCapabilityTrace;
import com.sentinq.evaluation.LlmInvocationException;
import com.sentinq.evaluation.LlmInvocationOutcome;
import com.sentinq.evaluation.LlmResult;
import org.springframework.stereotype.Component;

@Component
public class MetaProvider implements LlmProvider {

    private final OpenAIClient metaClient;

    public MetaProvider() {
        String apiKey = System.getenv("MUSE_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "MUSE_API_KEY is required for Meta provider."
            );
        }

        this.metaClient = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .baseUrl("https://api.meta.ai/v1")
                .build();
    }

    @Override
    public String getProviderId() {
        return "meta";
    }

    @Override
    public LlmResult<InterpretedShoppingGoal> interpretShoppingGoal(
            String rawGoalText,
            String model
    ) {
        StructuredResponseCreateParams<InterpretedShoppingGoal> params =
                ResponseCreateParams.builder()
                        .model(model)
                        .input(buildPrompt(rawGoalText))
                        .text(InterpretedShoppingGoal.class)
                        .build();

        StructuredResponse<InterpretedShoppingGoal> response;

        try {
            response = metaClient.responses().create(params);

        } catch (RuntimeException e) {
            LlmCapabilityTrace trace = new LlmCapabilityTrace();

            trace.setModel(model);
            trace.setOutcome(
                    LlmInvocationOutcome.PROVIDER_ERROR
            );

            throw new LlmInvocationException(
                    "Meta goal interpretation invocation failed.",
                    e,
                    trace
            );
        }

        LlmCapabilityTrace trace = new LlmCapabilityTrace();

        trace.setResponseId(response.id());
        trace.setModel(response.model().toString());
        trace.setStatus(response.status().toString());

        response.usage().ifPresent(usage -> {
            trace.setInputTokens(usage.inputTokens());
            trace.setOutputTokens(usage.outputTokens());
            trace.setReasoningTokens(
                    usage.outputTokensDetails().reasoningTokens()
            );
            trace.setTotalTokens(usage.totalTokens());
        });

        try {
            InterpretedShoppingGoal interpretedGoal =
                    response.output()
                            .stream()
                            .flatMap(item -> item.message().stream())
                            .flatMap(message -> message.content().stream())
                            .flatMap(content -> content.outputText().stream())
                            .findFirst()
                            .orElseThrow(() ->
                                    new IllegalStateException(
                                            "Meta returned no structured goal interpretation."
                                    )
                            );

            trace.setOutcome(LlmInvocationOutcome.SUCCESS);

            return new LlmResult<>(
                    interpretedGoal,
                    trace
            );

        } catch (RuntimeException e) {
            trace.setOutcome(
                    LlmInvocationOutcome.CONTRACT_VIOLATION
            );

            throw new LlmInvocationException(
                    "Meta goal interpretation violated the expected contract.",
                    e,
                    trace
            );
        }
    }

    private String buildPrompt(String rawGoalText) {
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
