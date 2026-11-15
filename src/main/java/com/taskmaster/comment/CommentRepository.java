package com.taskmaster.comment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

	@Query("""
			select comment from Comment comment
			join fetch comment.author
			where comment.task.id = :taskId
			order by comment.createdAt asc
			""")
	List<Comment> findByTaskId(UUID taskId);

	@Query("""
			select comment from Comment comment
			join fetch comment.author
			join fetch comment.task task
			join fetch task.team team
			join fetch team.owner
			where comment.id = :commentId and comment.task.id = :taskId
			""")
	Optional<Comment> findDetailed(UUID taskId, UUID commentId);
}
