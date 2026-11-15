package com.taskmaster.team;

import com.taskmaster.common.CurrentUser;
import com.taskmaster.team.dto.CreateTeamRequest;
import com.taskmaster.team.dto.InviteMemberRequest;
import com.taskmaster.team.dto.MemberResponse;
import com.taskmaster.team.dto.TeamResponse;
import com.taskmaster.team.dto.TeamSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teams")
@Tag(name = "Teams")
public class TeamController {

	private final TeamService teamService;

	public TeamController(TeamService teamService) {
		this.teamService = teamService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create a team")
	public TeamResponse create(@Valid @RequestBody CreateTeamRequest request) {
		return teamService.create(CurrentUser.id(), request);
	}

	@GetMapping
	@Operation(summary = "List teams the current user belongs to")
	public List<TeamSummaryResponse> list() {
		return teamService.list(CurrentUser.id());
	}

	@GetMapping("/{teamId}")
	@Operation(summary = "View a team and its members")
	public TeamResponse get(@PathVariable UUID teamId) {
		return teamService.get(teamId, CurrentUser.id());
	}

	@PostMapping("/{teamId}/members")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Invite an existing user by email")
	public MemberResponse invite(@PathVariable UUID teamId, @Valid @RequestBody InviteMemberRequest request) {
		return teamService.invite(teamId, CurrentUser.id(), request.email());
	}

	@DeleteMapping("/{teamId}/members/{userId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Remove a member, or leave a team")
	public void removeMember(@PathVariable UUID teamId, @PathVariable UUID userId) {
		teamService.removeMember(teamId, CurrentUser.id(), userId);
	}
}
