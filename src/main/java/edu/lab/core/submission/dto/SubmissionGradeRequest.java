package edu.lab.core.submission.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record SubmissionGradeRequest(
	@NotNull(message = "分数不能为空")
	@DecimalMin(value = "0.0", message = "分数不能小于 0")
	@DecimalMax(value = "100.0", message = "分数不能大于 100")
	BigDecimal score,
	@Size(max = 2000, message = "评语长度不能超过 2000")
	String feedback
) {
}