package edu.lab.core.course.learning.dto;

import edu.lab.core.user.dto.UserSummaryResponse;
import java.math.BigDecimal;
import java.util.List;

public record StudentLearningOverviewResponse(
	UserSummaryResponse student,
	int submissionCount,
	int gradedCount,
	BigDecimal averageScore,
	List<LearningTaskProgressResponse> tasks
) {
}