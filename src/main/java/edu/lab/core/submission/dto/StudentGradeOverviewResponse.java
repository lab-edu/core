package edu.lab.core.submission.dto;

import edu.lab.core.user.dto.UserSummaryResponse;
import java.math.BigDecimal;
import java.util.List;

public record StudentGradeOverviewResponse(
	UserSummaryResponse student,
	int submissionCount,
	int gradedCount,
	BigDecimal averageScore,
	List<ExperimentGradeItemResponse> experiments
) {
}