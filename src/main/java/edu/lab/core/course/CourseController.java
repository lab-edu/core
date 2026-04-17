package edu.lab.core.course;

import edu.lab.core.common.api.ApiResponse;
import edu.lab.core.course.dto.CourseCreateRequest;
import edu.lab.core.course.dto.CourseCreateResponse;
import edu.lab.core.course.dto.CourseDetailResponse;
import edu.lab.core.course.dto.CourseJoinRequest;
import edu.lab.core.course.dto.CourseJoinResponse;
import edu.lab.core.course.dto.CourseListResponse;
import edu.lab.core.course.dto.CourseMembersResponse;
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
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

	private final CourseService courseService;

	@PostMapping
	@Operation(summary = "教师创建课程")
	public ResponseEntity<ApiResponse<CourseCreateResponse>> createCourse(@AuthenticationPrincipal AuthenticatedUser currentUser,
		@Valid @RequestBody CourseCreateRequest request) {
		return ResponseEntity.status(201).body(ApiResponse.created(new CourseCreateResponse(courseService.createCourse(currentUser, request))));
	}

	@GetMapping
	@Operation(summary = "获取课程列表")
	public ResponseEntity<ApiResponse<CourseListResponse>> listCourses(@AuthenticationPrincipal AuthenticatedUser currentUser) {
		return ResponseEntity.ok(ApiResponse.ok(new CourseListResponse(courseService.listCourses(currentUser))));
	}

	@GetMapping("/{courseId}")
	@Operation(summary = "获取课程详情")
	public ResponseEntity<ApiResponse<CourseDetailResponse>> getCourseDetail(@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable UUID courseId) {
		return ResponseEntity.ok(ApiResponse.ok(courseService.getCourseDetail(currentUser, courseId)));
	}

	@PostMapping("/join")
	@Operation(summary = "学生加入课程")
	public ResponseEntity<ApiResponse<CourseJoinResponse>> joinCourse(@AuthenticationPrincipal AuthenticatedUser currentUser,
		@Valid @RequestBody CourseJoinRequest request) {
		return ResponseEntity.ok(ApiResponse.ok(new CourseJoinResponse(courseService.joinCourse(currentUser, request))));
	}

	@GetMapping("/{courseId}/members")
	@Operation(summary = "查看课程成员")
	public ResponseEntity<ApiResponse<CourseMembersResponse>> listMembers(@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable UUID courseId) {
		return ResponseEntity.ok(ApiResponse.ok(new CourseMembersResponse(courseService.listMembers(currentUser, courseId))));
	}
}