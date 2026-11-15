package com.taskmaster.task;

import com.taskmaster.common.BadRequestException;
import java.util.UUID;
import org.springframework.data.domain.Sort;

public final class TaskQuery {

	private TaskQuery() {
	}

	public static UUID parseAssignee(String assignee, UUID currentUserId) {
		if (assignee == null || assignee.isBlank()) {
			return null;
		}
		if ("me".equalsIgnoreCase(assignee.trim())) {
			return currentUserId;
		}
		try {
			return UUID.fromString(assignee.trim());
		}
		catch (IllegalArgumentException exception) {
			throw new BadRequestException("assignee must be 'me' or a user id");
		}
	}

	public static Sort parseSort(String sort) {
		if (sort == null || sort.isBlank()) {
			return Sort.by(Sort.Direction.DESC, "createdAt");
		}
		String[] parts = sort.split(",");
		if (parts.length > 2) {
			throw new BadRequestException("sort must be a field or field,asc|desc");
		}
		String property = switch (parts[0].trim()) {
			case "dueDate", "createdAt", "title" -> parts[0].trim();
			default -> throw new BadRequestException("sort must be one of dueDate, createdAt, title");
		};
		Sort.Direction direction = Sort.Direction.ASC;
		if (parts.length == 2) {
			direction = switch (parts[1].trim().toLowerCase()) {
				case "asc" -> Sort.Direction.ASC;
				case "desc" -> Sort.Direction.DESC;
				default -> throw new BadRequestException("sort direction must be asc or desc");
			};
		}
		Sort.Order order = new Sort.Order(direction, property);
		if ("dueDate".equals(property)) {
			order = order.with(Sort.NullHandling.NULLS_LAST);
		}
		return Sort.by(order);
	}
}
