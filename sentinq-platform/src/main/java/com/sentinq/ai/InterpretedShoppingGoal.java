package com.sentinq.ai;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

public class InterpretedShoppingGoal {

    @JsonPropertyDescription(
            "A concise product description suitable for product discovery."
    )
    public String productName;

    @JsonPropertyDescription(
            "Maximum total budget in cents, including shipping and tax."
    )
    public int maximumTotalCents;

    @JsonPropertyDescription(
            "Required delivery deadline in YYYY-MM-DD format."
    )
    public String deliveryDeadline;

    @JsonPropertyDescription(
            "Whether reasonable substitutions are allowed."
    )
    public boolean substitutionsAllowed;

    @JsonPropertyDescription(
            "Important product, appearance, size, compatibility, or usage requirements."
    )
    public List<String> productRequirements;

    @JsonPropertyDescription(
            "Questions that must be answered before execution. Empty when none are required."
    )
    public List<String> clarificationQuestions;
}