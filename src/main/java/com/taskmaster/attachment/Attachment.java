package com.taskmaster.attachment;

import com.taskmaster.task.Task;
import com.taskmaster.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "attachments")
public class Attachment {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "task_id", nullable = false)
	private Task task;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "uploader_id", nullable = false)
	private User uploader;

	@Column(nullable = false, length = 255)
	private String filename;

	@Column(name = "content_type", nullable = false, length = 120)
	private String contentType;

	@Column(name = "storage_path", nullable = false, length = 500)
	private String storagePath;

	@Column(name = "size_bytes", nullable = false)
	private long sizeBytes;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Attachment() {
	}

	public Attachment(Task task, User uploader, String filename, String contentType, String storagePath, long sizeBytes) {
		this.task = task;
		this.uploader = uploader;
		this.filename = filename;
		this.contentType = contentType;
		this.storagePath = storagePath;
		this.sizeBytes = sizeBytes;
	}

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public Task getTask() {
		return task;
	}

	public User getUploader() {
		return uploader;
	}

	public String getFilename() {
		return filename;
	}

	public String getContentType() {
		return contentType;
	}

	public String getStoragePath() {
		return storagePath;
	}

	public long getSizeBytes() {
		return sizeBytes;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
