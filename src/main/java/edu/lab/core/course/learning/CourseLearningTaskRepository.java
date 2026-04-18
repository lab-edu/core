package edu.lab.core.course.learning;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseLearningTaskRepository extends JpaRepository<CourseLearningTask, UUID> {

	@Query("select t from CourseLearningTask t join fetch t.createdBy where t.knowledgePoint.id = :pointId order by t.sortOrder asc, t.createdAt asc")
	List<CourseLearningTask> findByKnowledgePointIdOrderBySortOrderAscCreatedAtAsc(@Param("pointId") UUID pointId);

	@Query("select t from CourseLearningTask t join fetch t.createdBy join fetch t.knowledgePoint p join fetch p.unit u join fetch u.course where t.id = :taskId")
	Optional<CourseLearningTask> findWithCourseById(@Param("taskId") UUID taskId);
}