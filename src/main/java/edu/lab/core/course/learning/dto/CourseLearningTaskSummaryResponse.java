package edu.lab.core.course.learning.dto;

import edu.lab.core.course.learning.LearningMaterialType;
import edu.lab.core.course.learning.LearningQuestionType;
import edu.lab.core.course.learning.LearningTaskKind;
import edu.lab.core.course.learning.LearningTaskType;
import edu.lab.core.user.dto.UserSummaryResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CourseLearningTaskSummaryResponse(
	UUID id,
	UUID knowledgePointId,
	String title,
	String description,
	LearningTaskType taskType,
	LearningTaskKind taskKind,
	LearningMaterialType materialType,
	LearningQuestionType questionType,
	String contentText,
	String mediaUrl,
	String fileName,
	List<String> options,
	String referenceAnswer,
	BigDecimal maxScore,
	LocalDateTime startAt,
	LocalDateTime dueAt,
	boolean notifyOnStart,
	boolean notifyBeforeDue24h,
	boolean notifyOnDue,
	boolean required,
	int sortOrder,
	UserSummaryResponse createdBy,
	LocalDateTime createdAt
) {
}