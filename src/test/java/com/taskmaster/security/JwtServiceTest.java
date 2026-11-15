package com.taskmaster.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.taskmaster.config.TaskMasterProperties;
import com.taskmaster.user.User;
import io.jsonwebtoken.JwtException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

	private final TaskMasterProperties properties = new TaskMasterProperties(
			new TaskMasterProperties.Jwt("test-secret-key-must-be-at-least-32-bytes", 15, 7),
			new TaskMasterProperties.Attachments("build/test-attachments", 1024));

	@Test
	void accessTokenRoundTripsTheUserId() {
		JwtService jwtService = new JwtService(properties);
		User user = new User("ada@example.com", "hash", "Ada");
		user.setId(UUID.randomUUID());

		String token = jwtService.createAccessToken(user);

		assertThat(jwtService.parseUserId(token)).isEqualTo(user.getId());
	}

	@Test
	void rejectsATamperedToken() {
		JwtService jwtService = new JwtService(properties);
		User user = new User("ada@example.com", "hash", "Ada");
		user.setId(UUID.randomUUID());
		String token = jwtService.createAccessToken(user);

		assertThatThrownBy(() -> jwtService.parseUserId(token + "x")).isInstanceOf(JwtException.class);
	}

	@Test
	void rejectsAShortSecret() {
		TaskMasterProperties insecure = new TaskMasterProperties(
				new TaskMasterProperties.Jwt("too-short", 15, 7),
				properties.attachments());

		assertThatThrownBy(() -> new JwtService(insecure)).isInstanceOf(IllegalStateException.class);
	}
}
