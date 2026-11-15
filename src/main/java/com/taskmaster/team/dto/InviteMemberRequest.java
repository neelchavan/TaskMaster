package com.taskmaster.team.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InviteMemberRequest(@NotBlank @Email @Size(max = 255) String email) {
}
