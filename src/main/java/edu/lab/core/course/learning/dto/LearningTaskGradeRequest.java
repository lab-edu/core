package edu.lab.core.course.learning.dto;

import java.math.BigDecimal;

public record LearningTaskGradeRequest(
	BigDecimal score,
	String feedback
) {
}