package com.taskmaster.team;

import com.taskmaster.common.BadRequestException;
import com.taskmaster.common.ConflictException;
import com.taskmaster.common.ForbiddenException;
import com.taskmaster.common.ResourceNotFoundException;
import com.taskmaster.team.dto.CreateTeamRequest;
import com.taskmaster.team.dto.MemberResponse;
import com.taskmaster.team.dto.TeamResponse;
import com.taskmaster.team.dto.TeamSummaryResponse;
import com.taskmaster.user.User;
import com.taskmaster.user.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeamService {

	private final TeamRepository teams;
	private final TeamMembershipRepository memberships;
	private final UserRepository users;
	private final TeamAccess access;

	public TeamService(
			TeamRepository teams,
			TeamMembershipRepository memberships,
			UserRepository users,
			TeamAccess access) {
		this.teams = teams;
		this.memberships = memberships;
		this.users = users;
		this.access = access;
	}

	@Transactional
	public Team createPersonalTeam(User owner) {
		String name = owner.getDisplayName().trim() + "'s tasks";
		Team team = teams.save(new Team(name, null, true, owner));
		memberships.save(new TeamMembership(team, owner, TeamRole.OWNER));
		return team;
	}

	@Transactional
	public TeamResponse create(UUID userId, CreateTeamRequest request) {
		User owner = users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
		String description = blankToNull(request.description());
		Team team = teams.save(new Team(request.name().trim(), description, false, owner));
		memberships.save(new TeamMembership(team, owner, TeamRole.OWNER));
		return toResponse(team);
	}

	@Transactional(readOnly = true)
	public List<TeamSummaryResponse> list(UUID userId) {
		return memberships.findByUserId(userId).stream()
				.map(membership -> {
					Team team = membership.getTeam();
					return new TeamSummaryResponse(
							team.getId(),
							team.getName(),
							team.getDescription(),
							team.isPersonal(),
							team.getOwner().getId(),
							membership.getRole(),
							memberships.countByTeamId(team.getId()),
							team.getCreatedAt());
				})
				.toList();
	}

	@Transactional(readOnly = true)
	public TeamResponse get(UUID teamId, UUID userId) {
		Team team = access.requireMember(teamId, userId).getTeam();
		return toResponse(team);
	}

	@Transactional
	public MemberResponse invite(UUID teamId, UUID actorId, String email) {
		TeamMembership actor = access.requireMember(teamId, actorId);
		if (actor.getRole() != TeamRole.OWNER) {
			throw new ForbiddenException("Only the team owner can invite members");
		}
		User invitee = users.findByEmail(email.trim().toLowerCase())
				.orElseThrow(() -> new ResourceNotFoundException("No user registered with that email"));
		if (memberships.findWithDetails(teamId, invitee.getId()).isPresent()) {
			throw new ConflictException("User is already a member of this team");
		}
		TeamMembership membership = memberships.save(new TeamMembership(actor.getTeam(), invitee, TeamRole.MEMBER));
		return MemberResponse.from(membership);
	}

	@Transactional
	public void removeMember(UUID teamId, UUID actorId, UUID memberId) {
		TeamMembership actor = access.requireMember(teamId, actorId);
		if (actor.getTeam().getOwner().getId().equals(memberId)) {
			throw new BadRequestException("The team owner cannot be removed");
		}
		boolean removingSelf = actorId.equals(memberId);
		if (!removingSelf && actor.getRole() != TeamRole.OWNER) {
			throw new ForbiddenException("Only the team owner can remove other members");
		}
		TeamMembership target = memberships.findWithDetails(teamId, memberId)
				.orElseThrow(() -> new ResourceNotFoundException("Member not found"));
		memberships.delete(target);
	}

	private TeamResponse toResponse(Team team) {
		List<MemberResponse> members = memberships.findByTeamId(team.getId()).stream()
				.map(MemberResponse::from)
				.toList();
		return new TeamResponse(
				team.getId(),
				team.getName(),
				team.getDescription(),
				team.isPersonal(),
				team.getOwner().getId(),
				team.getCreatedAt(),
				members);
	}

	private static String blankToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
