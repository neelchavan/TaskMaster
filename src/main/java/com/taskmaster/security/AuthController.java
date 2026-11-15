package com.taskmaster.security;

import com.taskmaster.user.dto.AuthResponse;
import com.taskmaster.user.dto.LoginRequest;
import com.taskmaster.user.dto.LogoutRequest;
import com.taskmaster.user.dto.RefreshRequest;
import com.taskmaster.user.dto.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	@SecurityRequirements
	@Operation(summary = "Create an account and a personal team")
	public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
		return authService.register(request);
	}

	@PostMapping("/login")
	@SecurityRequirements
	@Operation(summary = "Log in with email and password")
	public AuthResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@PostMapping("/refresh")
	@SecurityRequirements
	@Operation(summary = "Rotate a refresh token and issue a new access token")
	public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
		return authService.refresh(request.refreshToken());
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@SecurityRequirements
	@Operation(summary = "Revoke a refresh token")
	public void logout(@RequestBody(required = false) LogoutRequest request) {
		authService.logout(request == null ? null : request.refreshToken());
	}
}
