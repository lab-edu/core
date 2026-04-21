package edu.lab.core.course.learning.dto;

import edu.lab.core.course.learning.LearningQuestionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AssignmentUpdateRequest(
    UUID id,
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
    List<AssignmentTaskItemUpdateRequest> taskItems
) {
    public record AssignmentTaskItemUpdateRequest(
        UUID id,
        String question,
        LearningQuestionType questionType,
        List<String> options,
        String referenceAnswer,
        BigDecimal maxScore,
        int sortOrder
    ) {}
}