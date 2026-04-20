package edu.lab.core.notification;

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
@Table(name = "inbox_notifications")
public class InboxNotification extends AuditableEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private AppUser user;

	@Enumerated(EnumType.STRING)
	@Column(name = "notification_type", nullable = false, length = 40)
	private NotificationType notificationType;

	@Column(nullable = false, length = 120)
	private String title;

	@Column(columnDefinition = "text", nullable = false)
	private String content;

	@Column(name = "action_path", length = 500)
	private String actionPath;

	@Column(name = "delivered_at", nullable = false)
	private LocalDateTime deliveredAt;

	@Column(name = "read_at")
	private LocalDateTime readAt;
}