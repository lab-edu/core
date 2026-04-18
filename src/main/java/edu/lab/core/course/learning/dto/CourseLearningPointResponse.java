package edu.lab.core.course.learning.dto;

import edu.lab.core.user.dto.UserSummaryResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CourseLearningPointResponse(
	UUID id,
	UUID unitId,
	String title,
	String summary,
	Integer estimatedMinutes,
	int sortOrder,
	UserSummaryResponse createdBy,
	LocalDateTime createdAt,
	List<CourseLearningTaskSummaryResponse> tasks
) {
}