package edu.lab.core.submission;

import edu.lab.core.common.api.ApiResponse;
import edu.lab.core.security.AuthenticatedUser;
import edu.lab.core.submission.dto.SubmissionCreateResponse;
import edu.lab.core.submission.dto.SubmissionDetailResponse;
import edu.lab.core.submission.dto.SubmissionListResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/experiments/{experimentId}/submissions")
@RequiredArgsConstructor
public class SubmissionController {

	private final SubmissionService submissionService;

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "学生提交实验文件")
	public ResponseEntity<ApiResponse<SubmissionCreateResponse>> submit(@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable UUID experimentId,
		@RequestParam("file") MultipartFile file,
		@RequestParam(value = "note", required = false) String note) {
		return ResponseEntity.status(201).body(ApiResponse.created(new SubmissionCreateResponse(submissionService.submit(currentUser, experimentId, file, note))));
	}

	@GetMapping
	@Operation(summary = "查看实验提交记录")
	public ResponseEntity<ApiResponse<SubmissionListResponse>> list(@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable UUID experimentId) {
		return ResponseEntity.ok(ApiResponse.ok(new SubmissionListResponse(submissionService.listSubmissions(currentUser, experimentId))));
	}

	@GetMapping("/latest")
	@Operation(summary = "查看当前用户最新提交")
	public ResponseEntity<ApiResponse<SubmissionDetailResponse>> latest(@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable UUID experimentId) {
		return ResponseEntity.ok(ApiResponse.ok(submissionService.getLatestSubmission(currentUser, experimentId)));
	}
}