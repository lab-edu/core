package edu.lab.core.course.learning;

import edu.lab.core.common.api.ApiResponse;
import edu.lab.core.course.learning.dto.CourseHomeworkListResponse;
import edu.lab.core.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/homeworks")
@RequiredArgsConstructor
public class HomeworkController {

	private final CourseLearningService courseLearningService;

	@GetMapping("/mine")
	@Operation(summary = "我的作业汇总")
	public ResponseEntity<ApiResponse<CourseHomeworkListResponse>> mine(@AuthenticationPrincipal AuthenticatedUser currentUser) {
		return ResponseEntity.ok(ApiResponse.ok(new CourseHomeworkListResponse(courseLearningService.listMyHomeworksAcrossCourses(currentUser))));
	}
}
