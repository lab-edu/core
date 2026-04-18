package edu.lab.core.resource;

import edu.lab.core.common.api.ApiResponse;
import edu.lab.core.resource.dto.ResourceCreateResponse;
import edu.lab.core.resource.dto.ResourceListResponse;
import edu.lab.core.security.AuthenticatedUser;
import edu.lab.core.storage.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class TeachingResourceController {

	private final TeachingResourceService teachingResourceService;

	@PostMapping(value = "/api/v1/courses/{courseId}/resources", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "教师上传课程资源")
	public ResponseEntity<ApiResponse<ResourceCreateResponse>> createResource(@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable UUID courseId,
		@RequestParam("name") String name,
		@RequestParam("type") ResourceType type,
		@RequestParam(value = "category", required = false) String category,
		@RequestParam(value = "externalUrl", required = false) String externalUrl,
		@RequestParam(value = "file", required = false) MultipartFile file) {
		return ResponseEntity.status(201).body(ApiResponse.created(new ResourceCreateResponse(
			teachingResourceService.createResource(currentUser, courseId, name, type, category, externalUrl, file))));
	}

	@GetMapping("/api/v1/courses/{courseId}/resources")
	@Operation(summary = "课程资源列表")
	public ResponseEntity<ApiResponse<ResourceListResponse>> listResources(@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable UUID courseId) {
		return ResponseEntity.ok(ApiResponse.ok(new ResourceListResponse(teachingResourceService.listResources(currentUser, courseId))));
	}

	@GetMapping("/api/v1/resources/{resourceId}/file")
	@Operation(summary = "下载课程文件资源")
	public ResponseEntity<InputStreamResource> accessFile(@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable UUID resourceId) {
		FileStorageService.StoredContent file = teachingResourceService.accessResourceFile(currentUser, resourceId);
		MediaType contentType = parseMediaType(file.contentType());
		ContentDisposition disposition = ContentDisposition.attachment().filename(file.fileName()).build();

		return ResponseEntity.ok()
			.contentType(contentType)
			.contentLength(file.size())
			.header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
			.body(new InputStreamResource(file.inputStream()));
	}

	private MediaType parseMediaType(String rawType) {
		if (rawType == null || rawType.isBlank()) {
			return MediaType.APPLICATION_OCTET_STREAM;
		}
		try {
			return MediaType.parseMediaType(rawType);
		} catch (IllegalArgumentException ignored) {
			return MediaType.APPLICATION_OCTET_STREAM;
		}
	}
}