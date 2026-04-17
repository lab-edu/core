package edu.lab.core.experiment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record ExperimentCreateRequest(
	@NotBlank @Size(max = 120) String title,
	String description,
	@NotNull LocalDateTime dueAt
) {
}