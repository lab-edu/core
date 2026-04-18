package edu.lab.core.course.learning.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CourseLearningDetailResponse(
	UUID courseId,
	String courseTitle,
	String courseDescription,
	String inviteCode,
	LocalDateTime createdAt,
	List<CourseLearningUnitResponse> units
) {
}