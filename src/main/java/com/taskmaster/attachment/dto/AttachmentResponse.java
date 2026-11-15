package com.taskmaster.attachment.dto;

import com.taskmaster.attachment.Attachment;
import com.taskmaster.user.dto.UserSummary;
import java.time.Instant;
import java.util.UUID;

public record AttachmentResponse(
		UUID id,
		UUID taskId,
		String filename,
		String contentType,
		long sizeBytes,
		UserSummary uploader,
		Instant createdAt) {

	public static AttachmentResponse from(Attachment attachment) {
		return new AttachmentResponse(
				attachment.getId(),
				attachment.getTask().getId(),
				attachment.getFilename(),
				attachment.getContentType(),
				attachment.getSizeBytes(),
				UserSummary.from(attachment.getUploader()),
				attachment.getCreatedAt());
	}
}
