package edu.lab.core.submission;

import edu.lab.core.common.api.ApiResponse;
import edu.lab.core.security.AuthenticatedUser;
import edu.lab.core.submission.dto.CourseGradeOverviewResponse;
import edu.lab.core.submission.dto.SubmissionDetailResponse;
import edu.lab.core.submission.dto.SubmissionGradeRequest;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GradeController {

	private final SubmissionService submissionService;

	@PatchMapping("/api/v1/submissions/{submissionId}/grade")
	@Operation(summary = "教师评分")
	public ResponseEntity<ApiResponse<SubmissionDetailResponse>> grade(@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable UUID submissionId,
		@Valid @RequestBody SubmissionGradeRequest request) {
		return ResponseEntity.ok(ApiResponse.ok(submissionService.gradeSubmission(currentUser, submissionId, request.score(), request.feedback())));
	}

	@GetMapping("/api/v1/courses/{courseId}/grades/overview")
	@Operation(summary = "课程成绩汇总")
	public ResponseEntity<ApiResponse<CourseGradeOverviewResponse>> overview(@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable UUID courseId) {
		return ResponseEntity.ok(ApiResponse.ok(submissionService.getCourseGradeOverview(currentUser, courseId)));
	}
}
