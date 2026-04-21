package edu.lab.core.course.learning.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record AssignmentSubmissionRequest(
    @NotEmpty(message = "提交内容不能为空")
    @Valid
    List<AssignmentAnswer> answers
) {
    public record AssignmentAnswer(
        @NotNull(message = "题目ID不能为空")
        UUID taskItemId,

        @NotBlank(message = "答案不能为空")
        String answer
    ) {}
}