package edu.lab.core.course.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CourseSummaryResponse(
	UUID id,
	String title,
	String description,
	String inviteCode,
	UUID ownerId,
	String ownerUsername,
	long memberCount,
	LocalDateTime createdAt
) {
}