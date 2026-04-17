package edu.lab.core.course.dto;

import edu.lab.core.experiment.dto.ExperimentSummaryResponse;
import edu.lab.core.user.dto.UserSummaryResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CourseDetailResponse(
	UUID id,
	String title,
	String description,
	String inviteCode,
	UserSummaryResponse owner,
	List<CourseMemberResponse> members,
	List<ExperimentSummaryResponse> experiments,
	LocalDateTime createdAt
) {
}