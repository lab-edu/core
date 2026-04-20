package edu.lab.core.notification.dto;

import java.util.List;

public record InboxNotificationListResponse(List<InboxNotificationItemResponse> items) {
}