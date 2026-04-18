package edu.lab.core.course.learning.dto;

public record LearningPointCreateRequest(
	String title,
	String summary,
	Integer estimatedMinutes,
	Integer sortOrder
) {
}