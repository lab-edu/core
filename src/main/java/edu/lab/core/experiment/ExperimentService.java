package edu.lab.core.experiment;

import edu.lab.core.common.exception.NotFoundException;
import edu.lab.core.course.Course;
import edu.lab.core.course.CourseService;
import edu.lab.core.course.dto.CourseSummaryResponse;
import edu.lab.core.experiment.dto.ExperimentCreateRequest;
import edu.lab.core.experiment.dto.ExperimentDetailResponse;
import edu.lab.core.experiment.dto.ExperimentSummaryResponse;
import edu.lab.core.security.AuthenticatedUser;
import edu.lab.core.user.AppUser;
import edu.lab.core.user.dto.UserSummaryResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExperimentService {

	private final ExperimentRepository experimentRepository;
	private final CourseService courseService;
	private final edu.lab.core.user.UserRepository userRepository;

	@Transactional
	public ExperimentSummaryResponse createExperiment(AuthenticatedUser currentUser, UUID courseId, ExperimentCreateRequest request) {
		Course course = courseService.requireTeachingCourse(currentUser.id(), courseId);
		AppUser creator = userRepository.findById(currentUser.id())
			.orElseThrow(() -> new NotFoundException("当前用户不存在"));

		Experiment experiment = new Experiment();
		experiment.setCourse(course);
		experiment.setTitle(request.title().trim());
		experiment.setDescription(request.description());
		experiment.setDueAt(request.dueAt());
		experiment.setPublishedAt(java.time.LocalDateTime.now());
		experiment.setCreatedBy(creator);
		experiment = experimentRepository.save(experiment);
		return toSummary(experiment);
	}

	@Transactional(readOnly = true)
	public List<ExperimentSummaryResponse> listExperiments(AuthenticatedUser currentUser, UUID courseId) {
		courseService.requireAccessibleCourse(currentUser.id(), courseId);
		return experimentRepository.findByCourseIdOrderByPublishedAtDesc(courseId).stream().map(this::toSummary).toList();
	}

	@Transactional(readOnly = true)
	public ExperimentDetailResponse getExperimentDetail(AuthenticatedUser currentUser, UUID experimentId) {
		Experiment experiment = experimentRepository.findById(experimentId)
			.orElseThrow(() -> new NotFoundException("实验不存在"));
		courseService.requireAccessibleCourse(currentUser.id(), experiment.getCourse().getId());
		return new ExperimentDetailResponse(
			experiment.getId(),
			new CourseSummaryResponse(
				experiment.getCourse().getId(),
				experiment.getCourse().getTitle(),
				experiment.getCourse().getDescription(),
				experiment.getCourse().getInviteCode(),
				experiment.getCourse().getOwner().getId(),
				experiment.getCourse().getOwner().getUsername(),
				0,
				experiment.getCourse().getCreatedAt()
			),
			experiment.getTitle(),
			experiment.getDescription(),
			experiment.getPublishedAt(),
			experiment.getDueAt(),
			new UserSummaryResponse(
				experiment.getCreatedBy().getId(),
				experiment.getCreatedBy().getUsername(),
				experiment.getCreatedBy().getDisplayName(),
				experiment.getCreatedBy().getEmail(),
				experiment.getCreatedBy().getRole())
		);
	}

	@Transactional(readOnly = true)
	public Experiment requireAccessibleExperiment(UUID userId, UUID experimentId) {
		Experiment experiment = experimentRepository.findById(experimentId)
			.orElseThrow(() -> new NotFoundException("实验不存在"));
		courseService.requireAccessibleCourse(userId, experiment.getCourse().getId());
		return experiment;
	}

	@Transactional(readOnly = true)
	public Experiment requireTeachingExperiment(UUID userId, UUID experimentId) {
		Experiment experiment = experimentRepository.findById(experimentId)
			.orElseThrow(() -> new NotFoundException("实验不存在"));
		courseService.requireTeachingCourse(userId, experiment.getCourse().getId());
		return experiment;
	}

	private ExperimentSummaryResponse toSummary(Experiment experiment) {
		return new ExperimentSummaryResponse(
			experiment.getId(),
			experiment.getCourse().getId(),
			experiment.getTitle(),
			experiment.getDescription(),
			experiment.getPublishedAt(),
			experiment.getDueAt(),
			experiment.getCreatedBy().getId(),
			experiment.getCreatedBy().getUsername()
		);
	}
}