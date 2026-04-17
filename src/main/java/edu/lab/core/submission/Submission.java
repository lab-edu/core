package edu.lab.core.submission;

import edu.lab.core.domain.AuditableEntity;
import edu.lab.core.experiment.Experiment;
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
@Table(name = "submissions")
public class Submission extends AuditableEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "experiment_id", nullable = false)
	private Experiment experiment;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private AppUser submittedBy;

	@Column(name = "file_name", nullable = false, length = 255)
	private String fileName;

	@Column(name = "file_path", nullable = false, length = 500)
	private String filePath;

	@Column(name = "content_type", length = 255)
	private String contentType;

	@Column(columnDefinition = "text")
	private String note;

	@Column(precision = 10, scale = 2)
	private BigDecimal score;

	@Column(columnDefinition = "text")
	private String feedback;

	@Column(nullable = false)
	private boolean latest;

	@Column(name = "submitted_at", nullable = false)
	private LocalDateTime submittedAt;
}