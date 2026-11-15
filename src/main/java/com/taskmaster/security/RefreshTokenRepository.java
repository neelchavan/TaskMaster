package com.taskmaster.security;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

	@Query("""
			select token from RefreshToken token
			join fetch token.user
			where token.tokenHash = :tokenHash
			""")
	Optional<RefreshToken> findByTokenHash(String tokenHash);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update RefreshToken token set token.revoked = true where token.user.id = :userId and token.revoked = false")
	void revokeAllActive(UUID userId);
}
