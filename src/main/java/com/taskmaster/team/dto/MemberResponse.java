package com.taskmaster.team.dto;

import com.taskmaster.team.TeamMembership;
import com.taskmaster.team.TeamRole;
import java.util.UUID;

public record MemberResponse(UUID userId, String email, String displayName, TeamRole role) {

	public static MemberResponse from(TeamMembership membership) {
		return new MemberResponse(
				membership.getUser().getId(),
				membership.getUser().getEmail(),
				membership.getUser().getDisplayName(),
				membership.getRole());
	}
}
