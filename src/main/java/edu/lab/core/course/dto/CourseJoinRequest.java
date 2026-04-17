package edu.lab.core.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CourseJoinRequest(
	@NotBlank @Size(min = 4, max = 32) String inviteCode
) {
}