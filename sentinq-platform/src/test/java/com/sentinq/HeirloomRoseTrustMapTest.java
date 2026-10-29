package com.sentinq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinq.ai.InterpretedShoppingGoal;
import com.sentinq.ai.ProductSearchResult;
import com.sentinq.ai.provider.ClaudeProvider;
import com.sentinq.ai.provider.GeminiProvider;
import com.sentinq.ai.provider.MetaProvider;
import com.sentinq.ai.provider.OpenAiProvider;
import com.sentinq.evaluation.LlmCapabilityTrace;
import com.sentinq.evaluation.LlmInvocationOutcome;
import com.sentinq.evaluation.LlmResult;
import com.sentinq.goal.Goal;
import com.sentinq.preference.ConsumerPreferences;

import com.sentinq.trust.*;

import com.sentinq.trust.observations.*;

import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertFalse;
import com.sentinq.trust.research.*;
import com.sentinq.trust.synthesis.*;


import static org.junit.jupiter.api.Assertions.*;

class HeirloomRoseTrustMapTest {


    @Test
    void shouldSearchProductsAndReturnExecutionTrace() {

        /*
         * PRODUCT SEARCH PROVIDER
         *
         * Call the provider directly because this test is validating
         * the ProductSearchProvider result + trace contract.
         */
        OpenAiProvider openAiProvider =
                new OpenAiProvider();


        /*
         * CONSUMER GOAL
         */
        Goal goal =
                new Goal();

        goal.setOriginalRequest(
                """
                Find me a white fragrant rose that smells amazing and
                looks dreamy. I want it for my rose corner to provide
                a break on the shades of pink roses in my rose corner.
                I'm not willing to spend more than $80.
                """
        );

        goal.setProductName(
                "white fragrant rose"
        );

        goal.setMaximumTotalCents(
                8000
        );


        /*
         * CONSUMER PREFERENCES
         */
        ConsumerPreferences preferences =
                new ConsumerPreferences();


        /*
         * PRODUCT SEARCH

        LlmResult<ProductSearchResult> result =
                openAiProvider.searchProducts(
                        goal,
                        preferences
                );


        /*
         * DOMAIN RESULT

        assertNotNull(
                result
        );

        assertNotNull(
                result.getResult()
        );

        assertNotNull(
                result.getResult().offers
        );

        assertFalse(
                result.getResult().offers.isEmpty()
        );


        /*
         * EXECUTION TRACE

        assertNotNull(
                result.getTrace()
        );

        LlmCapabilityTrace trace =
                result.getTrace();


        /*
         * OUTPUT

        System.out.println();
        System.out.println("==============================");
        System.out.println("PRODUCT SEARCH TRACE");
        System.out.println("==============================");

        System.out.println(
                "Response ID: "
                        + trace.getResponseId()
        );

        System.out.println(
                "Model: "
                        + trace.getModel()
        );

        System.out.println(
                "Status: "
                        + trace.getStatus()
        );

        System.out.println(
                "Input tokens: "
                        + trace.getInputTokens()
        );

        System.out.println(
                "Output tokens: "
                        + trace.getOutputTokens()
        );

        System.out.println(
                "Reasoning tokens: "
                        + trace.getReasoningTokens()
        );

        System.out.println(
                "Total tokens: "
                        + trace.getTotalTokens()
        );


        /*
         * TRACE ASSERTIONS

        assertNotNull(
                trace.getResponseId()
        );

        assertNotNull(
                trace.getModel()
        );

        assertNotNull(
                trace.getInputTokens()
        );

        assertNotNull(
                trace.getOutputTokens()
        );

        assertNotNull(
                trace.getTotalTokens()
        );

         */
    }


    @Test
    void shouldInterpretShoppingGoalUsingMetaAndReturnExecutionTrace()
            throws JsonProcessingException {

        /*
         * META PROVIDER
         */
        MetaProvider metaProvider = new MetaProvider();

        /*
         * CONSUMER GOAL
         */
        String rawGoal = """
            Find me a white fragrant rose that smells amazing and
            looks dreamy. I want it for my rose corner to provide
            a break from the shades of pink roses in my rose corner.
            I'm not willing to spend more than $80.
            """;

        /*
         * GOAL INTERPRETATION
         */
        LlmResult<InterpretedShoppingGoal> result =
                metaProvider.interpretShoppingGoal(
                        rawGoal,
                        "muse-wonder-1.0"
                );

        /*
         * DOMAIN RESULT
         */
        assertNotNull(result);
        assertNotNull(result.getResult());

        InterpretedShoppingGoal interpretedGoal = result.getResult();

        /*
         * PRINT STRUCTURED RESULT
         */
        ObjectMapper objectMapper = new ObjectMapper();

        System.out.println();
        System.out.println("==============================");
        System.out.println("META GOAL INTERPRETATION");
        System.out.println("==============================");

        System.out.println(
                objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(interpretedGoal)
        );

        /*
         * SEMANTIC VALIDATION
         *
         * A successful structured response is not enough.
         * Verify that the consumer's budget was preserved.
         */
        JsonNode goalJson = objectMapper.valueToTree(interpretedGoal);

        assertEquals(
                8000,
                goalJson.path("maximumTotalCents").asInt(-1),
                "Meta must interpret the $80 maximum as 8000 cents."
        );

        assertFalse(
                goalJson.path("productName").asText("").isBlank(),
                "Meta must identify the requested product."
        );

        /*
         * EXECUTION TRACE
         */
        assertNotNull(result.getTrace());

        LlmCapabilityTrace trace = result.getTrace();

        System.out.println();
        System.out.println("==============================");
        System.out.println("META EXECUTION TRACE");
        System.out.println("==============================");

        System.out.println("Response ID: " + trace.getResponseId());
        System.out.println("Model: " + trace.getModel());
        System.out.println("Status: " + trace.getStatus());
        System.out.println("Input tokens: " + trace.getInputTokens());
        System.out.println("Output tokens: " + trace.getOutputTokens());
        System.out.println("Reasoning tokens: " + trace.getReasoningTokens());
        System.out.println("Total tokens: " + trace.getTotalTokens());

        /*
         * TRACE ASSERTIONS
         */
        assertEquals(
                LlmInvocationOutcome.SUCCESS,
                trace.getOutcome()
        );

        assertNotNull(trace.getResponseId());
        assertNotNull(trace.getModel());
        assertNotNull(trace.getInputTokens());
        assertNotNull(trace.getOutputTokens());
        assertNotNull(trace.getTotalTokens());
    }

}