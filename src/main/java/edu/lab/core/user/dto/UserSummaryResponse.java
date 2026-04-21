package edu.lab.core.user.dto;

import edu.lab.core.user.AppUser;
import edu.lab.core.user.UserRole;
import java.util.UUID;

public record UserSummaryResponse(UUID id, String username, String displayName, String email, UserRole role) {
    public static UserSummaryResponse from(AppUser user) {
        return new UserSummaryResponse(
            user.getId(),
            user.getUsername(),
            user.getDisplayName(),
            user.getEmail(),
            user.getRole()
        );
    }
}