package edu.lab.core.submission;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

	@Query("select s from Submission s where s.experiment.id = :experimentId order by s.submittedAt desc")
	List<Submission> findByExperimentIdOrderBySubmittedAtDesc(@Param("experimentId") UUID experimentId);

	@Query("select s from Submission s where s.experiment.id = :experimentId and s.submittedBy.id = :userId order by s.submittedAt desc")
	List<Submission> findByExperimentIdAndSubmittedByIdOrderBySubmittedAtDesc(@Param("experimentId") UUID experimentId, @Param("userId") UUID userId);

	@Query("select s from Submission s join fetch s.experiment e join fetch s.submittedBy u where e.course.id = :courseId and s.latest = true order by s.submittedAt desc")
	List<Submission> findLatestByCourseId(@Param("courseId") UUID courseId);

	Optional<Submission> findByExperimentIdAndSubmittedByIdAndLatestTrue(UUID experimentId, UUID userId);

	@Modifying
	@Query("update Submission s set s.latest = false where s.experiment.id = :experimentId and s.submittedBy.id = :userId and s.latest = true")
	int markPreviousLatestFalse(@Param("experimentId") UUID experimentId, @Param("userId") UUID userId);
}