package edu.lab.core.course.learning.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CourseHomeworkItemResponse(
	UUID taskId,
	UUID courseId,
	String courseTitle,
	String title,
	LocalDateTime startAt,
	LocalDateTime dueAt,
	String status,
	UUID latestSubmissionId,
	LocalDateTime submittedAt,
	BigDecimal score,
	long submittedCount,
	long totalStudentCount,
	Long remainingSeconds
) {
}