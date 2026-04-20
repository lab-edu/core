package edu.lab.core.notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InboxNotificationRepository extends JpaRepository<InboxNotification, UUID> {

	@Query("select n from InboxNotification n where n.user.id = :userId order by n.deliveredAt desc")
	List<InboxNotification> findByUserIdOrderByDeliveredAtDesc(@Param("userId") UUID userId);

	@Query("select n from InboxNotification n where n.user.id = :userId and n.readAt is null order by n.deliveredAt desc")
	List<InboxNotification> findUnreadByUserIdOrderByDeliveredAtDesc(@Param("userId") UUID userId);

	@Modifying
	@Query("update InboxNotification n set n.readAt = :readAt where n.user.id = :userId and n.readAt is null")
	int markAllRead(@Param("userId") UUID userId, @Param("readAt") LocalDateTime readAt);
}