package com.taskmaster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TaskMasterApiIT {

	@Container
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

	@DynamicPropertySource
	static void datasource(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
	}

	@Autowired
	private MockMvc mockMvc;

	@Test
	void registerLoginProfileAndLogout() throws Exception {
		Session ada = register("Ada");

		mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, ada.bearer()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.displayName").value("Ada"))
				.andExpect(jsonPath("$.email").value(ada.email()));

		mockMvc.perform(patch("/api/users/me")
						.header(HttpHeaders.AUTHORIZATION, ada.bearer())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"displayName\":\"Ada Lovelace\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.displayName").value("Ada Lovelace"));

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"wrong-password"}
								""".formatted(ada.email())))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

		MvcResult refreshed = mockMvc.perform(post("/api/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + ada.refreshToken() + "\"}"))
				.andExpect(status().isOk())
				.andReturn();
		String rotated = JsonPath.read(refreshed.getResponse().getContentAsString(), "$.refreshToken");
		assertThat(rotated).isNotEqualTo(ada.refreshToken());

		mockMvc.perform(post("/api/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + ada.refreshToken() + "\"}"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/auth/logout")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + rotated + "\"}"))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + rotated + "\"}"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
	}

	@Test
	void tasksCanBeFilteredSearchedAssignedAndCompleted() throws Exception {
		Session ada = register("Ada");
		Session grace = register("Grace");
		Session lin = register("Lin");
		String personalTeamId = teamId(ada);

		String taskId = createTask(ada, personalTeamId, "Write the spec", "API contract", ada.userId().toString());

		mockMvc.perform(get("/api/tasks")
						.header(HttpHeaders.AUTHORIZATION, ada.bearer())
						.param("assignee", "me")
						.param("status", "OPEN")
						.param("q", "SPEC")
						.param("sort", "dueDate,asc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].title").value("Write the spec"))
				.andExpect(jsonPath("$[0].status").value("OPEN"));

		mockMvc.perform(get("/api/tasks")
						.header(HttpHeaders.AUTHORIZATION, ada.bearer())
						.param("q", "missing-term"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));

		mockMvc.perform(get("/api/tasks")
						.header(HttpHeaders.AUTHORIZATION, ada.bearer())
						.param("sort", "priority"))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post("/api/tasks/" + taskId + "/complete").header(HttpHeaders.AUTHORIZATION, ada.bearer()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("COMPLETED"));

		mockMvc.perform(get("/api/tasks")
						.header(HttpHeaders.AUTHORIZATION, ada.bearer())
						.param("status", "OPEN"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));

		mockMvc.perform(get("/api/tasks")
						.header(HttpHeaders.AUTHORIZATION, ada.bearer())
						.param("status", "COMPLETED"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(taskId));

		MvcResult createdTeam = mockMvc.perform(post("/api/teams")
						.header(HttpHeaders.AUTHORIZATION, ada.bearer())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Platform\",\"description\":\"Shared work\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.personal").value(false))
				.andReturn();
		String teamId = JsonPath.read(createdTeam.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(post("/api/teams/" + teamId + "/members")
						.header(HttpHeaders.AUTHORIZATION, ada.bearer())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"nobody@example.com\"}"))
				.andExpect(status().isNotFound());

		mockMvc.perform(post("/api/teams/" + teamId + "/members")
						.header(HttpHeaders.AUTHORIZATION, ada.bearer())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + grace.email() + "\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.role").value("MEMBER"));

		String assignedId = createTask(ada, teamId, "Review the patch", "Check the query", grace.userId().toString());

		mockMvc.perform(get("/api/tasks")
						.header(HttpHeaders.AUTHORIZATION, grace.bearer())
						.param("assignee", "me")
						.param("teamId", teamId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(assignedId));

		mockMvc.perform(get("/api/tasks/" + assignedId).header(HttpHeaders.AUTHORIZATION, lin.bearer()))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/tasks").header(HttpHeaders.AUTHORIZATION, lin.bearer()).param("teamId", teamId))
				.andExpect(status().isForbidden());

		mockMvc.perform(delete("/api/tasks/" + assignedId).header(HttpHeaders.AUTHORIZATION, grace.bearer()))
				.andExpect(status().isForbidden());

		mockMvc.perform(delete("/api/tasks/" + assignedId).header(HttpHeaders.AUTHORIZATION, ada.bearer()))
				.andExpect(status().isNoContent());
	}

	@Test
	void membersCanCommentAndAttachFiles() throws Exception {
		Session ada = register("Ada");
		Session grace = register("Grace");
		Session lin = register("Lin");
		String teamId = createSharedTeam(ada, grace);
		String taskId = createTask(ada, teamId, "Ship the notes", "Attach the draft", null);

		MvcResult comment = mockMvc.perform(post("/api/tasks/" + taskId + "/comments")
						.header(HttpHeaders.AUTHORIZATION, grace.bearer())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"body\":\"Looks good\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		String commentId = JsonPath.read(comment.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(get("/api/tasks/" + taskId + "/comments").header(HttpHeaders.AUTHORIZATION, ada.bearer()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].body").value("Looks good"));

		mockMvc.perform(get("/api/tasks/" + taskId + "/comments").header(HttpHeaders.AUTHORIZATION, lin.bearer()))
				.andExpect(status().isForbidden());

		mockMvc.perform(delete("/api/tasks/" + taskId + "/comments/" + commentId)
						.header(HttpHeaders.AUTHORIZATION, grace.bearer()))
				.andExpect(status().isNoContent());

		MvcResult ownerComment = mockMvc.perform(post("/api/tasks/" + taskId + "/comments")
						.header(HttpHeaders.AUTHORIZATION, ada.bearer())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"body\":\"Ship it\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		String ownerCommentId = JsonPath.read(ownerComment.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(delete("/api/tasks/" + taskId + "/comments/" + ownerCommentId)
						.header(HttpHeaders.AUTHORIZATION, grace.bearer()))
				.andExpect(status().isForbidden());

		mockMvc.perform(delete("/api/tasks/" + taskId + "/comments/" + ownerCommentId)
						.header(HttpHeaders.AUTHORIZATION, ada.bearer()))
				.andExpect(status().isNoContent());

		MockMultipartFile notes = new MockMultipartFile(
				"file",
				"notes.txt",
				"text/plain",
				"hello".getBytes(StandardCharsets.UTF_8));
		MvcResult uploaded = mockMvc.perform(multipart("/api/tasks/" + taskId + "/attachments")
						.file(notes)
						.header(HttpHeaders.AUTHORIZATION, grace.bearer()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.filename").value("notes.txt"))
				.andReturn();
		String attachmentId = JsonPath.read(uploaded.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(get("/api/tasks/" + taskId + "/attachments/" + attachmentId)
						.header(HttpHeaders.AUTHORIZATION, ada.bearer()))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("notes.txt")))
				.andExpect(content().bytes("hello".getBytes(StandardCharsets.UTF_8)));

		mockMvc.perform(get("/api/tasks/" + taskId + "/attachments/" + attachmentId)
						.header(HttpHeaders.AUTHORIZATION, lin.bearer()))
				.andExpect(status().isForbidden());

		MockMultipartFile rejected = new MockMultipartFile(
				"file",
				"run.exe",
				"application/octet-stream",
				"MZ".getBytes(StandardCharsets.UTF_8));
		mockMvc.perform(multipart("/api/tasks/" + taskId + "/attachments")
						.file(rejected)
						.header(HttpHeaders.AUTHORIZATION, ada.bearer()))
				.andExpect(status().isBadRequest());

		mockMvc.perform(delete("/api/tasks/" + taskId + "/attachments/" + attachmentId)
						.header(HttpHeaders.AUTHORIZATION, ada.bearer()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/tasks/" + taskId + "/attachments").header(HttpHeaders.AUTHORIZATION, ada.bearer()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void validationErrorsUseProblemDetails() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"not-an-email\",\"password\":\"short\",\"displayName\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.errors.password").exists())
				.andExpect(jsonPath("$.errors.email").exists());
	}

	private Session register(String displayName) throws Exception {
		String email = displayName.toLowerCase() + "-" + UUID.randomUUID() + "@example.com";
		MvcResult result = mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"password1","displayName":"%s"}
								""".formatted(email, displayName)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andReturn();
		String body = result.getResponse().getContentAsString();
		return new Session(
				UUID.fromString(JsonPath.read(body, "$.user.id")),
				email,
				JsonPath.read(body, "$.accessToken"),
				JsonPath.read(body, "$.refreshToken"));
	}

	private String teamId(Session session) throws Exception {
		MvcResult result = mockMvc.perform(get("/api/teams").header(HttpHeaders.AUTHORIZATION, session.bearer()))
				.andExpect(status().isOk())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$[0].id");
	}

	private String createSharedTeam(Session owner, Session member) throws Exception {
		MvcResult created = mockMvc.perform(post("/api/teams")
						.header(HttpHeaders.AUTHORIZATION, owner.bearer())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Platform\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		String teamId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
		mockMvc.perform(post("/api/teams/" + teamId + "/members")
						.header(HttpHeaders.AUTHORIZATION, owner.bearer())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + member.email() + "\"}"))
				.andExpect(status().isCreated());
		return teamId;
	}

	private String createTask(Session session, String teamId, String title, String description, String assigneeId) throws Exception {
		String assigneeJson = assigneeId == null ? "" : ",\"assigneeId\":\"" + assigneeId + "\"";
		MvcResult result = mockMvc.perform(post("/api/teams/" + teamId + "/tasks")
						.header(HttpHeaders.AUTHORIZATION, session.bearer())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"%s","description":"%s","dueDate":"2026-12-01"%s}
								""".formatted(title, description, assigneeJson)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private record Session(UUID userId, String email, String accessToken, String refreshToken) {

		private String bearer() {
			return "Bearer " + accessToken;
		}
	}
}
