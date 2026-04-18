package edu.lab.core.submission.dto;

import java.util.List;

public record CourseGradeOverviewResponse(List<StudentGradeOverviewResponse> students) {
}