package com.taskmaster.task;

import com.taskmaster.common.CurrentUser;
import com.taskmaster.task.dto.CreateTaskRequest;
import com.taskmaster.task.dto.TaskResponse;
import com.taskmaster.task.dto.UpdateTaskRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Tasks")
public class TaskController {

	private final TaskService taskService;

	public TaskController(TaskService taskService) {
		this.taskService = taskService;
	}

	@PostMapping("/teams/{teamId}/tasks")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create a task in a team")
	public TaskResponse create(@PathVariable UUID teamId, @Valid @RequestBody CreateTaskRequest request) {
		return taskService.create(teamId, CurrentUser.id(), request);
	}

	@GetMapping("/tasks")
	@Operation(summary = "List visible tasks, with status, assignee, text search, and sort")
	public List<TaskResponse> list(
			@RequestParam(required = false) TaskStatus status,
			@RequestParam(required = false) UUID teamId,
			@RequestParam(required = false) String assignee,
			@RequestParam(required = false) String q,
			@RequestParam(required = false, defaultValue = "createdAt,desc") String sort) {
		return taskService.list(CurrentUser.id(), status, teamId, assignee, q, sort);
	}

	@GetMapping("/tasks/{taskId}")
	@Operation(summary = "View a task")
	public TaskResponse get(@PathVariable UUID taskId) {
		return taskService.get(taskId, CurrentUser.id());
	}

	@PatchMapping("/tasks/{taskId}")
	@Operation(summary = "Update a task, including assignment and status")
	public TaskResponse update(@PathVariable UUID taskId, @Valid @RequestBody UpdateTaskRequest request) {
		return taskService.update(taskId, CurrentUser.id(), request);
	}

	@PostMapping("/tasks/{taskId}/complete")
	@Operation(summary = "Mark a task completed")
	public TaskResponse complete(@PathVariable UUID taskId) {
		return taskService.complete(taskId, CurrentUser.id());
	}

	@DeleteMapping("/tasks/{taskId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete a task")
	public void delete(@PathVariable UUID taskId) {
		taskService.delete(taskId, CurrentUser.id());
	}
}
