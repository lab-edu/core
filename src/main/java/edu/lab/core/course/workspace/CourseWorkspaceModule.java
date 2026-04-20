package edu.lab.core.course.workspace;

import edu.lab.core.course.Course;
import edu.lab.core.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "course_workspace_modules")
public class CourseWorkspaceModule extends AuditableEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "course_id", nullable = false)
	private Course course;

	@Enumerated(EnumType.STRING)
	@Column(name = "module_key", nullable = false, length = 40)
	private CourseWorkspaceModuleKey moduleKey;

	@Column(nullable = false)
	private boolean enabled;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;
}