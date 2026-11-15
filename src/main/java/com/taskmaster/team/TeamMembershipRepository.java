package com.taskmaster.team;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TeamMembershipRepository extends JpaRepository<TeamMembership, UUID> {

	@Query("""
			select membership from TeamMembership membership
			join fetch membership.user
			join fetch membership.team team
			join fetch team.owner
			where membership.team.id = :teamId and membership.user.id = :userId
			""")
	Optional<TeamMembership> findWithDetails(UUID teamId, UUID userId);

	@Query("""
			select membership from TeamMembership membership
			join fetch membership.team team
			join fetch team.owner
			where membership.user.id = :userId
			order by membership.createdAt asc
			""")
	List<TeamMembership> findByUserId(UUID userId);

	@Query("""
			select membership from TeamMembership membership
			join fetch membership.user
			where membership.team.id = :teamId
			order by membership.createdAt asc
			""")
	List<TeamMembership> findByTeamId(UUID teamId);

	@Query("select membership.team.id from TeamMembership membership where membership.user.id = :userId")
	List<UUID> findTeamIdsByUserId(UUID userId);

	long countByTeamId(UUID teamId);
}
