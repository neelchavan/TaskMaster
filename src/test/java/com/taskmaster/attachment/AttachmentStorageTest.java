package com.taskmaster.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.taskmaster.common.BadRequestException;
import com.taskmaster.config.TaskMasterProperties;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.util.UUID;

class AttachmentStorageTest {

	@TempDir
	Path tempDir;

	@Test
	void storesAPlainTextFileUnderTheTaskDirectory() throws Exception {
		AttachmentStorage storage = storage();
		UUID taskId = UUID.randomUUID();
		MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8));

		StoredFile stored = storage.store(taskId, file);

		assertThat(stored.filename()).isEqualTo("notes.txt");
		assertThat(stored.contentType()).isEqualTo("text/plain");
		assertThat(stored.relativePath()).startsWith(taskId + "/");
		assertThat(storage.load(stored.relativePath()).getContentAsByteArray()).isEqualTo("hello".getBytes(StandardCharsets.UTF_8));
	}

	@Test
	void rejectsUnsupportedTypesAndPathsOutsideTheRoot() {
		AttachmentStorage storage = storage();
		MockMultipartFile file = new MockMultipartFile("file", "run.exe", "application/octet-stream", "MZ".getBytes(StandardCharsets.UTF_8));

		assertThatThrownBy(() -> storage.store(UUID.randomUUID(), file)).isInstanceOf(BadRequestException.class);
		assertThatThrownBy(() -> storage.delete("../secrets.txt")).isInstanceOf(BadRequestException.class);
	}

	private AttachmentStorage storage() {
		return new AttachmentStorage(new TaskMasterProperties(
				new TaskMasterProperties.Jwt("test-secret-key-must-be-at-least-32-bytes", 15, 7),
				new TaskMasterProperties.Attachments(tempDir.toString(), 1024)));
	}
}
