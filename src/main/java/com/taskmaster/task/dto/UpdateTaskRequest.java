package com.taskmaster.task.dto;

import com.taskmaster.task.TaskStatus;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateTaskRequest(
		@Size(max = 200) String title,
		@Size(max = 5000) String description,
		LocalDate dueDate,
		Boolean clearDueDate,
		TaskStatus status,
		UUID assigneeId,
		Boolean unassign) {
}
