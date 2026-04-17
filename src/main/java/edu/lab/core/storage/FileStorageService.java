package edu.lab.core.storage;

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
			Path basePath = Path.of(storageProperties.basePath()).toAbsolutePath().normalize();
			Files.createDirectories(basePath);
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

	private String sanitizeName(String originalFilename) {
		if (originalFilename == null || originalFilename.isBlank()) {
			return UUID.randomUUID() + ".bin";
		}
		String name = Path.of(originalFilename).getFileName().toString();
		if (name.isBlank()) {
			return UUID.randomUUID() + ".bin";
		}
		return name.replaceAll("[^a-zA-Z0-9._-]", "_");
	}

	public record StoredFile(String filePath, String fileName, String contentType) {
	}
}