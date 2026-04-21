package edu.lab.core.submission;

import edu.lab.core.common.exception.BadRequestException;
import edu.lab.core.common.exception.ForbiddenException;
import edu.lab.core.common.exception.NotFoundException;
import edu.lab.core.course.CourseMemberRepository;
import edu.lab.core.course.CourseMemberRole;
import edu.lab.core.course.CourseService;
import edu.lab.core.experiment.Experiment;
import edu.lab.core.experiment.ExperimentRepository;
import edu.lab.core.experiment.ExperimentService;
import edu.lab.core.security.AuthenticatedUser;
import edu.lab.core.storage.FileStorageService;
import edu.lab.core.submission.dto.CourseGradeOverviewResponse;
import edu.lab.core.submission.dto.ExperimentGradeItemResponse;
import edu.lab.core.submission.dto.StudentGradeOverviewResponse;
import edu.lab.core.submission.dto.SubmissionDetailResponse;
import edu.lab.core.user.AppUser;
import edu.lab.core.user.UserRepository;
import edu.lab.core.user.UserRole;
import edu.lab.core.user.dto.UserSummaryResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class SubmissionService {

	private final SubmissionRepository submissionRepository;
	private final ExperimentService experimentService;
	private final ExperimentRepository experimentRepository;
	private final CourseService courseService;
	private final CourseMemberRepository courseMemberRepository;
	private final UserRepository userRepository;
	private final FileStorageService fileStorageService;

	@Transactional
	public SubmissionDetailResponse submit(AuthenticatedUser currentUser, UUID experimentId, MultipartFile file, String note) {
		if (file == null || file.isEmpty()) {
			throw new BadRequestException("提交文件不能为空");
		}
		AppUser user = requireStudent(currentUser.id());
		Experiment experiment = experimentService.requireAccessibleExperiment(currentUser.id(), experimentId);
		if (experiment.getDueAt() != null && experiment.getDueAt().isBefore(LocalDateTime.now())) {
			throw new ForbiddenException("实验已截止，无法提交");
		}
		Submission previousLatest = submissionRepository.findByExperimentIdAndSubmittedByIdAndLatestTrue(experimentId, user.getId()).orElse(null);
		if (previousLatest != null) {
			submissionRepository.markPreviousLatestFalse(experimentId, user.getId());
		}

		Submission submission = new Submission();
		submission.setExperiment(experiment);
		submission.setSubmittedBy(user);
		submission.setSubmittedAt(LocalDateTime.now());
		submission.setLatest(true);
		submission.setNote(normalizeNullableText(note));

		FileStorageService.StoredFile storedFile = fileStorageService.saveSubmissionFile(submission.getId(), file);
		submission.setFilePath(storedFile.filePath());
		submission.setFileName(storedFile.fileName());
		submission.setContentType(storedFile.contentType());
		submission = submissionRepository.save(submission);

		return toDetail(submission);
	}

	@Transactional(readOnly = true)
	public List<SubmissionDetailResponse> listSubmissions(AuthenticatedUser currentUser, UUID experimentId) {
		Experiment experiment = experimentService.requireAccessibleExperiment(currentUser.id(), experimentId);
		boolean teacher = courseService.isTeacherOfCourse(currentUser.id(), experiment.getCourse().getId());
		if (teacher) {
			return submissionRepository.findByExperimentIdOrderBySubmittedAtDesc(experimentId).stream().map(this::toDetail).toList();
		}
		return submissionRepository.findByExperimentIdAndSubmittedByIdOrderBySubmittedAtDesc(experimentId, currentUser.id()).stream().map(this::toDetail).toList();
	}

	@Transactional(readOnly = true)
	public SubmissionDetailResponse getLatestSubmission(AuthenticatedUser currentUser, UUID experimentId) {
		return submissionRepository.findByExperimentIdAndSubmittedByIdAndLatestTrue(experimentId, currentUser.id())
			.map(this::toDetail)
			.orElseThrow(() -> new NotFoundException("没有找到最新提交"));
	}

	@Transactional
	public SubmissionDetailResponse gradeSubmission(AuthenticatedUser currentUser, UUID submissionId, BigDecimal score, String feedback) {
		Submission submission = submissionRepository.findById(submissionId)
			.orElseThrow(() -> new NotFoundException("提交记录不存在"));
		courseService.requireTeachingCourse(currentUser.id(), submission.getExperiment().getCourse().getId());
		AppUser grader = requireUser(currentUser.id());

		submission.setScore(score);
		submission.setFeedback(normalizeNullableText(feedback));
		submission.setGradedAt(LocalDateTime.now());
		submission.setGradedBy(grader);
		submission = submissionRepository.save(submission);
		return toDetail(submission);
	}

	@Transactional(readOnly = true)
	public CourseGradeOverviewResponse getCourseGradeOverview(AuthenticatedUser currentUser, UUID courseId) {
		courseService.requireTeachingCourse(currentUser.id(), courseId);

		Map<UUID, List<Submission>> latestSubmissionsByStudentId = new HashMap<>();
		for (Submission submission : submissionRepository.findLatestByCourseId(courseId)) {
			latestSubmissionsByStudentId.computeIfAbsent(submission.getSubmittedBy().getId(), key -> new ArrayList<>()).add(submission);
		}

		Map<UUID, String> experimentTitles = new HashMap<>();
		for (Experiment experiment : experimentRepository.findByCourseIdOrderByPublishedAtDesc(courseId)) {
			experimentTitles.put(experiment.getId(), experiment.getTitle());
		}

		List<StudentGradeOverviewResponse> students = courseMemberRepository.findMembers(courseId).stream()
			.filter(member -> member.getMemberRole() == CourseMemberRole.STUDENT)
			.map(member -> toStudentOverview(member.getUser(), latestSubmissionsByStudentId.get(member.getUser().getId()), experimentTitles))
			.sorted(Comparator.comparing(item -> item.student().username()))
			.toList();
		return new CourseGradeOverviewResponse(students);
	}

	private AppUser requireUser(UUID userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new NotFoundException("当前用户不存在"));
	}

	private AppUser requireStudent(UUID userId) {
		// 任何认证用户都可以提交实验（权限检查在课程层面）
		return requireUser(userId);
	}

	private StudentGradeOverviewResponse toStudentOverview(AppUser student, List<Submission> latestSubmissions, Map<UUID, String> experimentTitles) {
		List<ExperimentGradeItemResponse> items = new ArrayList<>();
		if (latestSubmissions != null) {
			for (Submission latestSubmission : latestSubmissions) {
				UUID experimentId = latestSubmission.getExperiment().getId();
				items.add(new ExperimentGradeItemResponse(
					experimentId,
					experimentTitles.getOrDefault(experimentId, "实验"),
					latestSubmission.getId(),
					latestSubmission.getScore(),
					latestSubmission.getFeedback(),
					latestSubmission.getSubmittedAt(),
					latestSubmission.getGradedAt()
				));
			}
			items.sort(Comparator.comparing(ExperimentGradeItemResponse::submittedAt).reversed());
		}

		int submissionCount = items.size();
		int gradedCount = (int) items.stream().filter(item -> item.score() != null).count();
		BigDecimal averageScore = null;
		if (gradedCount > 0) {
			BigDecimal total = items.stream()
				.map(ExperimentGradeItemResponse::score)
				.filter(score -> score != null)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
			averageScore = total.divide(BigDecimal.valueOf(gradedCount), 2, RoundingMode.HALF_UP);
		}

		return new StudentGradeOverviewResponse(
			new UserSummaryResponse(student.getId(), student.getUsername(), student.getDisplayName(), student.getEmail(), student.getRole()),
			submissionCount,
			gradedCount,
			averageScore,
			items
		);
	}

	private String normalizeNullableText(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private SubmissionDetailResponse toDetail(Submission submission) {
		return new SubmissionDetailResponse(
			submission.getId(),
			submission.getExperiment().getId(),
			new UserSummaryResponse(
				submission.getSubmittedBy().getId(),
				submission.getSubmittedBy().getUsername(),
				submission.getSubmittedBy().getDisplayName(),
				submission.getSubmittedBy().getEmail(),
				submission.getSubmittedBy().getRole()),
			submission.getFileName(),
			submission.getFilePath(),
			submission.getNote(),
			submission.getScore(),
			submission.getFeedback(),
			submission.getGradedBy() == null ? null : new UserSummaryResponse(
				submission.getGradedBy().getId(),
				submission.getGradedBy().getUsername(),
				submission.getGradedBy().getDisplayName(),
				submission.getGradedBy().getEmail(),
				submission.getGradedBy().getRole()),
			submission.getGradedAt(),
			submission.isLatest(),
			submission.getSubmittedAt()
		);
	}
}
