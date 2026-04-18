package edu.lab.core.course.announcement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CourseAnnouncementCreateRequest(
	@NotBlank(message = "公告标题不能为空")
	@Size(max = 120, message = "公告标题长度不能超过 120")
	String title,

	@NotBlank(message = "公告内容不能为空")
	String content
) {
}