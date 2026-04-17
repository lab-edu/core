package edu.lab.core.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CourseCreateRequest(
	@NotBlank @Size(max = 120) String title,
	String description
) {
}