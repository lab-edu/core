package edu.lab.core.user.dto;

import edu.lab.core.user.UserRole;
import java.util.UUID;

public record UserSummaryResponse(UUID id, String username, String displayName, String email, UserRole role) {
}