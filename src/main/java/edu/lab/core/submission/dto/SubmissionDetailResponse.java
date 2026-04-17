package edu.lab.core.submission.dto;

import edu.lab.core.user.dto.UserSummaryResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SubmissionDetailResponse(
	UUID id,
	UUID experimentId,
	UserSummaryResponse submittedBy,
	String fileName,
	String filePath,
	String note,
	BigDecimal score,
	String feedback,
	boolean latest,
	LocalDateTime submittedAt
) {
}