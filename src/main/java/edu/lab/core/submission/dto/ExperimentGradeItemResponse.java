package edu.lab.core.submission.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ExperimentGradeItemResponse(
	UUID experimentId,
	String experimentTitle,
	UUID submissionId,
	BigDecimal score,
	String feedback,
	LocalDateTime submittedAt,
	LocalDateTime gradedAt
) {
}