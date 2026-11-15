package com.taskmaster.team.dto;

import com.taskmaster.team.TeamRole;
import java.time.Instant;
import java.util.UUID;

public record TeamSummaryResponse(
		UUID id,
		String name,
		String description,
		boolean personal,
		UUID ownerId,
		TeamRole myRole,
		long memberCount,
		Instant createdAt) {
}
