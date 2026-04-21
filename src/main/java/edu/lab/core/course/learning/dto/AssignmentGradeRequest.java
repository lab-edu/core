package edu.lab.core.course.learning.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AssignmentGradeRequest(
    @NotEmpty(message = "请至少提供一道题目的评分")
    @Valid
    List<AssignmentItemGrade> itemGrades,

    @DecimalMin(value = "0.00", inclusive = true, message = "总分不能小于0")
    BigDecimal totalScore,

    @Size(max = 10000, message = "总体反馈过长")
    String feedback
) {
    public record AssignmentItemGrade(
        @NotNull(message = "题目ID不能为空")
        UUID taskItemId,

        @NotNull(message = "题目得分不能为空")
        @DecimalMin(value = "0.00", inclusive = true, message = "题目得分不能小于0")
        BigDecimal score,

        @Size(max = 5000, message = "题目反馈过长")
        String feedback
    ) {}
}