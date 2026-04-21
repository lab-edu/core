package edu.lab.core.course.learning.dto;

import edu.lab.core.course.learning.LearningQuestionType;
import edu.lab.core.user.dto.UserSummaryResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AssignmentResponse(
    UUID id,
    UUID courseId,
    String title,
    String description,
    BigDecimal totalScore,
    LocalDateTime startAt,
    LocalDateTime dueAt,
    boolean required,
    int sortOrder,
    boolean published,
    boolean notifyOnStart,
    boolean notifyBeforeDue24h,
    boolean notifyOnDue,
    boolean autoCalculateTotal,
    UserSummaryResponse createdBy,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<AssignmentTaskItemResponse> taskItems
) {
    public record AssignmentTaskItemResponse(
        UUID id,
        String question,
        LearningQuestionType questionType,
        List<String> options,
        String referenceAnswer,
        BigDecimal maxScore,
        int sortOrder,
        UUID originalTaskId
    ) {}
}