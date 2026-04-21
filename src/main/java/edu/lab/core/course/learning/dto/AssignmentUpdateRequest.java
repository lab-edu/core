package edu.lab.core.course.learning.dto;

import edu.lab.core.course.learning.LearningQuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AssignmentUpdateRequest(
    UUID id,

    @NotBlank(message = "作业标题不能为空")
    @Size(max = 120, message = "作业标题长度不能超过120")
    String title,

    @Size(max = 20000, message = "作业描述过长")
    String description,

    @DecimalMin(value = "0.00", inclusive = true, message = "总分不能小于0")
    BigDecimal totalScore,

    LocalDateTime startAt,

    LocalDateTime dueAt,

    Boolean required,

    Integer sortOrder,

    Boolean published,

    Boolean notifyOnStart,

    Boolean notifyBeforeDue24h,

    Boolean notifyOnDue,

    Boolean autoCalculateTotal,

    @Valid
    List<AssignmentTaskItemUpdateRequest> taskItems
) {
    public record AssignmentTaskItemUpdateRequest(
        UUID id,

        @NotBlank(message = "题目内容不能为空")
        @Size(max = 500, message = "题目内容长度不能超过500")
        String question,

        @NotNull(message = "题型不能为空")
        LearningQuestionType questionType,

        @Size(max = 100, message = "选项数量过多")
        List<String> options,

        @Size(max = 5000, message = "参考答案过长")
        String referenceAnswer,

        @NotNull(message = "题目分值不能为空")
        @DecimalMin(value = "0.00", inclusive = true, message = "题目分值不能小于0")
        BigDecimal maxScore,

        Integer sortOrder
    ) {}
}