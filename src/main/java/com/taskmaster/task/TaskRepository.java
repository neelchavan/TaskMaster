package com.taskmaster.task;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {

	@Query("""
			select task from Task task
			join fetch task.team team
			join fetch team.owner
			join fetch task.creator
			left join fetch task.assignee
			where task.id = :id
			""")
	Optional<Task> findDetailedById(UUID id);
}
