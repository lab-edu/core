package edu.lab.core.experiment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExperimentRepository extends JpaRepository<Experiment, UUID> {

	@Query("select e from Experiment e where e.course.id = :courseId order by e.publishedAt desc")
	List<Experiment> findByCourseIdOrderByPublishedAtDesc(@Param("courseId") UUID courseId);

	Optional<Experiment> findByIdAndCourseId(UUID id, UUID courseId);
}