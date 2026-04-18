package edu.lab.core.course.learning;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseLearningPointRepository extends JpaRepository<CourseLearningPoint, UUID> {

	@Query("select p from CourseLearningPoint p join fetch p.createdBy where p.unit.id = :unitId order by p.sortOrder asc, p.createdAt asc")
	List<CourseLearningPoint> findByUnitIdOrderBySortOrderAscCreatedAtAsc(@Param("unitId") UUID unitId);
}