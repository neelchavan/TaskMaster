package com.taskmaster.security;

import com.taskmaster.config.TaskMasterProperties;
import com.taskmaster.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

	private final SecretKey key;
	private final int accessTokenMinutes;

	public JwtService(TaskMasterProperties properties) {
		byte[] secret = properties.jwt().secret().getBytes(StandardCharsets.UTF_8);
		if (secret.length < 32) {
			throw new IllegalStateException("taskmaster.jwt.secret must be at least 32 bytes");
		}
		this.key = Keys.hmacShaKeyFor(secret);
		this.accessTokenMinutes = properties.jwt().accessTokenMinutes();
	}

	public String createAccessToken(User user) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(user.getId().toString())
				.claim("email", user.getEmail())
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plus(accessTokenMinutes, ChronoUnit.MINUTES)))
				.signWith(key)
				.compact();
	}

	public UUID parseUserId(String token) {
		Claims claims = Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload();
		return UUID.fromString(claims.getSubject());
	}

	public int accessTokenMinutes() {
		return accessTokenMinutes;
	}
}
