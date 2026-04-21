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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
		registerUser("teacher1", "teacher1@example.com", "Password123!", "教师一号");
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

		registerUser("student1", "student1@example.com", "Password123!", "学生一号");
		var studentCookie = login("student1", "Password123!");

		mockMvc.perform(post("/api/v1/courses/join")
				.cookie(studentCookie)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"inviteCode":"%s"}
					""".formatted(inviteCode)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.course.id").value(courseId.toString()));

		MvcResult createAnnouncementResult = mockMvc.perform(post("/api/v1/courses/{courseId}/announcements", courseId)
				.cookie(teacherCookie)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"title":"实验提醒","content":"请在下周前完成实验一预习。"}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.announcement.title").value("实验提醒"))
			.andReturn();

		String announcementId = objectMapper.readTree(createAnnouncementResult.getResponse().getContentAsString())
			.path("data").path("announcement").path("id").asText();

		mockMvc.perform(get("/api/v1/courses/{courseId}/announcements", courseId).cookie(studentCookie))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.items[0].id").value(announcementId))
			.andExpect(jsonPath("$.data.items[0].title").value("实验提醒"));

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

		MockMultipartFile resourceFile = new MockMultipartFile(
			"file",
			"lecture.pdf",
			"application/pdf",
			"resource content".getBytes(StandardCharsets.UTF_8)
		);

		MvcResult createResourceResult = mockMvc.perform(multipart("/api/v1/courses/{courseId}/resources", courseId)
				.file(resourceFile)
				.param("name", "第一章课件")
				.param("type", "FILE")
				.param("category", "课件")
				.cookie(teacherCookie))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.resource.name").value("第一章课件"))
			.andReturn();

		String resourceId = objectMapper.readTree(createResourceResult.getResponse().getContentAsString())
			.path("data").path("resource").path("id").asText();

		mockMvc.perform(get("/api/v1/courses/{courseId}/resources", courseId).cookie(studentCookie))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.items[0].id").value(resourceId));

		mockMvc.perform(get("/api/v1/resources/{resourceId}/file", resourceId).cookie(studentCookie))
			.andExpect(status().isOk());

		MvcResult teacherSubmissionsResult = mockMvc.perform(get("/api/v1/experiments/{experimentId}/submissions", experimentId).cookie(teacherCookie))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.items[0].submittedBy.username").value("student1"))
			.andReturn();

		String submissionId = objectMapper.readTree(teacherSubmissionsResult.getResponse().getContentAsString())
			.path("data").path("items").get(0).path("id").asText();

		mockMvc.perform(patch("/api/v1/submissions/{submissionId}/grade", submissionId)
				.cookie(teacherCookie)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"score":95,"feedback":"完成质量优秀"}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.score").value(95))
			.andExpect(jsonPath("$.data.feedback").value("完成质量优秀"));

		mockMvc.perform(get("/api/v1/experiments/{experimentId}/submissions", experimentId).cookie(teacherCookie))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.items[0].submittedBy.username").value("student1"))
			.andExpect(jsonPath("$.data.items[0].score").value(95));

		mockMvc.perform(get("/api/v1/experiments/{experimentId}/submissions", experimentId).cookie(studentCookie))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.items[0].score").value(95));

		mockMvc.perform(get("/api/v1/courses/{courseId}/grades/overview", courseId).cookie(teacherCookie))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.students[0].student.username").value("student1"))
			.andExpect(jsonPath("$.data.students[0].gradedCount").value(1))
			.andExpect(jsonPath("$.data.students[0].averageScore").value(95));

		mockMvc.perform(post("/api/v1/auth/logout").cookie(teacherCookie))
			.andExpect(status().isOk())
			.andExpect(result -> {
				jakarta.servlet.http.Cookie cookie = result.getResponse().getCookie("lab_edu_token");
				assertThat(cookie).isNotNull();
				assertThat(cookie.getValue()).isEmpty();
				assertThat(cookie.getMaxAge()).isZero();
			});
	}

	@Test
	void courseLearningStructureSubmissionAndGradingFlowWorks() throws Exception {
		registerUser("teacher2", "teacher2@example.com", "Password123!", "教师二号");
		var teacherCookie = login("teacher2", "Password123!");

		MvcResult createCourseResult = mockMvc.perform(post("/api/v1/courses")
				.cookie(teacherCookie)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"title":"课程学习","description":"学习结构"}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.course.title").value("课程学习"))
			.andReturn();

		JsonNode courseNode = objectMapper.readTree(createCourseResult.getResponse().getContentAsString()).path("data").path("course");
		UUID courseId = UUID.fromString(courseNode.path("id").asText());

		registerUser("student2", "student2@example.com", "Password123!", "学生二号");
		var studentCookie = login("student2", "Password123!");

		mockMvc.perform(post("/api/v1/courses/join")
				.cookie(studentCookie)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"inviteCode":"%s"}
					""".formatted(courseNode.path("inviteCode").asText())))
			.andExpect(status().isOk());

		MvcResult unitResult = mockMvc.perform(post("/api/v1/courses/{courseId}/learning/units", courseId)
				.cookie(teacherCookie)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"title":"第一单元","description":"课程导论","sortOrder":1,"published":true}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.title").value("第一单元"))
			.andReturn();

		UUID unitId = UUID.fromString(objectMapper.readTree(unitResult.getResponse().getContentAsString()).path("data").path("id").asText());

		MvcResult pointResult = mockMvc.perform(post("/api/v1/courses/{courseId}/learning/units/{unitId}/points", courseId, unitId)
				.cookie(teacherCookie)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"title":"知识点一","summary":"基础概念","estimatedMinutes":20,"sortOrder":1}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.title").value("知识点一"))
			.andReturn();

		UUID pointId = UUID.fromString(objectMapper.readTree(pointResult.getResponse().getContentAsString()).path("data").path("id").asText());

		MvcResult mediaTaskResult = mockMvc.perform(multipart("/api/v1/courses/{courseId}/learning/points/{pointId}/tasks", courseId, pointId)
				.cookie(teacherCookie)
				.param("title", "媒体学习任务")
				.param("taskType", "MEDIA")
				.param("materialType", "TEXT")
				.param("contentText", "请阅读这段课程导语。")
				.param("maxScore", "10"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.title").value("媒体学习任务"))
			.andReturn();

		String mediaTaskId = objectMapper.readTree(mediaTaskResult.getResponse().getContentAsString()).path("data").path("id").asText();

		MvcResult quizTaskResult = mockMvc.perform(multipart("/api/v1/courses/{courseId}/learning/points/{pointId}/tasks", courseId, pointId)
				.cookie(teacherCookie)
				.param("title", "随堂测试一")
				.param("taskType", "QUIZ")
				.param("questionType", "SINGLE_CHOICE")
				.param("optionsText", "A. 错误\nB. 正确\nC. 其他")
				.param("referenceAnswer", "B")
				.param("maxScore", "100")
				.param("required", "true"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.title").value("随堂测试一"))
			.andReturn();

		UUID quizTaskId = UUID.fromString(objectMapper.readTree(quizTaskResult.getResponse().getContentAsString()).path("data").path("id").asText());

		mockMvc.perform(get("/api/v1/courses/{courseId}/learning", courseId).cookie(studentCookie))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.units[0].points[0].tasks[1].title").value("随堂测试一"));

		MvcResult submissionResult = mockMvc.perform(multipart("/api/v1/courses/{courseId}/learning/tasks/{taskId}/submissions", courseId, quizTaskId)
				.cookie(studentCookie)
				.param("answerText", "B"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.latest").value(true))
			.andReturn();

		String submissionId = objectMapper.readTree(submissionResult.getResponse().getContentAsString()).path("data").path("id").asText();

		mockMvc.perform(patch("/api/v1/courses/{courseId}/learning/submissions/{submissionId}/grade", courseId, submissionId)
				.cookie(teacherCookie)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"score":92.5,"feedback":"知识点掌握良好"}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.score").value(92.5));

		mockMvc.perform(get("/api/v1/courses/{courseId}/learning/tasks/{taskId}/submissions", courseId, quizTaskId).cookie(teacherCookie))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.items[0].submittedBy.username").value("student2"))
			.andExpect(jsonPath("$.data.items[0].score").value(92.5));

		mockMvc.perform(get("/api/v1/courses/{courseId}/learning/tasks/{taskId}/submissions/latest", courseId, quizTaskId).cookie(studentCookie))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.score").value(92.5));

		mockMvc.perform(get("/api/v1/courses/{courseId}/learning/overview", courseId).cookie(teacherCookie))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.students[0].student.username").value("student2"))
			.andExpect(jsonPath("$.data.students[0].gradedCount").value(1))
			.andExpect(jsonPath("$.data.students[0].averageScore").value(92.5));

		mockMvc.perform(get("/api/v1/courses/{courseId}/learning/tasks/{taskId}/file", courseId, mediaTaskId).cookie(studentCookie))
			.andExpect(status().isBadRequest());
	}

	private void registerUser(String username, String email, String password, String displayName) throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"username":"%s","email":"%s","password":"%s","displayName":"%s"}
					""".formatted(username, email, password, displayName)))
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