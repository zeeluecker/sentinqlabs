package com.sentinq.ai;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.stereotype.Service;

@Service
public class GoalInterpretationService {

    private final OpenAIClient openAIClient;

    public GoalInterpretationService() {
        this.openAIClient =
                OpenAIOkHttpClient.fromEnv();
    }
}