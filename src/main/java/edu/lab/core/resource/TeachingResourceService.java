package edu.lab.core.resource;

import edu.lab.core.common.exception.BadRequestException;
import edu.lab.core.common.exception.NotFoundException;
import edu.lab.core.course.Course;
import edu.lab.core.course.CourseService;
import edu.lab.core.resource.dto.ResourceSummaryResponse;
import edu.lab.core.security.AuthenticatedUser;
import edu.lab.core.storage.FileStorageService;
import edu.lab.core.user.AppUser;
import edu.lab.core.user.UserRepository;
import edu.lab.core.user.dto.UserSummaryResponse;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class TeachingResourceService {

	private static final Set<String> ALLOWED_FILE_EXTENSIONS = Set.of(
		"pdf", "ppt", "pptx", "doc", "docx", "txt", "md", "zip", "rar", "7z", "mp4", "webm", "mov"
	);

	private final TeachingResourceRepository teachingResourceRepository;
	private final CourseService courseService;
	private final UserRepository userRepository;
	private final FileStorageService fileStorageService;

	@Transactional
	public ResourceSummaryResponse createResource(AuthenticatedUser currentUser,
		UUID courseId,
		String name,
		ResourceType type,
		String category,
		String externalUrl,
		MultipartFile file) {
		Course course = courseService.requireTeachingCourse(currentUser.id(), courseId);
		AppUser uploader = requireUser(currentUser.id());
		String normalizedName = normalizeName(name);
		String normalizedCategory = normalizeText(category);

		validatePayload(type, externalUrl, file);

		TeachingResource resource = new TeachingResource();
		resource.setCourse(course);
		resource.setUploadedBy(uploader);
		resource.setName(normalizedName);
		resource.setType(type);
		resource.setCategory(normalizedCategory);
		resource.setUploadedAt(LocalDateTime.now());

		if (type == ResourceType.FILE) {
			ensureAllowedFileType(file.getOriginalFilename());
			FileStorageService.StoredFile storedFile = fileStorageService.saveResourceFile(courseId, resource.getId(), file);
			resource.setFileName(storedFile.fileName());
			resource.setFilePath(storedFile.filePath());
			resource.setContentType(storedFile.contentType());
		} else {
			resource.setExternalUrl(normalizeUrl(externalUrl));
		}

		resource = teachingResourceRepository.save(resource);
		return toSummary(resource);
	}

	@Transactional(readOnly = true)
	public List<ResourceSummaryResponse> listResources(AuthenticatedUser currentUser, UUID courseId) {
		courseService.requireAccessibleCourse(currentUser.id(), courseId);
		return teachingResourceRepository.findByCourseIdOrderByUploadedAtDesc(courseId).stream()
			.map(this::toSummary)
			.toList();
	}

	@Transactional(readOnly = true)
	public FileStorageService.StoredContent accessResourceFile(AuthenticatedUser currentUser, UUID resourceId) {
		TeachingResource resource = teachingResourceRepository.findById(resourceId)
			.orElseThrow(() -> new NotFoundException("资源不存在"));
		courseService.requireAccessibleCourse(currentUser.id(), resource.getCourse().getId());
		if (resource.getType() != ResourceType.FILE || resource.getFilePath() == null) {
			throw new BadRequestException("当前资源不是文件类型，无法下载");
		}
		return fileStorageService.openFile(resource.getFilePath(), resource.getFileName(), resource.getContentType());
	}

	@Transactional(readOnly = true)
	public FileStorageService.StoredContent accessResourceFilePublic(UUID resourceId) {
		TeachingResource resource = teachingResourceRepository.findById(resourceId)
			.orElseThrow(() -> new NotFoundException("资源不存在"));
		// 公开访问，跳过课程权限检查
		if (resource.getType() != ResourceType.FILE || resource.getFilePath() == null) {
			throw new BadRequestException("当前资源不是文件类型，无法下载");
		}
		return fileStorageService.openFile(resource.getFilePath(), resource.getFileName(), resource.getContentType());
	}

	private AppUser requireUser(UUID userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new NotFoundException("当前用户不存在"));
	}

	private void validatePayload(ResourceType type, String externalUrl, MultipartFile file) {
		if (type == null) {
			throw new BadRequestException("资源类型不能为空");
		}
		if (type == ResourceType.FILE) {
			if (file == null || file.isEmpty()) {
				throw new BadRequestException("文件资源必须上传文件");
			}
			return;
		}
		if (externalUrl == null || externalUrl.isBlank()) {
			throw new BadRequestException("链接资源必须提供 externalUrl");
		}
	}

	private String normalizeName(String name) {
		if (name == null || name.isBlank()) {
			throw new BadRequestException("资源名称不能为空");
		}
		return name.trim();
	}

	private String normalizeText(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.trim();
		return normalized.isEmpty() ? null : normalized;
	}

	private String normalizeUrl(String externalUrl) {
		String normalized = externalUrl.trim();
		try {
			URI uri = new URI(normalized);
			String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
			if (!"http".equals(scheme) && !"https".equals(scheme)) {
				throw new BadRequestException("externalUrl 仅支持 http/https");
			}
			return normalized;
		} catch (URISyntaxException exception) {
			throw new BadRequestException("externalUrl 格式不正确");
		}
	}

	private void ensureAllowedFileType(String fileName) {
		if (fileName == null || !fileName.contains(".")) {
			throw new BadRequestException("文件格式不支持");
		}
		String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
		if (!ALLOWED_FILE_EXTENSIONS.contains(extension)) {
			throw new BadRequestException("文件格式不支持，仅允许: " + String.join(", ", ALLOWED_FILE_EXTENSIONS));
		}
	}

	private ResourceSummaryResponse toSummary(TeachingResource resource) {
		return new ResourceSummaryResponse(
			resource.getId(),
			resource.getCourse().getId(),
			resource.getName(),
			resource.getType(),
			resource.getCategory(),
			resource.getFileName(),
			resource.getExternalUrl(),
			new UserSummaryResponse(
				resource.getUploadedBy().getId(),
				resource.getUploadedBy().getUsername(),
				resource.getUploadedBy().getDisplayName(),
				resource.getUploadedBy().getEmail(),
				resource.getUploadedBy().getRole()),
			resource.getUploadedAt()
		);
	}
}