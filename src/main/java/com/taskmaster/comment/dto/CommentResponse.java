package com.taskmaster.comment.dto;

import com.taskmaster.comment.Comment;
import com.taskmaster.user.dto.UserSummary;
import java.time.Instant;
import java.util.UUID;

public record CommentResponse(UUID id, UUID taskId, UserSummary author, String body, Instant createdAt) {

	public static CommentResponse from(Comment comment) {
		return new CommentResponse(
				comment.getId(),
				comment.getTask().getId(),
				UserSummary.from(comment.getAuthor()),
				comment.getBody(),
				comment.getCreatedAt());
	}
}
