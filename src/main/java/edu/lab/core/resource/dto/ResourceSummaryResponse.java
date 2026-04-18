package edu.lab.core.resource.dto;

import edu.lab.core.resource.ResourceType;
import edu.lab.core.user.dto.UserSummaryResponse;
import java.time.LocalDateTime;
import java.util.UUID;

public record ResourceSummaryResponse(
	UUID id,
	UUID courseId,
	String name,
	ResourceType type,
	String category,
	String fileName,
	String externalUrl,
	UserSummaryResponse uploadedBy,
	LocalDateTime uploadedAt
) {
}