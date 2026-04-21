package edu.lab.core.course.learning;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentTaskItemRepository extends JpaRepository<AssignmentTaskItem, UUID> {

    @Query("select i from AssignmentTaskItem i join fetch i.assignment where i.assignment.id = :assignmentId order by i.sortOrder asc, i.createdAt asc")
    List<AssignmentTaskItem> findByAssignmentIdOrderBySortOrderAscCreatedAtAsc(@Param("assignmentId") UUID assignmentId);

    @Query("select i from AssignmentTaskItem i where i.originalTaskId = :originalTaskId")
    List<AssignmentTaskItem> findByOriginalTaskId(@Param("originalTaskId") UUID originalTaskId);

    @Query("select i from AssignmentTaskItem i where i.assignment.id in :assignmentIds order by i.assignment.id, i.sortOrder asc, i.createdAt asc")
    List<AssignmentTaskItem> findByAssignmentIdsOrderByAssignmentIdAndSortOrderAsc(@Param("assignmentIds") List<UUID> assignmentIds);

    void deleteByAssignmentId(UUID assignmentId);
}