package com.sentinq.controller;

import com.sentinq.goal.GoalClarificationRequiredException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(
            GoalClarificationRequiredException.class
    )
    public ResponseEntity<ClarificationResponse>
    handleGoalClarificationRequired(
            GoalClarificationRequiredException exception
    ) {
        ClarificationResponse response =
                new ClarificationResponse(
                        "CLARIFICATION_REQUIRED",
                        exception.getClarificationQuestions()
                );

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(response);
    }

    public record ClarificationResponse(
            String status,
            List<String> questions
    ) {
    }
}
