package com.taskmaster.security;

import com.taskmaster.common.ConflictException;
import com.taskmaster.common.UnauthorizedException;
import com.taskmaster.config.TaskMasterProperties;
import com.taskmaster.team.TeamService;
import com.taskmaster.user.User;
import com.taskmaster.user.UserRepository;
import com.taskmaster.user.dto.AuthResponse;
import com.taskmaster.user.dto.LoginRequest;
import com.taskmaster.user.dto.RegisterRequest;
import com.taskmaster.user.dto.UserResponse;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);

	/**
	 * Compared when the email is unknown so login timing does not reveal whether the account exists.
	 */
	private static final String DUMMY_PASSWORD_HASH = "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG";

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final RefreshTokenRepository refreshTokens;
	private final TeamService teams;
	private final TaskMasterProperties properties;
	private final SecureRandom random = new SecureRandom();

	public AuthService(
			UserRepository users,
			PasswordEncoder passwordEncoder,
			JwtService jwtService,
			RefreshTokenRepository refreshTokens,
			TeamService teams,
			TaskMasterProperties properties) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.refreshTokens = refreshTokens;
		this.teams = teams;
		this.properties = properties;
	}

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		String email = normalizeEmail(request.email());
		if (users.existsByEmail(email)) {
			throw new ConflictException("An account with this email already exists");
		}
		User user = users.save(new User(email, passwordEncoder.encode(request.password()), request.displayName().trim()));
		teams.createPersonalTeam(user);
		log.info("Registered user {}", user.getId());
		return issueTokens(user);
	}

	@Transactional
	public AuthResponse login(LoginRequest request) {
		String email = normalizeEmail(request.email());
		User user = users.findByEmail(email).orElse(null);
		String hash = user == null ? DUMMY_PASSWORD_HASH : user.getPasswordHash();
		boolean passwordMatches = passwordEncoder.matches(request.password(), hash);
		if (user == null || !passwordMatches) {
			throw new UnauthorizedException("Invalid email or password");
		}
		return issueTokens(user);
	}

	@Transactional
	public AuthResponse refresh(String rawRefreshToken) {
		RefreshToken existing = refreshTokens.findByTokenHash(TokenHasher.sha256(rawRefreshToken))
				.orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
		if (existing.isRevoked()) {
			refreshTokens.revokeAllActive(existing.getUser().getId());
			throw new UnauthorizedException("Invalid refresh token");
		}
		if (existing.getExpiresAt().isBefore(Instant.now())) {
			existing.setRevoked(true);
			throw new UnauthorizedException("Invalid refresh token");
		}
		existing.setRevoked(true);
		return issueTokens(existing.getUser());
	}

	/**
	 * Refresh tokens are the revocable credential. Access tokens stay valid until they expire, so clients must drop them.
	 */
	@Transactional
	public void logout(String rawRefreshToken) {
		if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
			return;
		}
		refreshTokens.findByTokenHash(TokenHasher.sha256(rawRefreshToken)).ifPresent(token -> token.setRevoked(true));
	}

	private AuthResponse issueTokens(User user) {
		String refresh = newRefreshToken();
		Instant expiresAt = Instant.now().plus(properties.jwt().refreshTokenDays(), ChronoUnit.DAYS);
		refreshTokens.save(new RefreshToken(user, TokenHasher.sha256(refresh), expiresAt));
		return new AuthResponse(
				jwtService.createAccessToken(user),
				refresh,
				properties.jwt().accessTokenMinutes() * 60L,
				"Bearer",
				UserResponse.from(user));
	}

	private String newRefreshToken() {
		byte[] bytes = new byte[32];
		random.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private static String normalizeEmail(String email) {
		return email.trim().toLowerCase();
	}
}
