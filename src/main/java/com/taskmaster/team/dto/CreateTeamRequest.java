package com.taskmaster.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTeamRequest(@NotBlank @Size(max = 140) String name, @Size(max = 2000) String description) {
}
