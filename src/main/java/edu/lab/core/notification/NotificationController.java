package edu.lab.core.notification;

import edu.lab.core.common.api.ApiResponse;
import edu.lab.core.notification.dto.InboxNotificationItemResponse;
import edu.lab.core.notification.dto.InboxNotificationListResponse;
import edu.lab.core.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

	private final NotificationService notificationService;

	@GetMapping
	@Operation(summary = "站内通知列表")
	public ResponseEntity<ApiResponse<InboxNotificationListResponse>> list(@AuthenticationPrincipal AuthenticatedUser currentUser,
		@RequestParam(defaultValue = "false") boolean unreadOnly) {
		return ResponseEntity.ok(ApiResponse.ok(new InboxNotificationListResponse(notificationService.list(currentUser, unreadOnly))));
	}

	@PatchMapping("/{notificationId}/read")
	@Operation(summary = "标记通知为已读")
	public ResponseEntity<ApiResponse<InboxNotificationItemResponse>> markRead(@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable UUID notificationId) {
		return ResponseEntity.ok(ApiResponse.ok(notificationService.markRead(currentUser, notificationId)));
	}

	@PatchMapping("/read-all")
	@Operation(summary = "全部通知已读")
	public ResponseEntity<ApiResponse<Integer>> markAllRead(@AuthenticationPrincipal AuthenticatedUser currentUser) {
		return ResponseEntity.ok(ApiResponse.ok(notificationService.markAllRead(currentUser)));
	}
}