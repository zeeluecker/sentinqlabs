package com.sentinq.ai;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

public class GoalInterpretation {

    @JsonPropertyDescription(
            "A concise product or service objective suitable for downstream discovery."
    )
    public String objective;

    @JsonPropertyDescription(
            "The maximum total budget in cents, including shipping and tax."
    )
    public int maximumTotalCents;

    @JsonPropertyDescription(
            "The required delivery deadline in ISO-8601 format: YYYY-MM-DD."
    )
    public String deliveryDeadline;

    @JsonPropertyDescription(
            "Whether reasonable substitutions are allowed."
    )
    public boolean substitutionsAllowed;

    @JsonPropertyDescription(
            "Important product, compatibility, visual, functional, size, or usage requirements."
    )
    public List<String> requirements;

    @JsonPropertyDescription(
            "Questions that must be answered before this goal can safely proceed. Return an empty list if none are required."
    )
    public List<String> clarificationQuestions;
}