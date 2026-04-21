package edu.lab.core.course.learning;

import edu.lab.core.domain.AuditableEntity;
import edu.lab.core.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "course_learning_tasks")
public class CourseLearningTask extends AuditableEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "point_id", nullable = false)
	private CourseLearningPoint knowledgePoint;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "created_by", nullable = false)
	private AppUser createdBy;

	@Column(nullable = false, length = 120)
	private String title;

	@Column(columnDefinition = "text")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "task_type", nullable = false, length = 20)
	private LearningTaskType taskType;

	@Enumerated(EnumType.STRING)
	@Column(name = "task_kind", nullable = false, length = 20)
	private LearningTaskKind taskKind;

	@Enumerated(EnumType.STRING)
	@Column(name = "material_type", length = 20)
	private LearningMaterialType materialType;

	@Column(name = "content_text", columnDefinition = "text")
	private String contentText;

	@Column(name = "media_url", length = 500)
	private String mediaUrl;

	@Column(name = "file_name", length = 255)
	private String fileName;

	@Column(name = "file_path", length = 500)
	private String filePath;

	@Column(name = "content_type", length = 255)
	private String contentType;

	@Enumerated(EnumType.STRING)
	@Column(name = "question_type", length = 20)
	private LearningQuestionType questionType;

	@Column(name = "options_json", columnDefinition = "text")
	private String optionsJson;

	@Column(name = "reference_answer", columnDefinition = "text")
	private String referenceAnswer;

	@Column(name = "max_score", nullable = false, precision = 10, scale = 2)
	private BigDecimal maxScore;

	@Column(name = "start_at")
	private LocalDateTime startAt;

	@Column(name = "due_at")
	private LocalDateTime dueAt;

	@Column(name = "notify_on_start", nullable = false)
	private boolean notifyOnStart;

	@Column(name = "notify_before_due_24h", nullable = false)
	private boolean notifyBeforeDue24h;

	@Column(name = "notify_on_due", nullable = false)
	private boolean notifyOnDue;

	@Column(nullable = false)
	private boolean required;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Column(name = "migrated_to_assignment_id")
	private UUID migratedToAssignmentId;

	@Column(name = "migrated_to_item_id")
	private UUID migratedToItemId;
}