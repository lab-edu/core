package edu.lab.core.notification;

import edu.lab.core.course.CourseMember;
import edu.lab.core.course.CourseMemberRepository;
import edu.lab.core.course.CourseMemberRole;
import edu.lab.core.course.learning.CourseLearningTask;
import edu.lab.core.course.learning.CourseLearningTaskRepository;
import edu.lab.core.course.learning.LearningTaskKind;
import edu.lab.core.user.AppUser;
import edu.lab.core.user.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HomeworkReminderService {

	private final HomeworkReminderEventRepository reminderEventRepository;
	private final CourseMemberRepository courseMemberRepository;
	private final CourseLearningTaskRepository taskRepository;
	private final UserRepository userRepository;
	private final NotificationService notificationService;

	@Transactional
	public void scheduleForHomeworkTask(CourseLearningTask task) {
		if (task.getTaskKind() != LearningTaskKind.HOMEWORK) {
			return;
		}
		UUID courseId = task.getKnowledgePoint().getUnit().getCourse().getId();
		List<CourseMember> members = courseMemberRepository.findMembers(courseId);
		for (CourseMember member : members) {
			if (member.getMemberRole() == CourseMemberRole.STUDENT) {
				scheduleEvents(task, member.getUser());
			}
		}
	}

	@Transactional
	public void scheduleForStudentJoinedCourse(UUID courseId, UUID studentId) {
		AppUser student = userRepository.findById(studentId).orElse(null);
		if (student == null) {
			return;
		}
		for (CourseLearningTask task : taskRepository.findHomeworkByCourseId(courseId)) {
			scheduleEvents(task, student);
		}
	}

	@Scheduled(fixedDelayString = "${lab.notifications.reminder-scan-ms:60000}")
	@Transactional
	public void dispatchDueEvents() {
		LocalDateTime now = LocalDateTime.now();
		List<HomeworkReminderEvent> dueEvents = reminderEventRepository.findDueEvents(now);
		for (HomeworkReminderEvent event : dueEvents) {
			if (event.getSentAt() != null || event.isCanceled()) {
				continue;
			}
			CourseLearningTask task = event.getTask();
			String title = switch (event.getTriggerType()) {
				case START -> "作业已开始";
				case BEFORE_DUE_24H -> "作业将在 24 小时后截止";
				case DUE -> "作业已截止";
			};
			String content = "%s - %s".formatted(task.getKnowledgePoint().getUnit().getCourse().getTitle(), task.getTitle());
			String actionPath = "/homeworks";
			notificationService.createHomeworkReminderNotification(event.getTargetUser(), title, content, actionPath);
			event.setSentAt(now);
			reminderEventRepository.save(event);
		}
	}

	private void scheduleEvents(CourseLearningTask task, AppUser student) {
		List<EventSchedule> schedules = new ArrayList<>();
		if (task.isNotifyOnStart() && task.getStartAt() != null) {
			schedules.add(new EventSchedule(HomeworkReminderTriggerType.START, task.getStartAt()));
		}
		if (task.isNotifyBeforeDue24h() && task.getDueAt() != null) {
			schedules.add(new EventSchedule(HomeworkReminderTriggerType.BEFORE_DUE_24H, task.getDueAt().minusHours(24)));
		}
		if (task.isNotifyOnDue() && task.getDueAt() != null) {
			schedules.add(new EventSchedule(HomeworkReminderTriggerType.DUE, task.getDueAt()));
		}

		LocalDateTime now = LocalDateTime.now();
		for (EventSchedule schedule : schedules) {
			LocalDateTime scheduledAt = schedule.scheduledAt().isBefore(now) ? now : schedule.scheduledAt();
			HomeworkReminderEvent event = reminderEventRepository.findByTaskIdAndTargetUserIdAndTriggerType(task.getId(), student.getId(), schedule.triggerType())
				.orElseGet(HomeworkReminderEvent::new);
			event.setTask(task);
			event.setTargetUser(student);
			event.setTriggerType(schedule.triggerType());
			event.setScheduledAt(scheduledAt);
			event.setCanceled(false);
			if (event.getSentAt() != null && event.getSentAt().isBefore(now.minusMinutes(1))) {
				continue;
			}
			reminderEventRepository.save(event);
		}
	}

	private record EventSchedule(HomeworkReminderTriggerType triggerType, LocalDateTime scheduledAt) {
	}
}