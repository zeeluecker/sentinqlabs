package com.sentinq.goal;

import java.util.List;

public class GoalClarificationRequiredException
        extends RuntimeException {

    private final List<String> clarificationQuestions;

    public GoalClarificationRequiredException(
            List<String> clarificationQuestions
    ) {
        super("Goal requires clarification.");

        this.clarificationQuestions =
                clarificationQuestions;
    }

    public List<String> getClarificationQuestions() {
        return clarificationQuestions;
    }
}