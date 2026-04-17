package edu.lab.core.course.dto;

import java.util.List;

public record CourseListResponse(List<CourseSummaryResponse> items) {
}