package edu.lab.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TeachingFlowIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	void cleanUploads() throws Exception {
		java.nio.file.Path uploadRoot = java.nio.file.Path.of("./target/test-uploads").toAbsolutePath().normalize();
		if (java.nio.file.Files.exists(uploadRoot)) {
			try (var paths = java.nio.file.Files.walk(uploadRoot)) {
				paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
					if (!path.equals(uploadRoot)) {
						try {
							java.nio.file.Files.deleteIfExists(path);
						} catch (Exception ignored) {
						}
					}
				});
			}
		}
	}

	@Test
	void teacherStudentCourseExperimentAndSubmissionFlowWorks() throws Exception {
		registerUser("teacher1", "teacher1@example.com", "Password123!", "教师一号", "TEACHER");
		var teacherCookie = login("teacher1", "Password123!");

		MvcResult createCourseResult = mockMvc.perform(post("/api/v1/courses")
				.cookie(teacherCookie)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"title":"高等程序设计","description":"课程描述"}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.course.title").value("高等程序设计"))
			.andReturn();

		JsonNode courseNode = objectMapper.readTree(createCourseResult.getResponse().getContentAsString()).path("data").path("course");
		UUID courseId = UUID.fromString(courseNode.path("id").asText());
		String inviteCode = courseNode.path("inviteCode").asText();

		registerUser("student1", "student1@example.com", "Password123!", "学生一号", "STUDENT");
		var studentCookie = login("student1", "Password123!");

		mockMvc.perform(post("/api/v1/courses/join")
				.cookie(studentCookie)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"inviteCode":"%s"}
					""".formatted(inviteCode)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.course.id").value(courseId.toString()));

		MvcResult createExperimentResult = mockMvc.perform(post("/api/v1/courses/{courseId}/experiments", courseId)
				.cookie(teacherCookie)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"title":"实验 1","description":"实验说明","dueAt":"2099-12-31T23:59:59"}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.experiment.title").value("实验 1"))
			.andReturn();

		JsonNode experimentNode = objectMapper.readTree(createExperimentResult.getResponse().getContentAsString()).path("data").path("experiment");
		UUID experimentId = UUID.fromString(experimentNode.path("id").asText());

		mockMvc.perform(get("/api/v1/courses/{courseId}/experiments", courseId).cookie(studentCookie))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.items[0].id").value(experimentId.toString()));

		MockMultipartFile file = new MockMultipartFile(
			"file",
			"answer.txt",
			"text/plain",
			"hello lab-edu".getBytes(StandardCharsets.UTF_8)
		);

		mockMvc.perform(multipart("/api/v1/experiments/{experimentId}/submissions", experimentId)
				.file(file)
				.param("note", "第一次提交")
				.cookie(studentCookie))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.submission.latest").value(true));

		mockMvc.perform(get("/api/v1/experiments/{experimentId}/submissions", experimentId).cookie(teacherCookie))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.items[0].submittedBy.username").value("student1"));

		mockMvc.perform(post("/api/v1/auth/logout").cookie(teacherCookie))
			.andExpect(status().isOk())
			.andExpect(result -> {
				jakarta.servlet.http.Cookie cookie = result.getResponse().getCookie("lab_edu_token");
				assertThat(cookie).isNotNull();
				assertThat(cookie.getValue()).isEmpty();
				assertThat(cookie.getMaxAge()).isZero();
			});
	}

	private void registerUser(String username, String email, String password, String displayName, String role) throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"username":"%s","email":"%s","password":"%s","displayName":"%s","role":"%s"}
					""".formatted(username, email, password, displayName, role)))
			.andExpect(status().isCreated());
	}

	private jakarta.servlet.http.Cookie login(String identifier, String password) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"identifier":"%s","password":"%s"}
					""".formatted(identifier, password)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.user.username").value(identifier))
			.andReturn();
		jakarta.servlet.http.Cookie cookie = result.getResponse().getCookie("lab_edu_token");
		assertThat(cookie).isNotNull();
		return cookie;
	}
}