package com.taskmaster.team.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TeamResponse(
		UUID id,
		String name,
		String description,
		boolean personal,
		UUID ownerId,
		Instant createdAt,
		List<MemberResponse> members) {
}
