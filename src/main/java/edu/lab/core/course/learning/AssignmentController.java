package edu.lab.core.course.learning;

import edu.lab.core.course.learning.dto.AssignmentCreateRequest;
import edu.lab.core.course.learning.dto.AssignmentResponse;
import edu.lab.core.course.learning.dto.AssignmentSubmissionRequest;
import edu.lab.core.course.learning.dto.AssignmentSubmissionResponse;
import edu.lab.core.course.learning.dto.AssignmentUpdateRequest;
import edu.lab.core.course.learning.dto.AssignmentGradeRequest;
import edu.lab.core.common.api.ApiResponse;
import edu.lab.core.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/assignments")
@RequiredArgsConstructor
@Validated
public class AssignmentController {

    private final AssignmentService assignmentService;
    private static final Logger logger = LoggerFactory.getLogger(AssignmentController.class);

    @GetMapping
    public ResponseEntity<ApiResponse<List<AssignmentResponse>>> listAssignments(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        return ResponseEntity.ok(ApiResponse.ok(assignmentService.listAssignments(courseId, authUser)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AssignmentResponse>> createAssignment(
            @PathVariable UUID courseId,
            @Valid @RequestBody AssignmentCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(
                assignmentService.createAssignment(courseId, request, authUser)
        ));
    }

    @GetMapping("/{assignmentId}")
    public ResponseEntity<ApiResponse<AssignmentResponse>> getAssignment(
            @PathVariable UUID courseId,
            @PathVariable UUID assignmentId,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        AssignmentResponse resp = assignmentService.getAssignment(courseId, assignmentId, authUser);
        try {
            logger.debug("getAssignment: courseId={}, assignmentId={}, title={}, taskItems={}",
                    courseId, assignmentId, resp.title(), resp.taskItems() == null ? 0 : resp.taskItems().size());
        } catch (Exception e) {
            // ignore logging errors
        }
        return ResponseEntity.ok(ApiResponse.ok(resp));
    }

    @PutMapping("/{assignmentId}")
    public ResponseEntity<ApiResponse<AssignmentResponse>> updateAssignment(
            @PathVariable UUID courseId,
            @PathVariable UUID assignmentId,
            @Valid @RequestBody AssignmentUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        return ResponseEntity.ok(ApiResponse.ok(assignmentService.updateAssignment(courseId, assignmentId, request, authUser)));
    }

    @DeleteMapping("/{assignmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAssignment(
            @PathVariable UUID courseId,
            @PathVariable UUID assignmentId,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        assignmentService.deleteAssignment(courseId, assignmentId, authUser);
    }

    @PostMapping("/{assignmentId}/submissions")
    public ResponseEntity<ApiResponse<AssignmentSubmissionResponse>> submitAssignment(
            @PathVariable UUID courseId,
            @PathVariable UUID assignmentId,
            @Valid @RequestBody AssignmentSubmissionRequest request,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(
                assignmentService.submitAssignment(courseId, assignmentId, request, authUser)
        ));
    }

    @GetMapping("/{assignmentId}/submissions")
    public ResponseEntity<ApiResponse<List<AssignmentSubmissionResponse>>> listSubmissions(
            @PathVariable UUID courseId,
            @PathVariable UUID assignmentId,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        return ResponseEntity.ok(ApiResponse.ok(assignmentService.listSubmissions(courseId, assignmentId, authUser)));
    }

        @GetMapping("/{assignmentId}/submissions/latest")
        public ResponseEntity<ApiResponse<AssignmentSubmissionResponse>> getLatestSubmission(
            @PathVariable UUID courseId,
            @PathVariable UUID assignmentId,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        // 简化为获取用户的最新提交
        List<AssignmentSubmissionResponse> submissions = assignmentService.listSubmissions(courseId, assignmentId, authUser);
        AssignmentSubmissionResponse latest = submissions.stream()
            .filter(AssignmentSubmissionResponse::latest)
            .findFirst()
            .orElse(null);
        return ResponseEntity.ok(ApiResponse.ok(latest));
        }

    @PatchMapping("/submissions/{submissionId}/grade")
    public ResponseEntity<ApiResponse<AssignmentSubmissionResponse>> gradeSubmission(
            @PathVariable UUID courseId,
            @PathVariable UUID submissionId,
            @Valid @RequestBody AssignmentGradeRequest request,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        return ResponseEntity.ok(ApiResponse.ok(assignmentService.gradeSubmission(courseId, submissionId, request, authUser)));
    }
}