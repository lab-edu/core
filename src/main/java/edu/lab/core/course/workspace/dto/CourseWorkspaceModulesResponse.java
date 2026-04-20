package edu.lab.core.course.workspace.dto;

import java.util.List;
import java.util.UUID;

public record CourseWorkspaceModulesResponse(
	UUID courseId,
	List<CourseWorkspaceModuleItemResponse> modules
) {
}