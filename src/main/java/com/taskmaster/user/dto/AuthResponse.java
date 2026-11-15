package com.taskmaster.user.dto;

public record AuthResponse(
		String accessToken,
		String refreshToken,
		long expiresIn,
		String tokenType,
		UserResponse user) {
}
