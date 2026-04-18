package edu.lab.core.course.learning.dto;

import edu.lab.core.course.learning.LearningMaterialType;
import edu.lab.core.course.learning.LearningQuestionType;
import edu.lab.core.course.learning.LearningTaskType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record LearningTaskProgressResponse(
	UUID unitId,
	String unitTitle,
	UUID pointId,
	String pointTitle,
	UUID taskId,
	String taskTitle,
	LearningTaskType taskType,
	LearningMaterialType materialType,
	LearningQuestionType questionType,
	BigDecimal maxScore,
	UUID submissionId,
	String answerText,
	String fileName,
	BigDecimal score,
	String feedback,
	LocalDateTime submittedAt,
	LocalDateTime gradedAt,
	boolean latest
) {
}