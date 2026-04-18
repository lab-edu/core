package edu.lab.core.course.learning.dto;

import java.util.List;

public record CourseLearningOverviewResponse(
	int unitCount,
	int pointCount,
	int taskCount,
	List<StudentLearningOverviewResponse> students
) {
}