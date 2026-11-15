package com.taskmaster.user.dto;

import com.taskmaster.user.User;
import java.util.UUID;

public record UserSummary(UUID id, String email, String displayName) {

	public static UserSummary from(User user) {
		if (user == null) {
			return null;
		}
		return new UserSummary(user.getId(), user.getEmail(), user.getDisplayName());
	}
}
