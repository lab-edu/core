package edu.lab.core.course.learning;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseLearningUnitRepository extends JpaRepository<CourseLearningUnit, UUID> {

	@Query("select u from CourseLearningUnit u join fetch u.createdBy where u.course.id = :courseId order by u.sortOrder asc, u.createdAt asc")
	List<CourseLearningUnit> findByCourseIdOrderBySortOrderAscCreatedAtAsc(@Param("courseId") UUID courseId);
}