package com.taskmaster.comment;

import com.taskmaster.comment.dto.CommentResponse;
import com.taskmaster.common.ForbiddenException;
import com.taskmaster.common.ResourceNotFoundException;
import com.taskmaster.task.Task;
import com.taskmaster.team.TeamAccess;
import com.taskmaster.user.User;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

	private final CommentRepository comments;
	private final TeamAccess access;

	public CommentService(CommentRepository comments, TeamAccess access) {
		this.comments = comments;
		this.access = access;
	}

	@Transactional(readOnly = true)
	public List<CommentResponse> list(UUID taskId, UUID userId) {
		Task task = access.requireTask(taskId, userId);
		return comments.findByTaskId(task.getId()).stream().map(CommentResponse::from).toList();
	}

	@Transactional
	public CommentResponse add(UUID taskId, UUID userId, String body) {
		Task task = access.requireTask(taskId, userId);
		User author = access.requireMember(task.getTeam().getId(), userId).getUser();
		Comment comment = comments.save(new Comment(task, author, body.trim()));
		return CommentResponse.from(comment);
	}

	@Transactional
	public void delete(UUID taskId, UUID commentId, UUID userId) {
		Comment comment = comments.findDetailed(taskId, commentId)
				.orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
		access.requireMember(comment.getTask().getTeam().getId(), userId);
		boolean author = comment.getAuthor().getId().equals(userId);
		boolean owner = comment.getTask().getTeam().getOwner().getId().equals(userId);
		if (!author && !owner) {
			throw new ForbiddenException("Only the comment author or team owner can delete this comment");
		}
		comments.delete(comment);
	}
}
