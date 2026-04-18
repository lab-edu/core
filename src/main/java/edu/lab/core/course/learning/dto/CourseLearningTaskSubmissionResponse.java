package edu.lab.core.course.learning.dto;

import edu.lab.core.course.learning.LearningQuestionType;
import edu.lab.core.course.learning.LearningTaskType;
import edu.lab.core.course.learning.LearningMaterialType;
import edu.lab.core.user.dto.UserSummaryResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CourseLearningTaskSubmissionResponse(
	UUID id,
	UUID taskId,
	UserSummaryResponse submittedBy,
	String answerText,
	String fileName,
	String filePath,
	BigDecimal score,
	String feedback,
	UserSummaryResponse gradedBy,
	LocalDateTime gradedAt,
	boolean latest,
	LocalDateTime submittedAt
) {
}