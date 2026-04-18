package edu.lab.core.course.announcement;

import edu.lab.core.common.exception.NotFoundException;
import edu.lab.core.course.CourseService;
import edu.lab.core.course.announcement.dto.CourseAnnouncementCreateRequest;
import edu.lab.core.course.announcement.dto.CourseAnnouncementSummaryResponse;
import edu.lab.core.security.AuthenticatedUser;
import edu.lab.core.user.AppUser;
import edu.lab.core.user.UserRepository;
import edu.lab.core.user.dto.UserSummaryResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseAnnouncementService {

	private final CourseAnnouncementRepository courseAnnouncementRepository;
	private final CourseService courseService;
	private final UserRepository userRepository;

	@Transactional
	public CourseAnnouncementSummaryResponse createAnnouncement(AuthenticatedUser currentUser, UUID courseId, CourseAnnouncementCreateRequest request) {
		var course = courseService.requireTeachingCourse(currentUser.id(), courseId);
		AppUser creator = requireUser(currentUser.id());

		CourseAnnouncement announcement = new CourseAnnouncement();
		announcement.setCourse(course);
		announcement.setCreatedBy(creator);
		announcement.setTitle(request.title().trim());
		announcement.setContent(request.content().trim());
		announcement = courseAnnouncementRepository.save(announcement);

		return toSummary(announcement);
	}

	@Transactional(readOnly = true)
	public List<CourseAnnouncementSummaryResponse> listAnnouncements(AuthenticatedUser currentUser, UUID courseId) {
		courseService.requireAccessibleCourse(currentUser.id(), courseId);
		return courseAnnouncementRepository.findByCourseIdOrderByCreatedAtDesc(courseId).stream().map(this::toSummary).toList();
	}

	private AppUser requireUser(UUID userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new NotFoundException("当前用户不存在"));
	}

	private CourseAnnouncementSummaryResponse toSummary(CourseAnnouncement announcement) {
		return new CourseAnnouncementSummaryResponse(
			announcement.getId(),
			announcement.getCourse().getId(),
			announcement.getTitle(),
			announcement.getContent(),
			new UserSummaryResponse(
				announcement.getCreatedBy().getId(),
				announcement.getCreatedBy().getUsername(),
				announcement.getCreatedBy().getDisplayName(),
				announcement.getCreatedBy().getEmail(),
				announcement.getCreatedBy().getRole()),
			announcement.getCreatedAt()
		);
	}
}