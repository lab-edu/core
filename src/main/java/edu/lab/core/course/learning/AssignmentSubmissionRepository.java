package edu.lab.core.course.learning;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, UUID> {

    @Query("select s from AssignmentSubmission s join fetch s.submittedBy join fetch s.assignment where s.assignment.id = :assignmentId order by s.submittedAt desc")
    List<AssignmentSubmission> findByAssignmentIdOrderBySubmittedAtDesc(@Param("assignmentId") UUID assignmentId);

    @Query("select s from AssignmentSubmission s join fetch s.submittedBy join fetch s.assignment where s.assignment.id = :assignmentId and s.submittedBy.id = :userId order by s.submittedAt desc")
    List<AssignmentSubmission> findByAssignmentIdAndUserIdOrderBySubmittedAtDesc(@Param("assignmentId") UUID assignmentId, @Param("userId") UUID userId);

    @Query("select s from AssignmentSubmission s join fetch s.submittedBy join fetch s.assignment where s.assignment.id = :assignmentId and s.submittedBy.id = :userId and s.latest = true")
    Optional<AssignmentSubmission> findLatestByAssignmentIdAndUserId(@Param("assignmentId") UUID assignmentId, @Param("userId") UUID userId);

    @Query("select s from AssignmentSubmission s join fetch s.submittedBy join fetch s.assignment where s.assignment.course.id = :courseId order by s.submittedAt desc")
    List<AssignmentSubmission> findByCourseIdOrderBySubmittedAtDesc(@Param("courseId") UUID courseId);

    @Query("select s from AssignmentSubmission s join fetch s.submittedBy join fetch s.assignment where s.assignment.course.id = :courseId and s.submittedBy.id = :userId order by s.submittedAt desc")
    List<AssignmentSubmission> findByCourseIdAndUserIdOrderBySubmittedAtDesc(@Param("courseId") UUID courseId, @Param("userId") UUID userId);
}