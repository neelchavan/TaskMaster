package com.taskmaster.attachment;

import com.taskmaster.attachment.dto.AttachmentResponse;
import com.taskmaster.common.ForbiddenException;
import com.taskmaster.common.ResourceNotFoundException;
import com.taskmaster.task.Task;
import com.taskmaster.team.TeamAccess;
import com.taskmaster.user.User;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AttachmentService {

	private final AttachmentRepository attachments;
	private final AttachmentStorage storage;
	private final TeamAccess access;

	public AttachmentService(AttachmentRepository attachments, AttachmentStorage storage, TeamAccess access) {
		this.attachments = attachments;
		this.storage = storage;
		this.access = access;
	}

	@Transactional(readOnly = true)
	public List<AttachmentResponse> list(UUID taskId, UUID userId) {
		Task task = access.requireTask(taskId, userId);
		return attachments.findByTaskId(task.getId()).stream().map(AttachmentResponse::from).toList();
	}

	@Transactional
	public AttachmentResponse upload(UUID taskId, UUID userId, MultipartFile file) {
		Task task = access.requireTask(taskId, userId);
		User uploader = access.requireMember(task.getTeam().getId(), userId).getUser();
		StoredFile stored = storage.store(task.getId(), file);
		try {
			Attachment attachment = attachments.save(new Attachment(
					task,
					uploader,
					stored.filename(),
					stored.contentType(),
					stored.relativePath(),
					stored.sizeBytes()));
			return AttachmentResponse.from(attachment);
		}
		catch (RuntimeException exception) {
			storage.delete(stored.relativePath());
			throw exception;
		}
	}

	@Transactional(readOnly = true)
	public Download download(UUID taskId, UUID attachmentId, UUID userId) {
		Attachment attachment = requireVisible(taskId, attachmentId, userId);
		return new Download(attachment, storage.load(attachment.getStoragePath()));
	}

	@Transactional
	public void delete(UUID taskId, UUID attachmentId, UUID userId) {
		Attachment attachment = requireVisible(taskId, attachmentId, userId);
		boolean uploader = attachment.getUploader().getId().equals(userId);
		boolean owner = attachment.getTask().getTeam().getOwner().getId().equals(userId);
		if (!uploader && !owner) {
			throw new ForbiddenException("Only the uploader or team owner can delete this attachment");
		}
		storage.delete(attachment.getStoragePath());
		attachments.delete(attachment);
	}

	private Attachment requireVisible(UUID taskId, UUID attachmentId, UUID userId) {
		Attachment attachment = attachments.findDetailed(taskId, attachmentId)
				.orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));
		access.requireMember(attachment.getTask().getTeam().getId(), userId);
		return attachment;
	}

	public record Download(Attachment attachment, Resource resource) {
	}
}
