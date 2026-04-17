package edu.lab.core.submission;

import edu.lab.core.common.exception.BadRequestException;
import edu.lab.core.common.exception.ForbiddenException;
import edu.lab.core.common.exception.NotFoundException;
import edu.lab.core.course.CourseService;
import edu.lab.core.experiment.Experiment;
import edu.lab.core.experiment.ExperimentService;
import edu.lab.core.security.AuthenticatedUser;
import edu.lab.core.storage.FileStorageService;
import edu.lab.core.submission.dto.SubmissionDetailResponse;
import edu.lab.core.user.AppUser;
import edu.lab.core.user.UserRole;
import edu.lab.core.user.UserRepository;
import edu.lab.core.user.dto.UserSummaryResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
	private final CourseService courseService;
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
		submission.setNote(note);

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

	private AppUser requireStudent(UUID userId) {
		AppUser user = userRepository.findById(userId)
			.orElseThrow(() -> new NotFoundException("当前用户不存在"));
		if (user.getRole() != UserRole.STUDENT) {
			throw new ForbiddenException("只有学生可以提交实验");
		}
		return user;
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
			submission.isLatest(),
			submission.getSubmittedAt()
		);
	}
}