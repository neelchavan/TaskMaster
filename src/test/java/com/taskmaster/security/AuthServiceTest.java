package com.taskmaster.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.taskmaster.common.ConflictException;
import com.taskmaster.common.UnauthorizedException;
import com.taskmaster.config.TaskMasterProperties;
import com.taskmaster.team.TeamService;
import com.taskmaster.user.User;
import com.taskmaster.user.UserRepository;
import com.taskmaster.user.dto.AuthResponse;
import com.taskmaster.user.dto.LoginRequest;
import com.taskmaster.user.dto.RegisterRequest;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UserRepository users;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtService jwtService;

	@Mock
	private RefreshTokenRepository refreshTokens;

	@Mock
	private TeamService teams;

	private AuthService authService;

	@BeforeEach
	void setUp() {
		TaskMasterProperties properties = new TaskMasterProperties(
				new TaskMasterProperties.Jwt("test-secret-key-must-be-at-least-32-bytes", 15, 7),
				new TaskMasterProperties.Attachments("build/test-attachments", 1024));
		authService = new AuthService(users, passwordEncoder, jwtService, refreshTokens, teams, properties);
	}

	@Test
	void registerRejectsADuplicateEmail() {
		when(users.existsByEmail("ada@example.com")).thenReturn(true);

		assertThatThrownBy(() -> authService.register(new RegisterRequest("ada@example.com", "password1", "Ada")))
				.isInstanceOf(ConflictException.class);
		verify(teams, never()).createPersonalTeam(any());
	}

	@Test
	void loginRejectsAnUnknownEmailWithoutSkippingThePasswordCheck() {
		when(users.findByEmail("missing@example.com")).thenReturn(Optional.empty());
		when(passwordEncoder.matches(eq("password1"), anyString())).thenReturn(false);

		assertThatThrownBy(() -> authService.login(new LoginRequest("missing@example.com", "password1")))
				.isInstanceOf(UnauthorizedException.class);
		verify(passwordEncoder).matches(eq("password1"), anyString());
		verify(refreshTokens, never()).save(any());
	}

	@Test
	void loginIssuesTokensWhenThePasswordMatches() {
		User user = new User("ada@example.com", "hash", "Ada");
		user.setId(UUID.randomUUID());
		when(users.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("password1", "hash")).thenReturn(true);
		when(jwtService.createAccessToken(user)).thenReturn("access-token");

		AuthResponse response = authService.login(new LoginRequest("Ada@Example.com", "password1"));

		assertThat(response.accessToken()).isEqualTo("access-token");
		assertThat(response.refreshToken()).isNotBlank();
		assertThat(response.expiresIn()).isEqualTo(900);
		verify(refreshTokens).save(any(RefreshToken.class));
	}

	@Test
	void logoutRevokesThePresentedRefreshToken() {
		User user = new User("ada@example.com", "hash", "Ada");
		user.setId(UUID.randomUUID());
		RefreshToken token = new RefreshToken(user, "hash", Instant.now().plusSeconds(60));
		when(refreshTokens.findByTokenHash(anyString())).thenReturn(Optional.of(token));

		authService.logout("raw-token");

		assertThat(token.isRevoked()).isTrue();
	}

	@Test
	void refreshOfARevokedTokenRevokesTheUsersActiveTokens() {
		User user = new User("ada@example.com", "hash", "Ada");
		user.setId(UUID.randomUUID());
		RefreshToken token = new RefreshToken(user, "hash", Instant.now().plusSeconds(60));
		token.setRevoked(true);
		when(refreshTokens.findByTokenHash(anyString())).thenReturn(Optional.of(token));

		assertThatThrownBy(() -> authService.refresh("raw-token")).isInstanceOf(UnauthorizedException.class);
		verify(refreshTokens).revokeAllActive(user.getId());
	}
}
