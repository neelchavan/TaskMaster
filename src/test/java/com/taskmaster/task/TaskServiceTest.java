package com.taskmaster.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.taskmaster.attachment.AttachmentRepository;
import com.taskmaster.attachment.AttachmentStorage;
import com.taskmaster.common.ForbiddenException;
import com.taskmaster.task.dto.CreateTaskRequest;
import com.taskmaster.task.dto.TaskResponse;
import com.taskmaster.team.Team;
import com.taskmaster.team.TeamAccess;
import com.taskmaster.team.TeamMembership;
import com.taskmaster.team.TeamRole;
import com.taskmaster.user.User;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

	@Mock
	private TaskRepository tasks;

	@Mock
	private TeamAccess access;

	@Mock
	private AttachmentRepository attachments;

	@Mock
	private AttachmentStorage storage;

	@InjectMocks
	private TaskService taskService;

	@Test
	void listIsEmptyWhenTheUserHasNoTeams() {
		UUID userId = UUID.randomUUID();
		when(access.teamIdsFor(userId)).thenReturn(List.of());

		assertThat(taskService.list(userId, null, null, null, null, null)).isEmpty();
	}

	@Test
	void completeMarksTheTaskCompleted() {
		User creator = user("Ada");
		Task task = task(creator, creator);
		when(access.requireTask(task.getId(), creator.getId())).thenReturn(task);

		TaskResponse response = taskService.complete(task.getId(), creator.getId());

		assertThat(response.status()).isEqualTo(TaskStatus.COMPLETED);
		assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
	}

	@Test
	void deleteIsLimitedToTheCreatorOrTeamOwner() {
		User owner = user("Ada");
		User creator = user("Grace");
		User member = user("Lin");
		Task task = task(owner, creator);
		when(access.requireTask(task.getId(), member.getId())).thenReturn(task);

		assertThatThrownBy(() -> taskService.delete(task.getId(), member.getId())).isInstanceOf(ForbiddenException.class);
		verify(tasks, never()).delete(anyTask());
	}

	@Test
	void createRejectsAnAssigneeWhoIsNotOnTheTeam() {
		User creator = user("Ada");
		Team team = task(creator, creator).getTeam();
		UUID assigneeId = UUID.randomUUID();
		when(access.requireMember(team.getId(), creator.getId()))
				.thenReturn(new TeamMembership(team, creator, TeamRole.OWNER));
		when(access.requireAssignee(team.getId(), assigneeId))
				.thenThrow(new ForbiddenException("Assignee is not a member of this team"));

		CreateTaskRequest request = new CreateTaskRequest("Write spec", "Details", LocalDate.now(), assigneeId);

		assertThatThrownBy(() -> taskService.create(team.getId(), creator.getId(), request))
				.isInstanceOf(ForbiddenException.class);
	}

	private static User user(String name) {
		User user = new User(name.toLowerCase() + "@example.com", "hash", name);
		user.setId(UUID.randomUUID());
		return user;
	}

	private static Task task(User owner, User creator) {
		Team team = new Team(owner.getDisplayName() + "'s tasks", null, true, owner);
		team.setId(UUID.randomUUID());
		Task task = new Task(team, "Write spec", "API contract", LocalDate.of(2026, 12, 1), creator);
		task.setId(UUID.randomUUID());
		return task;
	}

	private static Task anyTask() {
		return org.mockito.ArgumentMatchers.any(Task.class);
	}
}
