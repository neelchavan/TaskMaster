package com.taskmaster.team;

import com.taskmaster.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "teams")
public class Team {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, length = 140)
	private String name;

	@Column(length = 2000)
	private String description;

	@Column(nullable = false)
	private boolean personal;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "owner_id", nullable = false)
	private User owner;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Team() {
	}

	public Team(String name, String description, boolean personal, User owner) {
		this.name = name;
		this.description = description;
		this.personal = personal;
		this.owner = owner;
	}

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public boolean isPersonal() {
		return personal;
	}

	public User getOwner() {
		return owner;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
