package edu.lab.core.course.workspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseWorkspaceModuleRepository extends JpaRepository<CourseWorkspaceModule, UUID> {

	@Query("select m from CourseWorkspaceModule m where m.course.id = :courseId order by m.sortOrder asc, m.createdAt asc")
	List<CourseWorkspaceModule> findByCourseIdOrderBySortOrderAsc(@Param("courseId") UUID courseId);

	Optional<CourseWorkspaceModule> findByCourseIdAndModuleKey(UUID courseId, CourseWorkspaceModuleKey moduleKey);
}