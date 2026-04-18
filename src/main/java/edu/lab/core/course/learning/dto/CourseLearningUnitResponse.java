package edu.lab.core.course.learning.dto;

import edu.lab.core.user.dto.UserSummaryResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CourseLearningUnitResponse(
	UUID id,
	UUID courseId,
	String title,
	String description,
	int sortOrder,
	boolean published,
	UserSummaryResponse createdBy,
	LocalDateTime createdAt,
	List<CourseLearningPointResponse> points
) {
}