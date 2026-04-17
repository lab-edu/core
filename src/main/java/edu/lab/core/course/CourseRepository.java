package edu.lab.core.course;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, UUID> {

	boolean existsByInviteCodeIgnoreCase(String inviteCode);

	Optional<Course> findByInviteCodeIgnoreCase(String inviteCode);

	@Query("select c from Course c where c.owner.id = :ownerId order by c.createdAt desc")
	List<Course> findOwnedCourses(@Param("ownerId") UUID ownerId);
}