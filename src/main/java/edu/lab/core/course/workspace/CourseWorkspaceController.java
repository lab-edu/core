package edu.lab.core.course.workspace;

import edu.lab.core.common.api.ApiResponse;
import edu.lab.core.course.CourseService;
import edu.lab.core.course.workspace.dto.CourseWorkspaceModulesResponse;
import edu.lab.core.course.workspace.dto.CourseWorkspaceModulesUpdateRequest;
import edu.lab.core.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/workspace/modules")
@RequiredArgsConstructor
public class CourseWorkspaceController {

	private final CourseWorkspaceService workspaceService;
	private final CourseService courseService;

	@GetMapping
	@Operation(summary = "获取课程工作区模块配置")
	public ResponseEntity<ApiResponse<CourseWorkspaceModulesResponse>> list(@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable UUID courseId) {
		courseService.requireAccessibleCourse(currentUser.id(), courseId);
		return ResponseEntity.ok(ApiResponse.ok(workspaceService.listModules(courseId)));
	}

	@PutMapping
	@Operation(summary = "更新课程工作区模块配置")
	public ResponseEntity<ApiResponse<CourseWorkspaceModulesResponse>> update(@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable UUID courseId,
		@Valid @RequestBody CourseWorkspaceModulesUpdateRequest request) {
		courseService.requireTeachingCourse(currentUser.id(), courseId);
		return ResponseEntity.ok(ApiResponse.ok(workspaceService.updateModules(courseId, request)));
	}
}