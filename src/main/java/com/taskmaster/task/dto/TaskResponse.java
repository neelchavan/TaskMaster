package com.taskmaster.task.dto;

import com.taskmaster.task.Task;
import com.taskmaster.task.TaskStatus;
import com.taskmaster.user.dto.UserSummary;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TaskResponse(
		UUID id,
		UUID teamId,
		String teamName,
		String title,
		String description,
		LocalDate dueDate,
		TaskStatus status,
		UserSummary creator,
		UserSummary assignee,
		Instant createdAt,
		Instant updatedAt) {

	public static TaskResponse from(Task task) {
		return new TaskResponse(
				task.getId(),
				task.getTeam().getId(),
				task.getTeam().getName(),
				task.getTitle(),
				task.getDescription(),
				task.getDueDate(),
				task.getStatus(),
				UserSummary.from(task.getCreator()),
				UserSummary.from(task.getAssignee()),
				task.getCreatedAt(),
				task.getUpdatedAt());
	}
}
