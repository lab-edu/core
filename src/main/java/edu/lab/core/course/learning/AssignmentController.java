package edu.lab.core.course.learning;

import edu.lab.core.course.learning.dto.AssignmentCreateRequest;
import edu.lab.core.course.learning.dto.AssignmentResponse;
import edu.lab.core.course.learning.dto.AssignmentSubmissionRequest;
import edu.lab.core.course.learning.dto.AssignmentSubmissionResponse;
import edu.lab.core.course.learning.dto.AssignmentUpdateRequest;
import edu.lab.core.course.learning.dto.AssignmentGradeRequest;
import edu.lab.core.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

@RestController
@RequestMapping("/api/v1/courses/{courseId}/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    @GetMapping
    public List<AssignmentResponse> listAssignments(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        return assignmentService.listAssignments(courseId, authUser);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssignmentResponse createAssignment(
            @PathVariable UUID courseId,
            @RequestBody AssignmentCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        return assignmentService.createAssignment(courseId, request, authUser);
    }

    @GetMapping("/{assignmentId}")
    public AssignmentResponse getAssignment(
            @PathVariable UUID courseId,
            @PathVariable UUID assignmentId,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        return assignmentService.getAssignment(courseId, assignmentId, authUser);
    }

    @PutMapping("/{assignmentId}")
    public AssignmentResponse updateAssignment(
            @PathVariable UUID courseId,
            @PathVariable UUID assignmentId,
            @RequestBody AssignmentUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        return assignmentService.updateAssignment(courseId, assignmentId, request, authUser);
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
    @ResponseStatus(HttpStatus.CREATED)
    public AssignmentSubmissionResponse submitAssignment(
            @PathVariable UUID courseId,
            @PathVariable UUID assignmentId,
            @RequestBody AssignmentSubmissionRequest request,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        return assignmentService.submitAssignment(courseId, assignmentId, request, authUser);
    }

    @GetMapping("/{assignmentId}/submissions")
    public List<AssignmentSubmissionResponse> listSubmissions(
            @PathVariable UUID courseId,
            @PathVariable UUID assignmentId,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        return assignmentService.listSubmissions(courseId, assignmentId, authUser);
    }

    @GetMapping("/{assignmentId}/submissions/latest")
    public AssignmentSubmissionResponse getLatestSubmission(
            @PathVariable UUID courseId,
            @PathVariable UUID assignmentId,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        // 简化为获取用户的最新提交
        List<AssignmentSubmissionResponse> submissions = assignmentService.listSubmissions(courseId, assignmentId, authUser);
        return submissions.stream()
                .filter(AssignmentSubmissionResponse::latest)
                .findFirst()
                .orElse(null);
    }

    @PatchMapping("/submissions/{submissionId}/grade")
    public AssignmentSubmissionResponse gradeSubmission(
            @PathVariable UUID courseId,
            @PathVariable UUID submissionId,
            @RequestBody AssignmentGradeRequest request,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        return assignmentService.gradeSubmission(courseId, submissionId, request, authUser);
    }
}