package com.taskmaster.attachment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {

	@Query("""
			select attachment from Attachment attachment
			join fetch attachment.uploader
			where attachment.task.id = :taskId
			order by attachment.createdAt asc
			""")
	List<Attachment> findByTaskId(UUID taskId);

	@Query("""
			select attachment from Attachment attachment
			join fetch attachment.uploader
			join fetch attachment.task task
			join fetch task.team team
			join fetch team.owner
			where attachment.id = :attachmentId and attachment.task.id = :taskId
			""")
	Optional<Attachment> findDetailed(UUID taskId, UUID attachmentId);
}
