package edu.lab.core.course.learning;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseLearningTaskSubmissionRepository extends JpaRepository<CourseLearningTaskSubmission, UUID> {

	Optional<CourseLearningTaskSubmission> findByTaskIdAndSubmittedByIdAndLatestTrue(UUID taskId, UUID userId);

	List<CourseLearningTaskSubmission> findByTaskIdOrderBySubmittedAtDesc(UUID taskId);

	List<CourseLearningTaskSubmission> findByTaskIdAndSubmittedByIdOrderBySubmittedAtDesc(UUID taskId, UUID userId);

	@Query("select s from CourseLearningTaskSubmission s join fetch s.task t join fetch t.knowledgePoint p join fetch p.unit u join fetch s.submittedBy left join fetch s.gradedBy where u.course.id = :courseId and s.latest = true")
	List<CourseLearningTaskSubmission> findLatestByCourseId(@Param("courseId") UUID courseId);

	@Query("select s from CourseLearningTaskSubmission s join fetch s.task t join fetch t.knowledgePoint p join fetch p.unit u join fetch s.submittedBy left join fetch s.gradedBy where s.task.id = :taskId order by s.submittedAt desc")
	List<CourseLearningTaskSubmission> findTaskSubmissionsWithUsers(@Param("taskId") UUID taskId);

	@Query("select s from CourseLearningTaskSubmission s join fetch s.task t where s.submittedBy.id = :userId and s.latest = true and s.task.id in :taskIds")
	List<CourseLearningTaskSubmission> findLatestByUserIdAndTaskIds(@Param("userId") UUID userId, @Param("taskIds") List<UUID> taskIds);

	@Query("select count(s) from CourseLearningTaskSubmission s where s.task.id = :taskId and s.latest = true")
	long countLatestByTaskId(@Param("taskId") UUID taskId);
}