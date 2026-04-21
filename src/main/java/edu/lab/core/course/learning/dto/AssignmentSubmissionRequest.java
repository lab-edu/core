package edu.lab.core.course.learning.dto;

import java.util.List;
import java.util.UUID;

public record AssignmentSubmissionRequest(
    List<AssignmentAnswer> answers
) {
    public record AssignmentAnswer(
        UUID taskItemId,
        String answer
    ) {}
}