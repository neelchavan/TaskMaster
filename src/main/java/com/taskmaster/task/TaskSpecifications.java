package com.taskmaster.task;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class TaskSpecifications {

	private TaskSpecifications() {
	}

	public static Specification<Task> matching(List<UUID> teamIds, TaskStatus status, UUID assigneeId, String query) {
		return (root, criteria, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(root.get("team").get("id").in(teamIds));
			if (status != null) {
				predicates.add(builder.equal(root.get("status"), status));
			}
			if (assigneeId != null) {
				predicates.add(builder.equal(root.get("assignee").get("id"), assigneeId));
			}
			if (query != null) {
				String pattern = "%" + escapeLike(query.toLowerCase()) + "%";
				predicates.add(builder.or(
						builder.like(builder.lower(root.get("title")), pattern, '\\'),
						builder.like(builder.lower(builder.coalesce(root.get("description"), "")), pattern, '\\')));
			}
			return builder.and(predicates.toArray(Predicate[]::new));
		};
	}

	private static String escapeLike(String value) {
		return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}
}
