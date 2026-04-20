package edu.lab.core.course.learning;

import edu.lab.core.common.exception.BadRequestException;
import edu.lab.core.common.exception.ForbiddenException;
import edu.lab.core.common.exception.NotFoundException;
import edu.lab.core.course.Course;
import edu.lab.core.course.CourseRepository;
import edu.lab.core.course.CourseMemberRepository;
import edu.lab.core.course.CourseMemberRole;
import edu.lab.core.course.CourseService;
import edu.lab.core.course.learning.dto.CourseLearningDetailResponse;
import edu.lab.core.course.learning.dto.CourseHomeworkItemResponse;
import edu.lab.core.course.learning.dto.CourseLearningOverviewResponse;
import edu.lab.core.course.learning.dto.CourseLearningPointResponse;
import edu.lab.core.course.learning.dto.LearningTaskProgressResponse;
import edu.lab.core.course.learning.dto.CourseLearningTaskSubmissionResponse;
import edu.lab.core.course.learning.dto.CourseLearningTaskSummaryResponse;
import edu.lab.core.course.learning.dto.CourseLearningUnitResponse;
import edu.lab.core.course.learning.dto.LearningTaskOrderUpdateRequest;
import edu.lab.core.course.learning.dto.LearningPointCreateRequest;
import edu.lab.core.course.learning.dto.LearningTaskGradeRequest;
import edu.lab.core.course.learning.dto.LearningUnitCreateRequest;
import edu.lab.core.notification.HomeworkReminderService;
import edu.lab.core.course.learning.dto.StudentLearningOverviewResponse;
import edu.lab.core.security.AuthenticatedUser;
import edu.lab.core.storage.FileStorageService;
import edu.lab.core.user.AppUser;
import edu.lab.core.user.UserRepository;
import edu.lab.core.user.UserRole;
import edu.lab.core.user.dto.UserSummaryResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class CourseLearningService {

	private final CourseService courseService;
	private final CourseMemberRepository courseMemberRepository;
	private final CourseRepository courseRepository;
	private final UserRepository userRepository;
	private final CourseLearningUnitRepository unitRepository;
	private final CourseLearningPointRepository pointRepository;
	private final CourseLearningTaskRepository taskRepository;
	private final CourseLearningTaskSubmissionRepository submissionRepository;
	private final HomeworkReminderService homeworkReminderService;
	private final FileStorageService fileStorageService;
	private final ObjectMapper objectMapper;

	@Transactional(readOnly = true)
	public CourseLearningDetailResponse getLearningDetail(AuthenticatedUser currentUser, UUID courseId) {
		Course course = courseService.requireAccessibleCourse(currentUser.id(), courseId);
		return new CourseLearningDetailResponse(
			course.getId(),
			course.getTitle(),
			course.getDescription(),
			course.getInviteCode(),
			course.getCreatedAt(),
			loadUnits(courseId)
		);
	}

	@Transactional(readOnly = true)
	public CourseLearningOverviewResponse getOverview(AuthenticatedUser currentUser, UUID courseId) {
		courseService.requireAccessibleCourse(currentUser.id(), courseId);
		List<CourseLearningUnit> units = unitRepository.findByCourseIdOrderBySortOrderAscCreatedAtAsc(courseId);
		Map<UUID, List<CourseLearningTaskSubmission>> latestSubmissionsByStudentId = new HashMap<>();
		for (CourseLearningTaskSubmission submission : submissionRepository.findLatestByCourseId(courseId)) {
			latestSubmissionsByStudentId.computeIfAbsent(submission.getSubmittedBy().getId(), key -> new ArrayList<>()).add(submission);
		}

		List<CourseLearningTask> allTasks = new ArrayList<>();
		for (CourseLearningUnit unit : units) {
			for (CourseLearningPoint point : pointRepository.findByUnitIdOrderBySortOrderAscCreatedAtAsc(unit.getId())) {
				allTasks.addAll(taskRepository.findByKnowledgePointIdOrderBySortOrderAscCreatedAtAsc(point.getId()));
			}
		}

		List<AppUser> students = currentUser.role() == UserRole.STUDENT
			? List.of(requireCurrentUser(currentUser.id()))
			: courseMemberRepository.findMembers(courseId).stream()
				.filter(member -> member.getMemberRole() == CourseMemberRole.STUDENT)
				.map(member -> member.getUser())
				.toList();

		List<StudentLearningOverviewResponse> items = students.stream()
			.map(student -> toStudentOverview(student, latestSubmissionsByStudentId.get(student.getId()), units, allTasks))
			.sorted(Comparator.comparing(item -> item.student().username()))
			.toList();

		return new CourseLearningOverviewResponse(
			units.size(),
			(int) units.stream().mapToLong(unit -> pointRepository.findByUnitIdOrderBySortOrderAscCreatedAtAsc(unit.getId()).size()).sum(),
			allTasks.size(),
			items
		);
	}

	@Transactional
	public CourseLearningUnitResponse createUnit(AuthenticatedUser currentUser, UUID courseId, LearningUnitCreateRequest request) {
		Course course = courseService.requireTeachingCourse(currentUser.id(), courseId);
		AppUser creator = requireUser(currentUser.id());
		CourseLearningUnit unit = new CourseLearningUnit();
		unit.setCourse(course);
		unit.setCreatedBy(creator);
		unit.setTitle(normalizeTitle(request.title(), "单元标题不能为空"));
		unit.setDescription(normalizeNullableText(request.description()));
		unit.setSortOrder(normalizeSortOrder(request.sortOrder()));
		unit.setPublished(request.published() == null || request.published());
		unit = unitRepository.save(unit);
		return toUnitResponse(unit, List.of());
	}

	@Transactional
	public CourseLearningPointResponse createPoint(AuthenticatedUser currentUser, UUID courseId, UUID unitId, LearningPointCreateRequest request) {
		CourseLearningUnit unit = requireUnitInCourse(currentUser.id(), courseId, unitId, true);
		AppUser creator = requireUser(currentUser.id());
		CourseLearningPoint point = new CourseLearningPoint();
		point.setUnit(unit);
		point.setCreatedBy(creator);
		point.setTitle(normalizeTitle(request.title(), "知识点标题不能为空"));
		point.setSummary(normalizeNullableText(request.summary()));
		point.setEstimatedMinutes(request.estimatedMinutes());
		point.setSortOrder(normalizeSortOrder(request.sortOrder()));
		point = pointRepository.save(point);
		return toPointResponse(point, List.of());
	}

	@Transactional
	public CourseLearningTaskSummaryResponse createTask(AuthenticatedUser currentUser,
		UUID courseId,
		UUID pointId,
		String title,
		String description,
		LearningTaskType taskType,
		LearningTaskKind taskKind,
		LearningMaterialType materialType,
		String contentText,
		String mediaUrl,
		LearningQuestionType questionType,
		String optionsText,
		String referenceAnswer,
		BigDecimal maxScore,
		LocalDateTime startAt,
		LocalDateTime dueAt,
		Boolean notifyOnStart,
		Boolean notifyBeforeDue24h,
		Boolean notifyOnDue,
		Boolean required,
		Integer sortOrder,
		MultipartFile file) {
		CourseLearningPoint point = requirePointInCourse(currentUser.id(), courseId, pointId, true);
		AppUser creator = requireUser(currentUser.id());
		LearningTaskType effectiveTaskType = taskType != null ? taskType : (questionType != null ? LearningTaskType.QUIZ : LearningTaskType.MEDIA);
		LearningTaskKind effectiveTaskKind = taskKind == null ? LearningTaskKind.LEARNING : taskKind;
		CourseLearningTask task = new CourseLearningTask();
		task.setKnowledgePoint(point);
		task.setCreatedBy(creator);
		task.setTitle(normalizeTitle(title, "任务标题不能为空"));
		task.setDescription(normalizeNullableText(description));
		task.setTaskType(effectiveTaskType);
		task.setTaskKind(effectiveTaskKind);
		task.setMaterialType(materialType);
		task.setContentText(normalizeNullableText(contentText));
		task.setMediaUrl(normalizeNullableText(mediaUrl));
		task.setQuestionType(questionType);
		task.setReferenceAnswer(normalizeNullableText(referenceAnswer));
		task.setMaxScore(maxScore == null ? BigDecimal.valueOf(10) : maxScore);
		task.setStartAt(startAt);
		task.setDueAt(dueAt);
		task.setNotifyOnStart(notifyOnStart == null || notifyOnStart);
		task.setNotifyBeforeDue24h(notifyBeforeDue24h == null || notifyBeforeDue24h);
		task.setNotifyOnDue(notifyOnDue == null || notifyOnDue);
		task.setRequired(required == null || required);
		task.setSortOrder(normalizeSortOrder(sortOrder));
		task.setOptionsJson(writeOptions(effectiveTaskType, questionType, optionsText));
		validateTaskPayload(effectiveTaskType, effectiveTaskKind, materialType, questionType, file, task.getMediaUrl(), task.getContentText(), task.getOptionsJson(), task.getStartAt(), task.getDueAt());
		task = taskRepository.save(task);

		if (task.getTaskType() == LearningTaskType.MEDIA && task.getMaterialType() == LearningMaterialType.FILE && file != null && !file.isEmpty()) {
			FileStorageService.StoredFile storedFile = fileStorageService.saveLearningTaskFile(courseId, task.getId(), file);
			task.setFileName(storedFile.fileName());
			task.setFilePath(storedFile.filePath());
			task.setContentType(storedFile.contentType());
			task = taskRepository.save(task);
		}

		if (task.getTaskKind() == LearningTaskKind.HOMEWORK) {
			homeworkReminderService.scheduleForHomeworkTask(task);
		}

		return toTaskResponse(task);
	}

	@Transactional
	public void reorderTasks(AuthenticatedUser currentUser, UUID courseId, UUID pointId, LearningTaskOrderUpdateRequest request) {
		requirePointInCourse(currentUser.id(), courseId, pointId, true);
		if (request == null || request.orderedTaskIds() == null || request.orderedTaskIds().isEmpty()) {
			throw new BadRequestException("任务排序数据不能为空");
		}

		List<CourseLearningTask> existingTasks = taskRepository.findByKnowledgePointIdOrderBySortOrderAscCreatedAtAsc(pointId);
		if (existingTasks.size() != request.orderedTaskIds().size()) {
			throw new BadRequestException("排序任务数量不匹配");
		}

		Map<UUID, CourseLearningTask> taskMap = new HashMap<>();
		for (CourseLearningTask task : existingTasks) {
			taskMap.put(task.getId(), task);
		}

		int order = 10;
		for (UUID taskId : request.orderedTaskIds()) {
			CourseLearningTask task = taskMap.get(taskId);
			if (task == null) {
				throw new BadRequestException("排序任务不属于该知识点");
			}
			task.setSortOrder(order);
			order += 10;
		}
		taskRepository.saveAll(existingTasks);
	}

	@Transactional(readOnly = true)
	public List<CourseHomeworkItemResponse> listCourseHomeworks(AuthenticatedUser currentUser, UUID courseId) {
		courseService.requireAccessibleCourse(currentUser.id(), courseId);
		List<CourseLearningTask> tasks = taskRepository.findHomeworkByCourseId(courseId);
		return toHomeworkItems(currentUser, tasks);
	}

	@Transactional(readOnly = true)
	public List<CourseHomeworkItemResponse> listMyHomeworks(AuthenticatedUser currentUser, UUID courseId) {
		courseService.requireAccessibleCourse(currentUser.id(), courseId);
		List<CourseLearningTask> tasks = taskRepository.findHomeworkByCourseId(courseId);
		return toHomeworkItems(currentUser, tasks);
	}

	@Transactional(readOnly = true)
	public List<CourseHomeworkItemResponse> listMyHomeworksAcrossCourses(AuthenticatedUser currentUser) {
		AppUser user = requireUser(currentUser.id());
		List<UUID> courseIds;
		if (user.getRole() == UserRole.TEACHER || user.getRole() == UserRole.ADMIN) {
			courseIds = courseRepository.findOwnedCourses(user.getId()).stream().map(Course::getId).toList();
		} else {
			courseIds = courseMemberRepository.findCourseIdsByUserId(user.getId());
		}
		if (courseIds.isEmpty()) {
			return List.of();
		}
		return toHomeworkItems(currentUser, taskRepository.findHomeworkByCourseIds(courseIds));
	}

	@Transactional(readOnly = true)
	public FileStorageService.StoredContent accessTaskFile(AuthenticatedUser currentUser, UUID courseId, UUID taskId) {
		CourseLearningTask task = requireTaskInCourse(currentUser.id(), courseId, taskId, false);
		if (task.getMaterialType() != LearningMaterialType.FILE || task.getFilePath() == null) {
			throw new BadRequestException("当前任务不是文件类型，无法下载");
		}
		return fileStorageService.openFile(task.getFilePath(), task.getFileName(), task.getContentType());
	}

	@Transactional(readOnly = true)
	public List<CourseLearningTaskSubmissionResponse> listTaskSubmissions(AuthenticatedUser currentUser, UUID courseId, UUID taskId) {
		CourseLearningTask task = requireTaskInCourse(currentUser.id(), courseId, taskId, false);
		boolean teacher = courseService.isTeacherOfCourse(currentUser.id(), task.getKnowledgePoint().getUnit().getCourse().getId());
		List<CourseLearningTaskSubmission> submissions = teacher
			? submissionRepository.findTaskSubmissionsWithUsers(taskId)
			: submissionRepository.findByTaskIdAndSubmittedByIdOrderBySubmittedAtDesc(taskId, currentUser.id());
		return submissions.stream().map(this::toSubmissionResponse).toList();
	}

	@Transactional(readOnly = true)
	public CourseLearningTaskSubmissionResponse getLatestSubmission(AuthenticatedUser currentUser, UUID courseId, UUID taskId) {
		requireTaskInCourse(currentUser.id(), courseId, taskId, false);
		return submissionRepository.findByTaskIdAndSubmittedByIdAndLatestTrue(taskId, currentUser.id())
			.map(this::toSubmissionResponse)
			.orElseThrow(() -> new NotFoundException("没有找到最新提交"));
	}

	@Transactional
	public CourseLearningTaskSubmissionResponse submitTask(AuthenticatedUser currentUser, UUID courseId, UUID taskId, String answerText, MultipartFile file) {
		if ((answerText == null || answerText.isBlank()) && (file == null || file.isEmpty())) {
			throw new BadRequestException("答卷内容不能为空");
		}
		AppUser user = requireStudent(currentUser.id());
		CourseLearningTask task = requireTaskInCourse(currentUser.id(), courseId, taskId, false);
		CourseLearningTaskSubmission previousLatest = submissionRepository.findByTaskIdAndSubmittedByIdAndLatestTrue(taskId, user.getId()).orElse(null);
		if (previousLatest != null) {
			previousLatest.setLatest(false);
			submissionRepository.save(previousLatest);
		}

		CourseLearningTaskSubmission submission = new CourseLearningTaskSubmission();
		submission.setTask(task);
		submission.setSubmittedBy(user);
		submission.setSubmittedAt(LocalDateTime.now());
		submission.setLatest(true);
		submission.setAnswerText(normalizeNullableText(answerText));
		submission = submissionRepository.save(submission);

		if (file != null && !file.isEmpty()) {
			FileStorageService.StoredFile storedFile = fileStorageService.saveLearningTaskSubmissionFile(task.getId(), submission.getId(), file);
			submission.setFileName(storedFile.fileName());
			submission.setFilePath(storedFile.filePath());
			submission.setContentType(storedFile.contentType());
			submission = submissionRepository.save(submission);
		}

		return toSubmissionResponse(submission);
	}

	@Transactional
	public CourseLearningTaskSubmissionResponse gradeSubmission(AuthenticatedUser currentUser, UUID courseId, UUID submissionId, LearningTaskGradeRequest request) {
		CourseLearningTaskSubmission submission = submissionRepository.findById(submissionId)
			.orElseThrow(() -> new NotFoundException("答卷不存在"));
		courseService.requireTeachingCourse(currentUser.id(), submission.getTask().getKnowledgePoint().getUnit().getCourse().getId());
		AppUser grader = requireUser(currentUser.id());
		submission.setScore(request.score());
		submission.setFeedback(normalizeNullableText(request.feedback()));
		submission.setGradedBy(grader);
		submission.setGradedAt(LocalDateTime.now());
		submission = submissionRepository.save(submission);
		return toSubmissionResponse(submission);
	}

	private List<CourseLearningUnitResponse> loadUnits(UUID courseId) {
		List<CourseLearningUnitResponse> units = new ArrayList<>();
		for (CourseLearningUnit unit : unitRepository.findByCourseIdOrderBySortOrderAscCreatedAtAsc(courseId)) {
			List<CourseLearningPointResponse> points = new ArrayList<>();
			for (CourseLearningPoint point : pointRepository.findByUnitIdOrderBySortOrderAscCreatedAtAsc(unit.getId())) {
				points.add(toPointResponse(point, loadTasks(point.getId())));
			}
			units.add(toUnitResponse(unit, points));
		}
		return units;
	}

	private List<CourseLearningTaskSummaryResponse> loadTasks(UUID pointId) {
		return taskRepository.findByKnowledgePointIdOrderBySortOrderAscCreatedAtAsc(pointId).stream().map(this::toTaskResponse).toList();
	}

	private StudentLearningOverviewResponse toStudentOverview(AppUser student,
		List<CourseLearningTaskSubmission> latestSubmissions,
		List<CourseLearningUnit> units,
		List<CourseLearningTask> allTasks) {
		Map<UUID, CourseLearningTaskSubmission> latestSubmissionByTaskId = new HashMap<>();
		if (latestSubmissions != null) {
			for (CourseLearningTaskSubmission submission : latestSubmissions) {
				latestSubmissionByTaskId.put(submission.getTask().getId(), submission);
			}
		}

		List<LearningTaskProgressResponse> tasks = new ArrayList<>();
		for (CourseLearningUnit unit : units) {
			for (CourseLearningPoint point : pointRepository.findByUnitIdOrderBySortOrderAscCreatedAtAsc(unit.getId())) {
				for (CourseLearningTask task : taskRepository.findByKnowledgePointIdOrderBySortOrderAscCreatedAtAsc(point.getId())) {
					CourseLearningTaskSubmission submission = latestSubmissionByTaskId.get(task.getId());
					tasks.add(new LearningTaskProgressResponse(
						unit.getId(),
						unit.getTitle(),
						point.getId(),
						point.getTitle(),
						task.getId(),
						task.getTitle(),
						task.getTaskType(),
						task.getTaskKind(),
						task.getMaterialType(),
						task.getQuestionType(),
						task.getMaxScore(),
						task.getStartAt(),
						task.getDueAt(),
						submission == null ? null : submission.getId(),
						submission == null ? null : submission.getAnswerText(),
						submission == null ? null : submission.getFileName(),
						submission == null ? null : submission.getScore(),
						submission == null ? null : submission.getFeedback(),
						submission == null ? null : submission.getSubmittedAt(),
						submission == null ? null : submission.getGradedAt(),
						submission != null && submission.isLatest()
					));
				}
			}
		}

		int submissionCount = (int) tasks.stream().filter(task -> task.submissionId() != null).count();
		int gradedCount = (int) tasks.stream().filter(task -> task.score() != null).count();
		BigDecimal averageScore = null;
		if (gradedCount > 0) {
			BigDecimal total = tasks.stream()
				.map(LearningTaskProgressResponse::score)
				.filter(score -> score != null)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
			averageScore = total.divide(BigDecimal.valueOf(gradedCount), 2, RoundingMode.HALF_UP);
		}

		return new StudentLearningOverviewResponse(
			new UserSummaryResponse(student.getId(), student.getUsername(), student.getDisplayName(), student.getEmail(), student.getRole()),
			submissionCount,
			gradedCount,
			averageScore,
			tasks
		);
	}

	private CourseLearningUnitResponse toUnitResponse(CourseLearningUnit unit, List<CourseLearningPointResponse> points) {
		return new CourseLearningUnitResponse(
			unit.getId(),
			unit.getCourse().getId(),
			unit.getTitle(),
			unit.getDescription(),
			unit.getSortOrder(),
			unit.isPublished(),
			toUserSummary(unit.getCreatedBy()),
			unit.getCreatedAt(),
			points
		);
	}

	private CourseLearningPointResponse toPointResponse(CourseLearningPoint point, List<CourseLearningTaskSummaryResponse> tasks) {
		return new CourseLearningPointResponse(
			point.getId(),
			point.getUnit().getId(),
			point.getTitle(),
			point.getSummary(),
			point.getEstimatedMinutes(),
			point.getSortOrder(),
			toUserSummary(point.getCreatedBy()),
			point.getCreatedAt(),
			tasks
		);
	}

	private CourseLearningTaskSummaryResponse toTaskResponse(CourseLearningTask task) {
		return new CourseLearningTaskSummaryResponse(
			task.getId(),
			task.getKnowledgePoint().getId(),
			task.getTitle(),
			task.getDescription(),
			task.getTaskType(),
			task.getTaskKind(),
			task.getMaterialType(),
			task.getQuestionType(),
			task.getContentText(),
			task.getMediaUrl(),
			task.getFileName(),
			readOptions(task.getOptionsJson()),
			task.getReferenceAnswer(),
			task.getMaxScore(),
			task.getStartAt(),
			task.getDueAt(),
			task.isNotifyOnStart(),
			task.isNotifyBeforeDue24h(),
			task.isNotifyOnDue(),
			task.isRequired(),
			task.getSortOrder(),
			toUserSummary(task.getCreatedBy()),
			task.getCreatedAt()
		);
	}

	private List<CourseHomeworkItemResponse> toHomeworkItems(AuthenticatedUser currentUser, List<CourseLearningTask> tasks) {
		if (tasks.isEmpty()) {
			return List.of();
		}
		List<UUID> taskIds = tasks.stream().map(CourseLearningTask::getId).toList();
		Map<UUID, CourseLearningTaskSubmission> latestByTaskId = submissionRepository.findLatestByUserIdAndTaskIds(currentUser.id(), taskIds).stream()
			.collect(java.util.stream.Collectors.toMap(item -> item.getTask().getId(), item -> item));

		Map<UUID, Long> totalStudentsByCourse = new HashMap<>();
		for (CourseLearningTask task : tasks) {
			UUID courseId = task.getKnowledgePoint().getUnit().getCourse().getId();
			totalStudentsByCourse.computeIfAbsent(courseId, key -> (long) courseMemberRepository.findStudentIdsByCourseId(key).size());
		}

		LocalDateTime now = LocalDateTime.now();
		List<CourseHomeworkItemResponse> items = new ArrayList<>();
		for (CourseLearningTask task : tasks) {
			CourseLearningTaskSubmission submission = latestByTaskId.get(task.getId());
			String status = resolveHomeworkStatus(task, submission, now);
			Long remainingSeconds = task.getDueAt() == null ? null : Duration.between(now, task.getDueAt()).getSeconds();
			if (remainingSeconds != null && remainingSeconds < 0) {
				remainingSeconds = 0L;
			}
			UUID courseId = task.getKnowledgePoint().getUnit().getCourse().getId();
			items.add(new CourseHomeworkItemResponse(
				task.getId(),
				courseId,
				task.getKnowledgePoint().getUnit().getCourse().getTitle(),
				task.getTitle(),
				task.getStartAt(),
				task.getDueAt(),
				status,
				submission == null ? null : submission.getId(),
				submission == null ? null : submission.getSubmittedAt(),
				submission == null ? null : submission.getScore(),
				submissionRepository.countLatestByTaskId(task.getId()),
				totalStudentsByCourse.getOrDefault(courseId, 0L),
				remainingSeconds
			));
		}
		return items;
	}

	private String resolveHomeworkStatus(CourseLearningTask task, CourseLearningTaskSubmission submission, LocalDateTime now) {
		if (submission != null) {
			return "SUBMITTED";
		}
		if (task.getStartAt() != null && now.isBefore(task.getStartAt())) {
			return "NOT_STARTED";
		}
		if (task.getDueAt() != null && now.isAfter(task.getDueAt())) {
			return "OVERDUE";
		}
		return "IN_PROGRESS";
	}

	private CourseLearningTaskSubmissionResponse toSubmissionResponse(CourseLearningTaskSubmission submission) {
		return new CourseLearningTaskSubmissionResponse(
			submission.getId(),
			submission.getTask().getId(),
			toUserSummary(submission.getSubmittedBy()),
			submission.getAnswerText(),
			submission.getFileName(),
			submission.getFilePath(),
			submission.getScore(),
			submission.getFeedback(),
			submission.getGradedBy() == null ? null : toUserSummary(submission.getGradedBy()),
			submission.getGradedAt(),
			submission.isLatest(),
			submission.getSubmittedAt()
		);
	}

	private UserSummaryResponse toUserSummary(AppUser user) {
		return new UserSummaryResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getEmail(), user.getRole());
	}

	private CourseLearningUnit requireUnitInCourse(UUID userId, UUID courseId, UUID unitId, boolean teachingOnly) {
		CourseLearningUnit unit = unitRepository.findById(unitId)
			.orElseThrow(() -> new NotFoundException("学习单元不存在"));
		if (!unit.getCourse().getId().equals(courseId)) {
			throw new NotFoundException("学习单元不存在");
		}
		if (teachingOnly) {
			courseService.requireTeachingCourse(userId, courseId);
		} else {
			courseService.requireAccessibleCourse(userId, courseId);
		}
		return unit;
	}

	private CourseLearningPoint requirePointInCourse(UUID userId, UUID courseId, UUID pointId, boolean teachingOnly) {
		CourseLearningPoint point = pointRepository.findById(pointId)
			.orElseThrow(() -> new NotFoundException("知识点不存在"));
		if (!point.getUnit().getCourse().getId().equals(courseId)) {
			throw new NotFoundException("知识点不存在");
		}
		if (teachingOnly) {
			courseService.requireTeachingCourse(userId, courseId);
		} else {
			courseService.requireAccessibleCourse(userId, courseId);
		}
		return point;
	}

	private CourseLearningTask requireTaskInCourse(UUID userId, UUID courseId, UUID taskId, boolean teachingOnly) {
		CourseLearningTask task = taskRepository.findWithCourseById(taskId)
			.orElseThrow(() -> new NotFoundException("学习任务不存在"));
		if (!task.getKnowledgePoint().getUnit().getCourse().getId().equals(courseId)) {
			throw new NotFoundException("学习任务不存在");
		}
		if (teachingOnly) {
			courseService.requireTeachingCourse(userId, courseId);
		} else {
			courseService.requireAccessibleCourse(userId, courseId);
		}
		return task;
	}

	private AppUser requireUser(UUID userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new NotFoundException("当前用户不存在"));
	}

	private AppUser requireCurrentUser(UUID userId) {
		return requireUser(userId);
	}

	private AppUser requireStudent(UUID userId) {
		AppUser user = requireUser(userId);
		if (user.getRole() != UserRole.STUDENT) {
			throw new ForbiddenException("只有学生可以提交答卷");
		}
		return user;
	}

	private String normalizeTitle(String value, String message) {
		if (value == null || value.isBlank()) {
			throw new BadRequestException(message);
		}
		return value.trim();
	}

	private String normalizeNullableText(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private int normalizeSortOrder(Integer sortOrder) {
		return sortOrder == null ? 0 : Math.max(sortOrder, 0);
	}

	private String normalizeUrl(String externalUrl) {
		String normalized = externalUrl.trim();
		try {
			URI uri = new URI(normalized);
			String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
			if (!"http".equals(scheme) && !"https".equals(scheme)) {
				throw new BadRequestException("externalUrl 仅支持 http/https");
			}
			return normalized;
		} catch (URISyntaxException exception) {
			throw new BadRequestException("externalUrl 格式不正确");
		}
	}

	private String writeOptions(LearningTaskType taskType, LearningQuestionType questionType, String optionsText) {
		if (taskType != LearningTaskType.QUIZ) {
			return null;
		}
		List<String> options = parseOptions(optionsText);
		if (options.isEmpty() && questionType != LearningQuestionType.SHORT_ANSWER) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(options);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("选项写入失败", exception);
		}
	}

	private List<String> readOptions(String optionsJson) {
		if (optionsJson == null || optionsJson.isBlank()) {
			return List.of();
		}
		try {
			return objectMapper.readValue(optionsJson, new TypeReference<List<String>>() {});
		} catch (JsonProcessingException exception) {
			return List.of();
		}
	}

	private List<String> parseOptions(String optionsText) {
		if (optionsText == null || optionsText.isBlank()) {
			return List.of();
		}
		return optionsText.lines()
			.map(String::trim)
			.filter(line -> !line.isBlank())
			.toList();
	}

	private void validateTaskPayload(LearningTaskType taskType,
		LearningTaskKind taskKind,
		LearningMaterialType materialType,
		LearningQuestionType questionType,
		MultipartFile file,
		String mediaUrl,
		String contentText,
		String optionsJson,
		LocalDateTime startAt,
		LocalDateTime dueAt) {
		if (taskKind == LearningTaskKind.HOMEWORK) {
			if (startAt == null || dueAt == null) {
				throw new BadRequestException("作业任务必须设置开始和截止时间");
			}
			if (startAt.isAfter(dueAt)) {
				throw new BadRequestException("作业开始时间不能晚于截止时间");
			}
		}

		if (taskType == LearningTaskType.MEDIA) {
			if (materialType == null) {
				throw new BadRequestException("媒体学习任务必须指定 materialType");
			}
			if (materialType == LearningMaterialType.FILE && (file == null || file.isEmpty())) {
				throw new BadRequestException("文件类型媒体任务必须上传文件");
			}
			if (materialType == LearningMaterialType.LINK && (mediaUrl == null || mediaUrl.isBlank())) {
				throw new BadRequestException("链接类型媒体任务必须提供 mediaUrl");
			}
			if (materialType == LearningMaterialType.TEXT && (contentText == null || contentText.isBlank())) {
				throw new BadRequestException("文本类型媒体任务必须提供内容");
			}
			if (mediaUrl != null && !mediaUrl.isBlank()) {
				normalizeUrl(mediaUrl);
			}
			return;
		}

		if (questionType == null) {
			throw new BadRequestException("随堂测试任务必须指定题型");
		}
		if ((questionType == LearningQuestionType.SINGLE_CHOICE || questionType == LearningQuestionType.MULTIPLE_CHOICE) && (optionsJson == null || optionsJson.isBlank())) {
			throw new BadRequestException("选择题必须提供选项");
		}
	}
}