package edu.lab.core.course;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseMemberRepository extends JpaRepository<CourseMember, UUID> {

	boolean existsByCourseIdAndUserId(UUID courseId, UUID userId);

	Optional<CourseMember> findByCourseIdAndUserId(UUID courseId, UUID userId);

	@Query("select m from CourseMember m join fetch m.user where m.course.id = :courseId order by m.createdAt asc")
	List<CourseMember> findMembers(@Param("courseId") UUID courseId);

	@Query("select distinct m.course from CourseMember m where m.user.id = :userId and m.memberRole = :memberRole order by m.course.createdAt desc")
	List<Course> findCoursesByMemberRole(@Param("userId") UUID userId, @Param("memberRole") CourseMemberRole memberRole);

	@Query("select m.user.id from CourseMember m where m.course.id = :courseId and m.memberRole = 'STUDENT'")
	List<UUID> findStudentIdsByCourseId(@Param("courseId") UUID courseId);

	@Query("select distinct m.course.id from CourseMember m where m.user.id = :userId")
	List<UUID> findCourseIdsByUserId(@Param("userId") UUID userId);

	@Modifying
	@Query("delete from CourseMember m where m.course.id = :courseId and m.user.id = :userId")
	int deleteByCourseIdAndUserId(@Param("courseId") UUID courseId, @Param("userId") UUID userId);
}