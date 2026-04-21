package edu.lab.core.course.learning.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AssignmentGradeRequest(
    List<AssignmentItemGrade> itemGrades,
    BigDecimal totalScore,
    String feedback
) {
    public record AssignmentItemGrade(
        UUID taskItemId,
        BigDecimal score,
        String feedback
    ) {}
}