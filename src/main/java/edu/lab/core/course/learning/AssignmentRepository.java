package edu.lab.core.course.learning;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

    @Query("select a from Assignment a join fetch a.createdBy where a.course.id = :courseId order by a.sortOrder asc, a.createdAt asc")
    List<Assignment> findByCourseIdOrderBySortOrderAscCreatedAtAsc(@Param("courseId") UUID courseId);

    @Query("select a from Assignment a join fetch a.createdBy join fetch a.course where a.id = :assignmentId")
    Optional<Assignment> findWithCourseById(@Param("assignmentId") UUID assignmentId);

    @Query("select a from Assignment a join fetch a.createdBy join fetch a.course where a.course.id = :courseId and a.published = true order by a.sortOrder asc, a.createdAt asc")
    List<Assignment> findPublishedByCourseId(@Param("courseId") UUID courseId);

    @Query("select a from Assignment a join fetch a.createdBy join fetch a.course where a.course.id in :courseIds and a.published = true order by a.dueAt asc nulls last, a.startAt asc nulls last, a.createdAt asc")
    List<Assignment> findPublishedByCourseIds(@Param("courseIds") List<UUID> courseIds);

    @Query("select a from Assignment a join fetch a.createdBy join fetch a.course where a.course.id = :courseId and a.published = true and a.dueAt > current_timestamp order by a.dueAt asc, a.sortOrder asc")
    List<Assignment> findActiveByCourseId(@Param("courseId") UUID courseId);
}