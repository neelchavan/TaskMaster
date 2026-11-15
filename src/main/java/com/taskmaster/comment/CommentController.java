package com.taskmaster.comment;

import com.taskmaster.comment.dto.CommentResponse;
import com.taskmaster.comment.dto.CreateCommentRequest;
import com.taskmaster.common.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks/{taskId}/comments")
@Tag(name = "Comments")
public class CommentController {

	private final CommentService commentService;

	public CommentController(CommentService commentService) {
		this.commentService = commentService;
	}

	@GetMapping
	@Operation(summary = "List comments on a task")
	public List<CommentResponse> list(@PathVariable UUID taskId) {
		return commentService.list(taskId, CurrentUser.id());
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Add a comment")
	public CommentResponse add(@PathVariable UUID taskId, @Valid @RequestBody CreateCommentRequest request) {
		return commentService.add(taskId, CurrentUser.id(), request.body());
	}

	@DeleteMapping("/{commentId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete a comment")
	public void delete(@PathVariable UUID taskId, @PathVariable UUID commentId) {
		commentService.delete(taskId, commentId, CurrentUser.id());
	}
}
