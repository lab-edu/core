package edu.lab.core.course.announcement;

import edu.lab.core.common.api.ApiResponse;
import edu.lab.core.course.announcement.dto.CourseAnnouncementCreateRequest;
import edu.lab.core.course.announcement.dto.CourseAnnouncementCreateResponse;
import edu.lab.core.course.announcement.dto.CourseAnnouncementListResponse;
import edu.lab.core.course.announcement.dto.CourseAnnouncementSummaryResponse;
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
@RequestMapping("/api/v1/courses/{courseId}/announcements")
@RequiredArgsConstructor
public class CourseAnnouncementController {

	private final CourseAnnouncementService courseAnnouncementService;

	@PostMapping
	@Operation(summary = "教师发布课堂公告")
	public ResponseEntity<ApiResponse<CourseAnnouncementCreateResponse>> createAnnouncement(@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable UUID courseId,
		@Valid @RequestBody CourseAnnouncementCreateRequest request) {
		CourseAnnouncementSummaryResponse announcement = courseAnnouncementService.createAnnouncement(currentUser, courseId, request);
		return ResponseEntity.status(201).body(ApiResponse.created(new CourseAnnouncementCreateResponse(announcement)));
	}

	@GetMapping
	@Operation(summary = "获取课程公告列表")
	public ResponseEntity<ApiResponse<CourseAnnouncementListResponse>> listAnnouncements(@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable UUID courseId) {
		return ResponseEntity.ok(ApiResponse.ok(new CourseAnnouncementListResponse(courseAnnouncementService.listAnnouncements(currentUser, courseId))));
	}
}