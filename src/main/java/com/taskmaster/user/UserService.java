package com.taskmaster.user;

import com.taskmaster.common.BadRequestException;
import com.taskmaster.common.ConflictException;
import com.taskmaster.common.ResourceNotFoundException;
import com.taskmaster.user.dto.UpdateProfileRequest;
import com.taskmaster.user.dto.UserResponse;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

	private final UserRepository users;

	public UserService(UserRepository users) {
		this.users = users;
	}

	@Transactional(readOnly = true)
	public UserResponse currentUser(UUID userId) {
		return UserResponse.from(find(userId));
	}

	@Transactional
	public UserResponse update(UUID userId, UpdateProfileRequest request) {
		User user = find(userId);
		if (request.displayName() != null) {
			String displayName = request.displayName().trim();
			if (displayName.isEmpty()) {
				throw new BadRequestException("Display name cannot be blank");
			}
			user.setDisplayName(displayName);
		}
		if (request.email() != null) {
			String email = request.email().trim().toLowerCase();
			if (email.isEmpty()) {
				throw new BadRequestException("Email cannot be blank");
			}
			users.findByEmail(email)
					.filter(existing -> !existing.getId().equals(userId))
					.ifPresent(existing -> {
						throw new ConflictException("An account with this email already exists");
					});
			user.setEmail(email);
		}
		return UserResponse.from(user);
	}

	private User find(UUID userId) {
		return users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
	}
}
