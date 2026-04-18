package edu.lab.core.course.announcement;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseAnnouncementRepository extends JpaRepository<CourseAnnouncement, UUID> {

	@Query("select a from CourseAnnouncement a join fetch a.createdBy where a.course.id = :courseId order by a.createdAt desc")
	List<CourseAnnouncement> findByCourseIdOrderByCreatedAtDesc(@Param("courseId") UUID courseId);
}