package com.taskmaster.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "taskmaster")
public record TaskMasterProperties(Jwt jwt, Attachments attachments) {

	public record Jwt(String secret, int accessTokenMinutes, int refreshTokenDays) {
	}

	public record Attachments(String directory, long maxBytes) {
	}
}
