package com.taskmaster.team;

import com.taskmaster.common.ForbiddenException;
import com.taskmaster.common.ResourceNotFoundException;
import com.taskmaster.task.Task;
import com.taskmaster.task.TaskRepository;
import com.taskmaster.user.User;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TeamAccess {

	private final TeamRepository teams;
	private final TeamMembershipRepository memberships;
	private final TaskRepository tasks;

	public TeamAccess(TeamRepository teams, TeamMembershipRepository memberships, TaskRepository tasks) {
		this.teams = teams;
		this.memberships = memberships;
		this.tasks = tasks;
	}

	public TeamMembership requireMember(UUID teamId, UUID userId) {
		return requireMembership(teamId, userId, "You are not a member of this team");
	}

	public User requireAssignee(UUID teamId, UUID userId) {
		return requireMembership(teamId, userId, "Assignee is not a member of this team").getUser();
	}

	public List<UUID> teamIdsFor(UUID userId) {
		return memberships.findTeamIdsByUserId(userId);
	}

	public Task requireTask(UUID taskId, UUID userId) {
		Task task = tasks.findDetailedById(taskId).orElseThrow(() -> new ResourceNotFoundException("Task not found"));
		requireMember(task.getTeam().getId(), userId);
		return task;
	}

	private TeamMembership requireMembership(UUID teamId, UUID userId, String deniedMessage) {
		return memberships.findWithDetails(teamId, userId).orElseThrow(() -> {
			if (!teams.existsById(teamId)) {
				return new ResourceNotFoundException("Team not found");
			}
			return new ForbiddenException(deniedMessage);
		});
	}
}
