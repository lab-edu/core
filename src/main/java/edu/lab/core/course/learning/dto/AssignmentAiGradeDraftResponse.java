package edu.lab.core.course.learning.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AssignmentAiGradeDraftResponse(
    BigDecimal totalScore,
    String feedback,
    List<AssignmentAiItemGradeResponse> itemGrades
) {
    public record AssignmentAiItemGradeResponse(
        UUID taskItemId,
        BigDecimal score,
        String feedback
    ) {
    }
}
