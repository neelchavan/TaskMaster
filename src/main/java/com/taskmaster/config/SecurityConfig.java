package com.taskmaster.config;

import com.taskmaster.security.JwtAuthenticationFilter;
import com.taskmaster.user.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login", "/api/auth/refresh",
								"/api/auth/logout")
						.permitAll()
						.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
						.permitAll()
						.requestMatchers("/error")
						.permitAll()
						.anyRequest()
						.authenticated())
				.exceptionHandling(handler -> handler
						.authenticationEntryPoint((request, response, exception) -> writeProblem(response,
								HttpStatus.UNAUTHORIZED, "Authentication required"))
						.accessDeniedHandler((request, response, exception) -> writeProblem(response, HttpStatus.FORBIDDEN,
								"Forbidden")))
				.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(12);
	}

	/**
	 * Present so Spring Boot does not generate a throwaway local user. Requests are authenticated by the JWT filter.
	 */
	@Bean
	UserDetailsService userDetailsService(UserRepository users) {
		return username -> users.findById(UUID.fromString(username))
				.map(user -> org.springframework.security.core.userdetails.User.withUsername(user.getId().toString())
						.password(user.getPasswordHash())
						.authorities("ROLE_USER")
						.build())
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));
	}

	private static void writeProblem(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		String safeDetail = detail.replace("\\", "\\\\").replace("\"", "\\\"");
		response.getWriter().write("""
				{"type":"about:blank","title":"%s","status":%d,"detail":"%s"}
				""".formatted(status.getReasonPhrase(), status.value(), safeDetail));
	}
}
