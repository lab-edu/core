package edu.lab.core.course.workspace.dto;

import java.util.List;

public record CourseWorkspaceModulesUpdateRequest(List<CourseWorkspaceModuleItemRequest> modules) {
}