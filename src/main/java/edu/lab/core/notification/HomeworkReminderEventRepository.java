package edu.lab.core.notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HomeworkReminderEventRepository extends JpaRepository<HomeworkReminderEvent, UUID> {

	Optional<HomeworkReminderEvent> findByTaskIdAndTargetUserIdAndTriggerType(UUID taskId, UUID targetUserId, HomeworkReminderTriggerType triggerType);

	@Query("select e from HomeworkReminderEvent e join fetch e.task t join fetch t.knowledgePoint p join fetch p.unit u join fetch u.course join fetch e.targetUser where e.canceled = false and e.sentAt is null and e.scheduledAt <= :now order by e.scheduledAt asc")
	List<HomeworkReminderEvent> findDueEvents(@Param("now") LocalDateTime now);
}