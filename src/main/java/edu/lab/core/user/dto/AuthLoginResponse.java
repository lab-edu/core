package edu.lab.core.user.dto;

import java.time.LocalDateTime;

public record AuthLoginResponse(UserSummaryResponse user, String tokenType, LocalDateTime expiresAt) {
}