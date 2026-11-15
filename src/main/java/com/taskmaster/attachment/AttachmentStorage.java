package com.taskmaster.attachment;

import com.taskmaster.common.ApiException;
import com.taskmaster.common.BadRequestException;
import com.taskmaster.common.ResourceNotFoundException;
import com.taskmaster.config.TaskMasterProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class AttachmentStorage {

	private static final Logger log = LoggerFactory.getLogger(AttachmentStorage.class);

	private static final Set<String> ALLOWED_TYPES = Set.of(
			"image/png",
			"image/jpeg",
			"image/gif",
			"image/webp",
			"application/pdf",
			"text/plain",
			"text/csv",
			"application/json",
			"application/zip",
			"application/msword",
			"application/vnd.ms-excel",
			"application/vnd.openxmlformats-officedocument.wordprocessingml.document",
			"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

	private final Path root;
	private final long maxBytes;

	public AttachmentStorage(TaskMasterProperties properties) {
		this.root = Path.of(properties.attachments().directory()).toAbsolutePath().normalize();
		this.maxBytes = properties.attachments().maxBytes();
	}

	public StoredFile store(UUID taskId, MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BadRequestException("Attachment file is empty");
		}
		if (file.getSize() > maxBytes) {
			throw new BadRequestException("Attachment exceeds the 10 MB limit");
		}
		String contentType = normalizeContentType(file.getContentType());
		if (!ALLOWED_TYPES.contains(contentType)) {
			throw new BadRequestException("Unsupported attachment type");
		}
		String filename = sanitizeFilename(file.getOriginalFilename());
		String relativePath = taskId + "/" + UUID.randomUUID() + "-" + filename;
		Path destination = resolve(relativePath);
		try {
			Files.createDirectories(destination.getParent());
			try (InputStream input = file.getInputStream()) {
				Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
			}
		}
		catch (IOException exception) {
			log.warn("Failed to store attachment for task {}", taskId, exception);
			throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store attachment");
		}
		return new StoredFile(filename, contentType, relativePath, file.getSize());
	}

	public Resource load(String relativePath) {
		Path path = resolve(relativePath);
		if (!Files.exists(path)) {
			throw new ResourceNotFoundException("Attachment file not found");
		}
		return new FileSystemResource(path);
	}

	public void delete(String relativePath) {
		try {
			Files.deleteIfExists(resolve(relativePath));
		}
		catch (IOException exception) {
			log.warn("Failed to delete attachment {}", relativePath, exception);
			throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not delete attachment");
		}
	}

	private Path resolve(String relativePath) {
		Path path = root.resolve(relativePath).normalize();
		if (!path.startsWith(root)) {
			throw new BadRequestException("Invalid attachment path");
		}
		return path;
	}

	private static String normalizeContentType(String contentType) {
		if (contentType == null || contentType.isBlank()) {
			throw new BadRequestException("Attachment content type is required");
		}
		String normalized = contentType.toLowerCase(Locale.ROOT).split(";", 2)[0].trim();
		if ("image/jpg".equals(normalized)) {
			return "image/jpeg";
		}
		return normalized;
	}

	private static String sanitizeFilename(String original) {
		String name = original == null ? "file" : Path.of(original).getFileName().toString();
		String safe = name.replaceAll("[^A-Za-z0-9._-]", "_");
		if (safe.isBlank() || ".".equals(safe) || "..".equals(safe)) {
			safe = "file";
		}
		if (safe.length() > 80) {
			safe = safe.substring(safe.length() - 80);
		}
		return safe;
	}
}
