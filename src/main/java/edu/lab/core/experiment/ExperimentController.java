package edu.lab.core.experiment;

import edu.lab.core.common.api.ApiResponse;

import edu.lab.core.experiment.dto.ExperimentCreateRequest;
import edu.lab.core.experiment.dto.ExperimentCreateResponse;
import edu.lab.core.experiment.dto.ExperimentDetailResponse;
import edu.lab.core.experiment.dto.ExperimentListResponse;
import edu.lab.core.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/experiments")
@RequiredArgsConstructor
public class ExperimentController {

	private final ExperimentService experimentService;

	@PostMapping
	@Operation(summary = "教师发布实验")
	public ResponseEntity<ApiResponse<ExperimentCreateResponse>> createExperiment(@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable UUID courseId,
		@Valid @RequestBody ExperimentCreateRequest request) {
		return ResponseEntity.status(201).body(ApiResponse.created(new ExperimentCreateResponse(experimentService.createExperiment(currentUser, courseId, request))));
	}

	@GetMapping
	@Operation(summary = "获取课程下实验列表")
	public ResponseEntity<ApiResponse<ExperimentListResponse>> listExperiments(@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable UUID courseId) {
		return ResponseEntity.ok(ApiResponse.ok(new ExperimentListResponse(experimentService.listExperiments(currentUser, courseId))));
	}

	@GetMapping("/{experimentId}")
	@Operation(summary = "获取实验详情")
	public ResponseEntity<ApiResponse<ExperimentDetailResponse>> getExperimentDetail(@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable UUID courseId,
		@PathVariable UUID experimentId) {
		return ResponseEntity.ok(ApiResponse.ok(experimentService.getExperimentDetail(currentUser, experimentId)));
	}
}