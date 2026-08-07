package com.sentinq.goal;

import com.sentinq.ai.InterpretedShoppingGoal;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@Component
public class GoalFactory {

    public Goal create(
            UUID principalId,
            String rawGoalText,
            InterpretedShoppingGoal interpretation
    ) {
        validateInputs(
                principalId,
                rawGoalText,
                interpretation
        );

        Goal goal = new Goal();

        goal.setGoalId(
                UUID.randomUUID()
        );

        goal.setPrincipalId(
                principalId
        );

        goal.setOriginalRequest(
                rawGoalText
        );

        goal.setProductName(
                interpretation.productName
        );

        goal.setMaximumTotalCents(
                interpretation.maximumTotalCents
        );

        goal.setDeliveryDeadline(
                parseDeliveryDeadline(
                        interpretation.deliveryDeadline
                )
        );

        goal.setSubstitutionsAllowed(
                interpretation.substitutionsAllowed
        );

        return goal;
    }

    private void validateInputs(
            UUID principalId,
            String rawGoalText,
            InterpretedShoppingGoal interpretation
    ) {
        if (principalId == null) {
            throw new IllegalArgumentException(
                    "principalId is required."
            );
        }

        if (rawGoalText == null ||
                rawGoalText.isBlank()) {
            throw new IllegalArgumentException(
                    "Raw goal text is required."
            );
        }

        if (interpretation == null) {
            throw new IllegalArgumentException(
                    "Goal interpretation is required."
            );
        }

        if (interpretation.productName == null ||
                interpretation.productName.isBlank()) {
            throw new IllegalArgumentException(
                    "Interpreted product name is required."
            );
        }

        if (interpretation.maximumTotalCents <= 0) {
            throw new IllegalArgumentException(
                    "Maximum total must be greater than zero."
            );
        }

        if (interpretation.clarificationQuestions != null &&
                !interpretation.clarificationQuestions.isEmpty()) {
            throw new GoalClarificationRequiredException(
                    interpretation.clarificationQuestions
            );
        }
    }

    private LocalDate parseDeliveryDeadline(
            String deliveryDeadline
    ) {
        if (deliveryDeadline == null ||
                deliveryDeadline.isBlank()) {
            throw new IllegalArgumentException(
                    "Delivery deadline is required."
            );
        }

        try {
            return LocalDate.parse(
                    deliveryDeadline
            );
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    "Delivery deadline must use YYYY-MM-DD.",
                    exception
            );
        }
    }
}