package edu.lab.core.course.announcement.dto;

import java.util.List;

public record CourseAnnouncementListResponse(List<CourseAnnouncementSummaryResponse> items) {
}