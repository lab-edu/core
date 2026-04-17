package edu.lab.core.course.dto;

import edu.lab.core.course.CourseMemberRole;
import edu.lab.core.user.dto.UserSummaryResponse;
import java.time.LocalDateTime;

public record CourseMemberResponse(UserSummaryResponse user, CourseMemberRole memberRole, LocalDateTime joinedAt) {
}