package edu.lab.core.resource;

import edu.lab.core.course.Course;
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
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "resources")
public class TeachingResource extends AuditableEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "course_id", nullable = false)
	private Course course;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "uploaded_by", nullable = false)
	private AppUser uploadedBy;

	@Column(nullable = false, length = 120)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ResourceType type;

	@Column(length = 64)
	private String category;

	@Column(name = "file_name", length = 255)
	private String fileName;

	@Column(name = "file_path", length = 500)
	private String filePath;

	@Column(name = "content_type", length = 255)
	private String contentType;

	@Column(name = "external_url", length = 500)
	private String externalUrl;

	@Column(name = "uploaded_at", nullable = false)
	private LocalDateTime uploadedAt;
}