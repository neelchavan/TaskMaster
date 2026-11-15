package com.taskmaster.user;

import com.taskmaster.common.CurrentUser;
import com.taskmaster.user.dto.UpdateProfileRequest;
import com.taskmaster.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/me")
	@Operation(summary = "View the signed-in user's profile")
	public UserResponse me() {
		return userService.currentUser(CurrentUser.id());
	}

	@PatchMapping("/me")
	@Operation(summary = "Update the signed-in user's display name or email")
	public UserResponse update(@Valid @RequestBody UpdateProfileRequest request) {
		return userService.update(CurrentUser.id(), request);
	}
}
