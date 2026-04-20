package edu.lab.core.course.workspace.dto;

import edu.lab.core.course.workspace.CourseWorkspaceModuleKey;

public record CourseWorkspaceModuleItemResponse(
	CourseWorkspaceModuleKey moduleKey,
	boolean enabled,
	int sortOrder
) {
}