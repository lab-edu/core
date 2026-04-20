package edu.lab.core.course.learning;

import edu.lab.core.domain.AuditableEntity;
import edu.lab.core.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "course_learning_task_submissions")
public class CourseLearningTaskSubmission extends AuditableEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "task_id", nullable = false)
	private CourseLearningTask task;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private AppUser submittedBy;

	@Column(name = "answer_text", columnDefinition = "text")
	private String answerText;

	@Column(name = "file_name", length = 255)
	private String fileName;

	@Column(name = "file_path", length = 500)
	private String filePath;

	@Column(name = "content_type", length = 255)
	private String contentType;

	@Column(precision = 10, scale = 2)
	private BigDecimal score;

	@Column(columnDefinition = "text")
	private String feedback;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "graded_by")
	private AppUser gradedBy;

	@Column(name = "graded_at")
	private LocalDateTime gradedAt;

	@Column(nullable = false)
	private boolean latest;

	@Column(name = "submitted_at", nullable = false)
	private LocalDateTime submittedAt;
}