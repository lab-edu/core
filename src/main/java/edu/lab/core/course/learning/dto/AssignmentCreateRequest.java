package edu.lab.core.course.learning.dto;

import edu.lab.core.course.learning.LearningQuestionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AssignmentCreateRequest(
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
    List<AssignmentTaskItemRequest> taskItems
) {
    public record AssignmentTaskItemRequest(
        String question,
        LearningQuestionType questionType,
        List<String> options,
        String referenceAnswer,
        BigDecimal maxScore,
        int sortOrder
    ) {}
}