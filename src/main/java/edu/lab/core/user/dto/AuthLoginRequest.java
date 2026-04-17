package edu.lab.core.user.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthLoginRequest(
	@NotBlank String identifier,
	@NotBlank String password
) {
}