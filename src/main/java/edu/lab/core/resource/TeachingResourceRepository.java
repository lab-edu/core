package edu.lab.core.resource;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeachingResourceRepository extends JpaRepository<TeachingResource, UUID> {

	@Query("select r from TeachingResource r join fetch r.uploadedBy where r.course.id = :courseId order by r.uploadedAt desc")
	List<TeachingResource> findByCourseIdOrderByUploadedAtDesc(@Param("courseId") UUID courseId);
}