package edu.lab.core.storage;

import edu.lab.core.common.exception.NotFoundException;
import edu.lab.core.config.StorageProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FileStorageService {

	private final StorageProperties storageProperties;

	public StoredFile saveSubmissionFile(UUID submissionId, MultipartFile file) {
		try {
			Path basePath = getBasePath();
			String originalName = sanitizeName(file.getOriginalFilename());
			Path directory = basePath.resolve("submissions").resolve(submissionId.toString());
			Files.createDirectories(directory);
			Path target = directory.resolve(originalName);
			try (InputStream inputStream = file.getInputStream()) {
				Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
			}
			return new StoredFile(target.toAbsolutePath().toString(), originalName, file.getContentType());
		} catch (IOException exception) {
			throw new IllegalStateException("文件保存失败", exception);
		}
	}

	public StoredFile saveResourceFile(UUID courseId, UUID resourceId, MultipartFile file) {
		try {
			Path basePath = getBasePath();
			String originalName = sanitizeName(file.getOriginalFilename());
			Path directory = basePath.resolve("resources").resolve(courseId.toString()).resolve(resourceId.toString());
			Files.createDirectories(directory);
			Path target = directory.resolve(originalName);
			try (InputStream inputStream = file.getInputStream()) {
				Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
			}
			return new StoredFile(target.toAbsolutePath().toString(), originalName, file.getContentType());
		} catch (IOException exception) {
			throw new IllegalStateException("文件保存失败", exception);
		}
	}

	public StoredFile saveLearningTaskFile(UUID courseId, UUID taskId, MultipartFile file) {
		try {
			Path basePath = getBasePath();
			String originalName = sanitizeName(file.getOriginalFilename());
			Path directory = basePath.resolve("learning").resolve("tasks").resolve(courseId.toString()).resolve(taskId.toString());
			Files.createDirectories(directory);
			Path target = directory.resolve(originalName);
			try (InputStream inputStream = file.getInputStream()) {
				Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
			}
			return new StoredFile(target.toAbsolutePath().toString(), originalName, file.getContentType());
		} catch (IOException exception) {
			throw new IllegalStateException("文件保存失败", exception);
		}
	}

	public StoredFile saveLearningTaskSubmissionFile(UUID taskId, UUID submissionId, MultipartFile file) {
		try {
			Path basePath = getBasePath();
			String originalName = sanitizeName(file.getOriginalFilename());
			Path directory = basePath.resolve("learning").resolve("task-submissions").resolve(taskId.toString()).resolve(submissionId.toString());
			Files.createDirectories(directory);
			Path target = directory.resolve(originalName);
			try (InputStream inputStream = file.getInputStream()) {
				Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
			}
			return new StoredFile(target.toAbsolutePath().toString(), originalName, file.getContentType());
		} catch (IOException exception) {
			throw new IllegalStateException("文件保存失败", exception);
		}
	}

	public StoredContent openFile(String filePath, String fileName, String contentType) {
		try {
			Path path = Path.of(filePath).toAbsolutePath().normalize();
			Path basePath = getBasePath();
			if (!path.startsWith(basePath)) {
				throw new IllegalStateException("文件路径不合法");
			}
			if (!Files.exists(path) || !Files.isRegularFile(path)) {
				throw new NotFoundException("文件不存在或已失效");
			}
			long fileSize = Files.size(path);
			return new StoredContent(Files.newInputStream(path), fileSize, contentType, fileName);
		} catch (IOException exception) {
			throw new IllegalStateException("读取文件失败", exception);
		}
	}

	private Path getBasePath() throws IOException {
		Path basePath = Path.of(storageProperties.basePath()).toAbsolutePath().normalize();
		Files.createDirectories(basePath);
		return basePath;
	}

	private String sanitizeName(String originalFilename) {
		if (originalFilename == null || originalFilename.isBlank()) {
			return UUID.randomUUID() + ".bin";
		}
		String name = Path.of(originalFilename).getFileName().toString();
		if (name.isBlank()) {
			return UUID.randomUUID() + ".bin";
		}
		return name.replaceAll("[^\\p{L}\\p{N} ._-]", "_");
	}

	public record StoredFile(String filePath, String fileName, String contentType) {
	}

	public record StoredContent(InputStream inputStream, long size, String contentType, String fileName) {
	}
}