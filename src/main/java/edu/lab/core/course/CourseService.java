package edu.lab.core.course;

import edu.lab.core.common.exception.ConflictException;
import edu.lab.core.common.exception.ForbiddenException;
import edu.lab.core.common.exception.NotFoundException;
import edu.lab.core.course.dto.CourseCreateRequest;
import edu.lab.core.course.dto.CourseDetailResponse;
import edu.lab.core.course.dto.CourseJoinRequest;
import edu.lab.core.course.dto.CourseMemberResponse;
import edu.lab.core.course.dto.CourseSummaryResponse;
import edu.lab.core.experiment.ExperimentRepository;
import edu.lab.core.experiment.dto.ExperimentSummaryResponse;
import edu.lab.core.notification.HomeworkReminderService;
import edu.lab.core.security.AuthenticatedUser;
import edu.lab.core.user.AppUser;
import edu.lab.core.user.UserRole;
import edu.lab.core.user.UserRepository;
import edu.lab.core.user.dto.UserSummaryResponse;
import edu.lab.core.course.workspace.CourseWorkspaceService;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseService {

	private static final SecureRandom RANDOM = new SecureRandom();

	private final CourseRepository courseRepository;
	private final CourseMemberRepository courseMemberRepository;
	private final ExperimentRepository experimentRepository;
	private final UserRepository userRepository;
	private final CourseWorkspaceService workspaceService;
	private final HomeworkReminderService homeworkReminderService;

	@Transactional
	public CourseSummaryResponse createCourse(AuthenticatedUser currentUser, CourseCreateRequest request) {
		AppUser user = requireTeacher(currentUser.id());
		Course course = new Course();
		course.setTitle(request.title().trim());
		course.setDescription(request.description());
		course.setOwner(user);
		course.setInviteCode(generateUniqueInviteCode());
		course = courseRepository.save(course);
		workspaceService.ensureDefaultWorkspaceModules(course.getId());

		CourseMember member = new CourseMember();
		member.setCourse(course);
		member.setUser(user);
		member.setMemberRole(CourseMemberRole.TEACHER);
		courseMemberRepository.save(member);

		return toSummary(course, 1L);
	}

	@Transactional(readOnly = true)
	public List<CourseSummaryResponse> listCourses(AuthenticatedUser currentUser) {
		AppUser user = requireUser(currentUser.id());
		if (user.getRole() == UserRole.TEACHER || user.getRole() == UserRole.ADMIN) {
			return courseRepository.findOwnedCourses(user.getId()).stream()
				.map(course -> toSummary(course, courseMemberRepository.findMembers(course.getId()).size()))
				.toList();
		}

		return courseMemberRepository.findCoursesByMemberRole(user.getId(), CourseMemberRole.STUDENT).stream()
			.map(course -> toSummary(course, courseMemberRepository.findMembers(course.getId()).size()))
			.toList();
	}

	@Transactional(readOnly = true)
	public CourseDetailResponse getCourseDetail(AuthenticatedUser currentUser, UUID courseId) {
		Course course = requireAccessibleCourse(currentUser.id(), courseId);
		List<CourseMemberResponse> members = courseMemberRepository.findMembers(courseId).stream()
			.map(this::toMemberResponse)
			.toList();
		List<ExperimentSummaryResponse> experiments = experimentRepository.findByCourseIdOrderByPublishedAtDesc(courseId).stream()
			.map(experiment -> new ExperimentSummaryResponse(
				experiment.getId(),
				experiment.getCourse().getId(),
				experiment.getTitle(),
				experiment.getDescription(),
				experiment.getPublishedAt(),
				experiment.getDueAt(),
				experiment.getCreatedBy().getId(),
				experiment.getCreatedBy().getUsername()))
			.toList();
		return new CourseDetailResponse(
			course.getId(),
			course.getTitle(),
			course.getDescription(),
			course.getInviteCode(),
			toUserSummary(course.getOwner()),
			members,
			experiments,
			course.getCreatedAt()
		);
	}

	@Transactional
	public CourseSummaryResponse joinCourse(AuthenticatedUser currentUser, CourseJoinRequest request) {
		AppUser user = requireStudent(currentUser.id());
		Course course = courseRepository.findByInviteCodeIgnoreCase(request.inviteCode().trim().toUpperCase(Locale.ROOT))
			.orElseThrow(() -> new NotFoundException("课程不存在"));
		if (courseMemberRepository.existsByCourseIdAndUserId(course.getId(), user.getId())) {
			throw new ConflictException("已经加入过该课程");
		}

		CourseMember member = new CourseMember();
		member.setCourse(course);
		member.setUser(user);
		member.setMemberRole(CourseMemberRole.STUDENT);
		courseMemberRepository.save(member);
		homeworkReminderService.scheduleForStudentJoinedCourse(course.getId(), user.getId());
		return toSummary(course, courseMemberRepository.findMembers(course.getId()).size());
	}

	@Transactional(readOnly = true)
	public List<CourseMemberResponse> listMembers(AuthenticatedUser currentUser, UUID courseId) {
		requireOwnedCourse(currentUser.id(), courseId);
		return courseMemberRepository.findMembers(courseId).stream().map(this::toMemberResponse).toList();
	}

	@Transactional(readOnly = true)
	public Course requireOwnedCourse(UUID userId, UUID courseId) {
		Course course = courseRepository.findById(courseId)
			.orElseThrow(() -> new NotFoundException("课程不存在"));
		if (!course.getOwner().getId().equals(userId)) {
			throw new ForbiddenException("只有课程创建者可以执行该操作");
		}
		return course;
	}

	@Transactional(readOnly = true)
	public Course requireAccessibleCourse(UUID userId, UUID courseId) {
		Course course = courseRepository.findById(courseId)
			.orElseThrow(() -> new NotFoundException("课程不存在"));
		if (!course.getOwner().getId().equals(userId) && !courseMemberRepository.existsByCourseIdAndUserId(courseId, userId)) {
			throw new ForbiddenException("你不是该课程成员");
		}
		return course;
	}

	@Transactional(readOnly = true)
	public Course requireTeachingCourse(UUID userId, UUID courseId) {
		Course course = courseRepository.findById(courseId)
			.orElseThrow(() -> new NotFoundException("课程不存在"));
		if (!course.getOwner().getId().equals(userId)) {
			throw new ForbiddenException("只有课程创建者可以执行该操作");
		}
		return course;
	}

	@Transactional(readOnly = true)
	public boolean isTeacherOfCourse(UUID userId, UUID courseId) {
		return courseRepository.findById(courseId)
			.map(course -> course.getOwner().getId().equals(userId))
			.orElse(false);
	}

	private AppUser requireUser(UUID userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new NotFoundException("当前用户不存在"));
	}

	private AppUser requireTeacher(UUID userId) {
		AppUser user = requireUser(userId);
		if (user.getRole() != UserRole.TEACHER && user.getRole() != UserRole.ADMIN) {
			throw new ForbiddenException("只有教师可以执行该操作");
		}
		return user;
	}

	private AppUser requireStudent(UUID userId) {
		AppUser user = requireUser(userId);
		if (user.getRole() != UserRole.STUDENT) {
			throw new ForbiddenException("只有学生可以加入课程");
		}
		return user;
	}

	private String generateUniqueInviteCode() {
		for (int i = 0; i < 20; i++) {
			String candidate = randomCode(8);
			if (!courseRepository.existsByInviteCodeIgnoreCase(candidate)) {
				return candidate;
			}
		}
		throw new IllegalStateException("无法生成唯一的邀请码");
	}

	private String randomCode(int length) {
		byte[] bytes = new byte[length];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).substring(0, length).toUpperCase(Locale.ROOT);
	}

	private CourseSummaryResponse toSummary(Course course, long memberCount) {
		return new CourseSummaryResponse(
			course.getId(),
			course.getTitle(),
			course.getDescription(),
			course.getInviteCode(),
			course.getOwner().getId(),
			course.getOwner().getUsername(),
			memberCount,
			course.getCreatedAt()
		);
	}

	private CourseMemberResponse toMemberResponse(CourseMember member) {
		return new CourseMemberResponse(
			toUserSummary(member.getUser()),
			member.getMemberRole(),
			member.getCreatedAt()
		);
	}

	private UserSummaryResponse toUserSummary(AppUser user) {
		return new UserSummaryResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getEmail(), user.getRole());
	}
}