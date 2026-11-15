package com.taskmaster.task;

import com.taskmaster.attachment.AttachmentRepository;
import com.taskmaster.attachment.AttachmentStorage;
import com.taskmaster.common.BadRequestException;
import com.taskmaster.common.ForbiddenException;
import com.taskmaster.task.dto.CreateTaskRequest;
import com.taskmaster.task.dto.TaskResponse;
import com.taskmaster.task.dto.UpdateTaskRequest;
import com.taskmaster.team.TeamAccess;
import com.taskmaster.team.TeamMembership;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

	private final TaskRepository tasks;
	private final TeamAccess access;
	private final AttachmentRepository attachments;
	private final AttachmentStorage storage;

	public TaskService(
			TaskRepository tasks,
			TeamAccess access,
			AttachmentRepository attachments,
			AttachmentStorage storage) {
		this.tasks = tasks;
		this.access = access;
		this.attachments = attachments;
		this.storage = storage;
	}

	@Transactional
	public TaskResponse create(UUID teamId, UUID userId, CreateTaskRequest request) {
		TeamMembership membership = access.requireMember(teamId, userId);
		Task task = new Task(
				membership.getTeam(),
				request.title().trim(),
				blankToNull(request.description()),
				request.dueDate(),
				membership.getUser());
		if (request.assigneeId() != null) {
			task.setAssignee(access.requireAssignee(teamId, request.assigneeId()));
		}
		return TaskResponse.from(tasks.save(task));
	}

	@Transactional(readOnly = true)
	public List<TaskResponse> list(UUID userId, TaskStatus status, UUID teamId, String assignee, String query, String sort) {
		UUID assigneeId = TaskQuery.parseAssignee(assignee, userId);
		Sort ordering = TaskQuery.parseSort(sort);
		List<UUID> teamIds = access.teamIdsFor(userId);
		if (teamId != null) {
			access.requireMember(teamId, userId);
			teamIds = List.of(teamId);
		}
		if (teamIds.isEmpty()) {
			return List.of();
		}
		String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
		return tasks.findAll(TaskSpecifications.matching(teamIds, status, assigneeId, normalizedQuery), ordering)
				.stream()
				.map(TaskResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public TaskResponse get(UUID taskId, UUID userId) {
		return TaskResponse.from(access.requireTask(taskId, userId));
	}

	@Transactional
	public TaskResponse update(UUID taskId, UUID userId, UpdateTaskRequest request) {
		if (Boolean.TRUE.equals(request.unassign()) && request.assigneeId() != null) {
			throw new BadRequestException("Send either assigneeId or unassign, not both");
		}
		Task task = access.requireTask(taskId, userId);
		if (request.title() != null) {
			String title = request.title().trim();
			if (title.isEmpty()) {
				throw new BadRequestException("Title cannot be blank");
			}
			task.setTitle(title);
		}
		if (request.description() != null) {
			task.setDescription(blankToNull(request.description()));
		}
		if (Boolean.TRUE.equals(request.clearDueDate())) {
			task.setDueDate(null);
		}
		else if (request.dueDate() != null) {
			task.setDueDate(request.dueDate());
		}
		if (request.status() != null) {
			task.setStatus(request.status());
		}
		if (Boolean.TRUE.equals(request.unassign())) {
			task.setAssignee(null);
		}
		else if (request.assigneeId() != null) {
			task.setAssignee(access.requireAssignee(task.getTeam().getId(), request.assigneeId()));
		}
		return TaskResponse.from(task);
	}

	@Transactional
	public TaskResponse complete(UUID taskId, UUID userId) {
		Task task = access.requireTask(taskId, userId);
		task.setStatus(TaskStatus.COMPLETED);
		return TaskResponse.from(task);
	}

	@Transactional
	public void delete(UUID taskId, UUID userId) {
		Task task = access.requireTask(taskId, userId);
		boolean creator = task.getCreator().getId().equals(userId);
		boolean owner = task.getTeam().getOwner().getId().equals(userId);
		if (!creator && !owner) {
			throw new ForbiddenException("Only the task creator or team owner can delete this task");
		}
		attachments.findByTaskId(task.getId()).forEach(attachment -> storage.delete(attachment.getStoragePath()));
		tasks.delete(task);
	}

	private static String blankToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
