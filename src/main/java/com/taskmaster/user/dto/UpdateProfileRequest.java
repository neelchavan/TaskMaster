package com.taskmaster.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(@Size(max = 100) String displayName, @Email @Size(max = 255) String email) {
}
