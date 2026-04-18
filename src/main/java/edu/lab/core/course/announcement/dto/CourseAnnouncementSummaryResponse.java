package edu.lab.core.course.announcement.dto;

import edu.lab.core.user.dto.UserSummaryResponse;
import java.time.LocalDateTime;
import java.util.UUID;

public record CourseAnnouncementSummaryResponse(
	UUID id,
	UUID courseId,
	String title,
	String content,
	UserSummaryResponse createdBy,
	LocalDateTime createdAt
) {
}