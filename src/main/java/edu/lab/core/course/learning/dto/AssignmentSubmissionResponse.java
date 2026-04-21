package edu.lab.core.course.learning.dto;

import edu.lab.core.course.learning.LearningQuestionType;

import edu.lab.core.user.dto.UserSummaryResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AssignmentSubmissionResponse(
    UUID id,
    UUID assignmentId,
    List<AssignmentAnswerResponse> answers,
    BigDecimal totalScore,
    String feedback,
    UserSummaryResponse gradedBy,
    LocalDateTime gradedAt,
    boolean latest,
    UserSummaryResponse submittedBy,
    LocalDateTime submittedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public record AssignmentAnswerResponse(
        UUID taskItemId,
        String question,
        LearningQuestionType questionType,
        List<String> options,
        String answer,
        BigDecimal maxScore,
        BigDecimal score,
        String feedback
    ) {}
}