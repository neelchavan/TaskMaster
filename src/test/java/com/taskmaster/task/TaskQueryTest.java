package com.taskmaster.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.taskmaster.common.BadRequestException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class TaskQueryTest {

	private final UUID currentUser = UUID.randomUUID();

	@Test
	void assigneeMeResolvesToTheCurrentUser() {
		assertThat(TaskQuery.parseAssignee("me", currentUser)).isEqualTo(currentUser);
		assertThat(TaskQuery.parseAssignee(" ME ", currentUser)).isEqualTo(currentUser);
		assertThat(TaskQuery.parseAssignee("  ", currentUser)).isNull();
	}

	@Test
	void assigneeAcceptsAUserIdAndRejectsOtherText() {
		UUID assignee = UUID.randomUUID();
		assertThat(TaskQuery.parseAssignee(assignee.toString(), currentUser)).isEqualTo(assignee);
		assertThatThrownBy(() -> TaskQuery.parseAssignee("someone", currentUser)).isInstanceOf(BadRequestException.class);
	}

	@Test
	void sortAcceptsTheSupportedFields() {
		Sort dueDate = TaskQuery.parseSort("dueDate,desc");
		assertThat(dueDate.getOrderFor("dueDate").getDirection()).isEqualTo(Sort.Direction.DESC);
		assertThat(dueDate.getOrderFor("dueDate").getNullHandling()).isEqualTo(Sort.NullHandling.NULLS_LAST);

		assertThat(TaskQuery.parseSort("title").getOrderFor("title").getDirection()).isEqualTo(Sort.Direction.ASC);
		assertThat(TaskQuery.parseSort(null).getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
	}

	@Test
	void sortRejectsUnknownFieldsAndDirections() {
		assertThatThrownBy(() -> TaskQuery.parseSort("priority")).isInstanceOf(BadRequestException.class);
		assertThatThrownBy(() -> TaskQuery.parseSort("title,sideways")).isInstanceOf(BadRequestException.class);
		assertThatThrownBy(() -> TaskQuery.parseSort("title,asc,extra")).isInstanceOf(BadRequestException.class);
	}
}
