package edu.lab.core.course.learning;

import edu.lab.core.common.api.ApiResponse;
import edu.lab.core.course.learning.dto.CourseLearningDetailResponse;
import edu.lab.core.course.learning.dto.CourseLearningOverviewResponse;
import edu.lab.core.course.learning.dto.CourseLearningPointCreateResponse;
import edu.lab.core.course.learning.dto.CourseLearningPointResponse;
import edu.lab.core.course.learning.dto.CourseLearningTaskCreateResponse;
import edu.lab.core.course.learning.dto.CourseLearningTaskSubmissionCreateResponse;
import edu.lab.core.course.learning.dto.CourseLearningTaskSubmissionListResponse;
import edu.lab.core.course.learning.dto.CourseLearningTaskSubmissionResponse;
import edu.lab.core.course.learning.dto.CourseLearningTaskSummaryResponse;
import edu.lab.core.course.learning.dto.CourseLearningUnitCreateResponse;
import edu.lab.core.course.learning.dto.CourseLearningUnitResponse;
import edu.lab.core.course.learning.dto.LearningPointCreateRequest;
import edu.lab.core.course.learning.dto.LearningTaskGradeRequest;
import edu.lab.core.course.learning.dto.LearningUnitCreateRequest;
import edu.lab.core.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/learning")
@RequiredArgsConstructor
public class CourseLearningController {

	private final CourseLearningService courseLearningService;

	@GetMapping
	@Operation(summary = "获取课程学习结构")
	public ResponseEntity<ApiResponse<CourseLearningDetailResponse>> detail(@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable UUID courseId) {
		return ResponseEntity.ok(ApiResponse.ok(courseLearningService.getLearningDetail(currentUser, courseId)));
	}

	@GetMapping("/overview")
	@Operation(summary = "获取课程学习汇总")
	public ResponseEntity<ApiResponse<CourseLearningOverviewResponse>> overview(@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable UUID courseId) {
		return ResponseEntity.ok(ApiResponse.ok(courseLearningService.getOverview(currentUser, courseId)));
	}

	@PostMapping("/units")
	@Operation(summary = "创建学习单元")
	public ResponseEntity<ApiResponse<CourseLearningUnitResponse>> createUnit(@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable UUID courseId,
		@Valid @RequestBody LearningUnitCreateRequest request) {
		return ResponseEntity.status(201).body(ApiResponse.created(courseLearningService.createUnit(currentUser, courseId, request)));
	}

	@PostMapping("/units/{unitId}/points")
	@Operation(summary = "创建知识点")
	public ResponseEntity<ApiResponse<CourseLearningPointResponse>> createPoint(@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable UUID courseId,
		@PathVariable UUID unitId,
		@Valid @RequestBody LearningPointCreateRequest request) {
		return ResponseEntity.status(201).body(ApiResponse.created(courseLearningService.createPoint(currentUser, courseId, unitId, request)));
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, value = "/points/{pointId}/tasks")
	@Operation(summary = "创建学习任务")
	public ResponseEntity<ApiResponse<CourseLearningTaskSummaryResponse>> createTask(@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable UUID courseId,
		@PathVariable UUID pointId,
		@RequestParam("title") String title,
		@RequestParam(value = "description", required = false) String description,
		@RequestParam(value = "taskType", required = false) LearningTaskType taskType,
		@RequestParam(value = "materialType", required = false) LearningMaterialType materialType,
		@RequestParam(value = "contentText", required = false) String contentText,
		@RequestParam(value = "mediaUrl", required = false) String mediaUrl,
		@RequestParam(value = "questionType", required = false) LearningQuestionType questionType,
		@RequestParam(value = "optionsText", required = false) String optionsText,
		@RequestParam(value = "referenceAnswer", required = false) String referenceAnswer,
		@RequestParam(value = "maxScore", required = false) BigDecimal maxScore,
		@RequestParam(value = "required", required = false) Boolean required,
		@RequestParam(value = "sortOrder", required = false) Integer sortOrder,
		@RequestParam(value = "file", required = false) MultipartFile file) {
		return ResponseEntity.status(201).body(ApiResponse.created(courseLearningService.createTask(
			currentUser,
			courseId,
			pointId,
			title,
			description,
			taskType,
			materialType,
			contentText,
			mediaUrl,
			questionType,
			optionsText,
			referenceAnswer,
			maxScore,
			required,
			sortOrder,
			file)));
	}

	@GetMapping("/tasks/{taskId}/file")
	@Operation(summary = "下载学习任务文件")
	public ResponseEntity<InputStreamResource> accessFile(@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable UUID courseId,
		@PathVariable UUID taskId) {
		var file = courseLearningService.accessTaskFile(currentUser, courseId, taskId);
		MediaType contentType = parseMediaType(file.contentType());
		ContentDisposition disposition = ContentDisposition.attachment().filename(file.fileName()).build();
		return ResponseEntity.ok()
			.contentType(contentType)
			.contentLength(file.size())
			.header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
			.body(new InputStreamResource(file.inputStream()));
	}

	@GetMapping("/tasks/{taskId}/submissions")
	@Operation(summary = "查看学习任务提交记录")
	public ResponseEntity<ApiResponse<CourseLearningTaskSubmissionListResponse>> listSubmissions(@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable UUID courseId,
		@PathVariable UUID taskId) {
		return ResponseEntity.ok(ApiResponse.ok(new CourseLearningTaskSubmissionListResponse(courseLearningService.listTaskSubmissions(currentUser, courseId, taskId))));
	}

	@GetMapping("/tasks/{taskId}/submissions/latest")
	@Operation(summary = "查看当前用户最新学习任务提交")
	public ResponseEntity<ApiResponse<CourseLearningTaskSubmissionResponse>> latestSubmission(@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable UUID courseId,
		@PathVariable UUID taskId) {
		return ResponseEntity.ok(ApiResponse.ok(courseLearningService.getLatestSubmission(currentUser, courseId, taskId)));
	}

	@PostMapping(value = "/tasks/{taskId}/submissions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "提交学习任务答卷")
	public ResponseEntity<ApiResponse<CourseLearningTaskSubmissionResponse>> submit(@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable UUID courseId,
		@PathVariable UUID taskId,
		@RequestParam(value = "answerText", required = false) String answerText,
		@RequestParam(value = "file", required = false) MultipartFile file) {
		return ResponseEntity.status(201).body(ApiResponse.created(courseLearningService.submitTask(currentUser, courseId, taskId, answerText, file)));
	}

	@PatchMapping("/submissions/{submissionId}/grade")
	@Operation(summary = "教师批阅学习任务答卷")
	public ResponseEntity<ApiResponse<CourseLearningTaskSubmissionResponse>> grade(@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable UUID courseId,
		@PathVariable UUID submissionId,
		@Valid @RequestBody LearningTaskGradeRequest request) {
		return ResponseEntity.ok(ApiResponse.ok(courseLearningService.gradeSubmission(currentUser, courseId, submissionId, request)));
	}

	private MediaType parseMediaType(String rawType) {
		if (rawType == null || rawType.isBlank()) {
			return MediaType.APPLICATION_OCTET_STREAM;
		}
		try {
			return MediaType.parseMediaType(rawType);
		} catch (IllegalArgumentException ignored) {
			return MediaType.APPLICATION_OCTET_STREAM;
		}
	}
}