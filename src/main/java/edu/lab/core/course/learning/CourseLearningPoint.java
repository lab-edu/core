package edu.lab.core.course.learning;

import edu.lab.core.domain.AuditableEntity;
import edu.lab.core.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "course_learning_points")
public class CourseLearningPoint extends AuditableEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "unit_id", nullable = false)
	private CourseLearningUnit unit;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "created_by", nullable = false)
	private AppUser createdBy;

	@Column(nullable = false, length = 120)
	private String title;

	@Column(columnDefinition = "text")
	private String summary;

	@Column(name = "estimated_minutes")
	private Integer estimatedMinutes;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;
}