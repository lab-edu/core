package edu.lab.core.course.learning.dto;

public record LearningUnitCreateRequest(
	String title,
	String description,
	Integer sortOrder,
	Boolean published
) {
}