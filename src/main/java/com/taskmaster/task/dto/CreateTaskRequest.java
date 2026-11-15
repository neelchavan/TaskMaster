package com.taskmaster.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record CreateTaskRequest(
		@NotBlank @Size(max = 200) String title,
		@Size(max = 5000) String description,
		LocalDate dueDate,
		UUID assigneeId) {
}
