package edu.lab.core.experiment.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ExperimentSummaryResponse(
	UUID id,
	UUID courseId,
	String title,
	String description,
	LocalDateTime publishedAt,
	LocalDateTime dueAt,
	UUID createdById,
	String createdByUsername
) {
}