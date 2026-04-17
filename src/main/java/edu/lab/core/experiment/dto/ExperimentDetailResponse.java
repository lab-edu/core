package edu.lab.core.experiment.dto;

import edu.lab.core.course.dto.CourseSummaryResponse;
import edu.lab.core.user.dto.UserSummaryResponse;
import java.time.LocalDateTime;
import java.util.UUID;

public record ExperimentDetailResponse(
	UUID id,
	CourseSummaryResponse course,
	String title,
	String description,
	LocalDateTime publishedAt,
	LocalDateTime dueAt,
	UserSummaryResponse createdBy
) {
}