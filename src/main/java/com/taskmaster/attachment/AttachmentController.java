package com.taskmaster.attachment;

import com.taskmaster.attachment.dto.AttachmentResponse;
import com.taskmaster.common.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tasks/{taskId}/attachments")
@Tag(name = "Attachments")
public class AttachmentController {

	private final AttachmentService attachmentService;

	public AttachmentController(AttachmentService attachmentService) {
		this.attachmentService = attachmentService;
	}

	@GetMapping
	@Operation(summary = "List attachments on a task")
	public List<AttachmentResponse> list(@PathVariable UUID taskId) {
		return attachmentService.list(taskId, CurrentUser.id());
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Upload an attachment")
	public AttachmentResponse upload(@PathVariable UUID taskId, @RequestPart("file") MultipartFile file) {
		return attachmentService.upload(taskId, CurrentUser.id(), file);
	}

	@GetMapping("/{attachmentId}")
	@Operation(summary = "Download an attachment")
	public ResponseEntity<org.springframework.core.io.Resource> download(
			@PathVariable UUID taskId,
			@PathVariable UUID attachmentId) {
		AttachmentService.Download download = attachmentService.download(taskId, attachmentId, CurrentUser.id());
		ContentDisposition disposition = ContentDisposition.attachment()
				.filename(download.attachment().getFilename(), StandardCharsets.UTF_8)
				.build();
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(download.attachment().getContentType()))
				.header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
				.body(download.resource());
	}

	@DeleteMapping("/{attachmentId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete an attachment")
	public void delete(@PathVariable UUID taskId, @PathVariable UUID attachmentId) {
		attachmentService.delete(taskId, attachmentId, CurrentUser.id());
	}
}
