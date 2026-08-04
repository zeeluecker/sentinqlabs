package com.sentinq.ai;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

public class InterpretedShoppingGoal {

    @JsonPropertyDescription(
            "A concise product category or product type, such as container dahlia."
    )
    public String productName;

    @JsonPropertyDescription(
            "Maximum total purchase amount in cents, including shipping and tax."
    )
    public int maximumTotalCents;

    @JsonPropertyDescription(
            "Required delivery deadline in ISO-8601 date format: YYYY-MM-DD."
    )
    public String deliveryDeadline;

    @JsonPropertyDescription(
            "Whether reasonable substitutions are permitted."
    )
    public boolean substitutionsAllowed;

    @JsonPropertyDescription(
            "Important product requirements inferred from the consumer's request."
    )
    public List<String> productRequirements;

    @JsonPropertyDescription(
            "Questions that must be answered before the goal can be safely executed. Return an empty list when none are required."
    )
    public List<String> clarificationQuestions;
}