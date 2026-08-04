package com.sentinq.ai;

import com.openai.client.OpenAIClient;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import org.springframework.stereotype.Service;

@Service
public class GptTestService {

    private final OpenAIClient openAIClient;

    public GptTestService(OpenAIClient openAIClient) {
        this.openAIClient = openAIClient;
    }

    public String test() {
        ResponseCreateParams params =
                ResponseCreateParams.builder()
                        .model("gpt-5-mini")
                        .input(
                                "Reply with exactly: Hello from Sentinq GPT"
                        )
                        .build();

        Response response =
                openAIClient.responses().create(params);

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
                .map(outputText ->
                        outputText.text()
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "GPT returned no text output."
                        )
                );
    }
}