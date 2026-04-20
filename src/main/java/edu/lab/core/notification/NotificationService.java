package edu.lab.core.notification;

import edu.lab.core.common.exception.ForbiddenException;
import edu.lab.core.common.exception.NotFoundException;
import edu.lab.core.notification.dto.InboxNotificationItemResponse;
import edu.lab.core.security.AuthenticatedUser;
import edu.lab.core.user.AppUser;
import edu.lab.core.user.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

	private final InboxNotificationRepository notificationRepository;
	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public List<InboxNotificationItemResponse> list(AuthenticatedUser currentUser, boolean unreadOnly) {
		requireUser(currentUser.id());
		List<InboxNotification> notifications = unreadOnly
			? notificationRepository.findUnreadByUserIdOrderByDeliveredAtDesc(currentUser.id())
			: notificationRepository.findByUserIdOrderByDeliveredAtDesc(currentUser.id());
		return notifications.stream().map(this::toItem).toList();
	}

	@Transactional
	public InboxNotificationItemResponse markRead(AuthenticatedUser currentUser, UUID notificationId) {
		InboxNotification notification = notificationRepository.findById(notificationId)
			.orElseThrow(() -> new NotFoundException("通知不存在"));
		if (!notification.getUser().getId().equals(currentUser.id())) {
			throw new ForbiddenException("无权操作该通知");
		}
		if (notification.getReadAt() == null) {
			notification.setReadAt(LocalDateTime.now());
			notification = notificationRepository.save(notification);
		}
		return toItem(notification);
	}

	@Transactional
	public int markAllRead(AuthenticatedUser currentUser) {
		requireUser(currentUser.id());
		return notificationRepository.markAllRead(currentUser.id(), LocalDateTime.now());
	}

	@Transactional
	public InboxNotification createHomeworkReminderNotification(AppUser user, String title, String content, String actionPath) {
		InboxNotification notification = new InboxNotification();
		notification.setUser(user);
		notification.setNotificationType(NotificationType.HOMEWORK_REMINDER);
		notification.setTitle(title);
		notification.setContent(content);
		notification.setActionPath(actionPath);
		notification.setDeliveredAt(LocalDateTime.now());
		return notificationRepository.save(notification);
	}

	private AppUser requireUser(UUID userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new NotFoundException("当前用户不存在"));
	}

	private InboxNotificationItemResponse toItem(InboxNotification notification) {
		return new InboxNotificationItemResponse(
			notification.getId(),
			notification.getNotificationType(),
			notification.getTitle(),
			notification.getContent(),
			notification.getActionPath(),
			notification.getDeliveredAt(),
			notification.getReadAt()
		);
	}
}