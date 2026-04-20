package edu.lab.core.course.workspace.dto;

import edu.lab.core.course.workspace.CourseWorkspaceModuleKey;

public record CourseWorkspaceModuleItemRequest(
	CourseWorkspaceModuleKey moduleKey,
	boolean enabled,
	Integer sortOrder
) {
}