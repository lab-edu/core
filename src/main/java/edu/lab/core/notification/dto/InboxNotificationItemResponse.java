package edu.lab.core.notification.dto;

import edu.lab.core.notification.NotificationType;
import java.time.LocalDateTime;
import java.util.UUID;

public record InboxNotificationItemResponse(
	UUID id,
	NotificationType notificationType,
	String title,
	String content,
	String actionPath,
	LocalDateTime deliveredAt,
	LocalDateTime readAt
) {
}