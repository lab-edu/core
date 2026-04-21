package edu.lab.core.course.learning;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.lab.core.common.exception.BadRequestException;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AssignmentAiGradingClient {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AssignmentAiGradingClient.class);

    private static final String SYSTEM_PROMPT = """
        你是严谨的课程作业批改助教。你的任务是根据题目、题面、选项、标准答案、学生答案和分值信息进行评分。
        评分要求：
        1) 所有题目必须给出分数与反馈，分数不得为负数。
        2) 分项分数不能超过该题 maxScore。
        3) totalScore 建议与分项分数求和一致，且不能超过作业总分。
        4) 反馈语言用中文，简洁且可执行，避免空话。
        5) 只输出一个 JSON 对象，包含以下字段：
           - totalScore: 数字，总分
           - feedback: 字符串，总体反馈
           - itemGrades: 数组，每个元素包含 taskItemId（字符串）、score（数字）、feedback（字符串）
        不要重复原始上下文，不要输出任何额外文本。
        """;

    private final ObjectMapper objectMapper;
    private final AssignmentAiOpenAiProperties properties;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public AiGradeRawResult generateGradeDraft(AiGradeContext context) {
        String apiKey = properties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new BadRequestException("AI 批改未配置：缺少 LAB_AI_OPENAI_API_KEY");
        }

        String baseUrl = normalizeBaseUrl(properties.getBaseUrl());
        String endpoint = baseUrl + "/chat/completions";

        Map<String, Object> requestPayload = buildRequestPayload(context);
        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(requestPayload);
        } catch (IOException ex) {
            log.error("构建 AI 请求失败 - Request payload: {}", requestPayload, ex);
            throw new BadRequestException("构建 AI 请求失败");
        }

        log.debug("Calling AI grading endpoint: {}", endpoint);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.debug("AI grading response status: {}", response.statusCode());
        } catch (IOException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("AI grading request failed - Endpoint: {}, Request body: {}", endpoint, requestBody, ex);
            throw new BadRequestException("调用 AI 批改服务失败");
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.error("AI grading service returned error: HTTP {} - Request body: {}, Response body: {}", response.statusCode(), requestBody, response.body());
            throw new BadRequestException("AI 批改服务返回错误: HTTP " + response.statusCode());
        }

        String content = extractContent(response.body());
        if (content == null || content.isBlank()) {
            log.error("AI 批改结果为空 - Request body: {}, Full response: {}", requestBody, response.body());
            throw new BadRequestException("AI 批改结果为空");
        }

        try {
            return parseAiResponse(content);
        } catch (IOException ex) {
            log.error("AI 批改结果解析失败 - Request body: {}, Response content: {}", requestBody, content, ex);
            throw new BadRequestException("AI 批改结果解析失败");
        }
    }

    private String extractContent(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                return null;
            }

            JsonNode content = choices.get(0).path("message").path("content");
            if (content.isTextual()) {
                return content.asText();
            }
            if (content.isArray()) {
                StringBuilder builder = new StringBuilder();
                for (JsonNode node : content) {
                    String text = node.path("text").asText("");
                    if (!text.isBlank()) {
                        builder.append(text);
                    }
                }
                return builder.toString();
            }
            return null;
        } catch (IOException ex) {
            return null;
        }
    }

    private AiGradeRawResult parseAiResponse(String content) throws IOException {
        JsonNode root = objectMapper.readTree(content);

        // 尝试从根节点获取 totalScore 和 feedback
        BigDecimal totalScore = root.has("totalScore") && root.get("totalScore").isNumber()
                ? root.get("totalScore").decimalValue()
                : BigDecimal.ZERO;
        String feedback = root.has("feedback") && root.get("feedback").isTextual()
                ? root.get("feedback").asText()
                : "";

        List<AiGradeRawItem> itemGrades = new ArrayList<>();

        // 尝试从 taskItems 数组提取评分信息
        if (root.has("taskItems") && root.get("taskItems").isArray()) {
            for (JsonNode taskItem : root.get("taskItems")) {
                if (taskItem.has("taskItemId") && taskItem.has("score")) {
                    String taskItemId = taskItem.get("taskItemId").asText();
                    BigDecimal score = taskItem.get("score").decimalValue();
                    String itemFeedback = taskItem.has("feedback") && taskItem.get("feedback").isTextual()
                            ? taskItem.get("feedback").asText()
                            : "";
                    itemGrades.add(new AiGradeRawItem(taskItemId, score, itemFeedback));
                }
            }
        }

        // 如果 taskItems 中没有找到评分，尝试从 itemGrades 数组获取（向后兼容）
        if (itemGrades.isEmpty() && root.has("itemGrades") && root.get("itemGrades").isArray()) {
            for (JsonNode item : root.get("itemGrades")) {
                if (item.has("taskItemId") && item.has("score")) {
                    String taskItemId = item.get("taskItemId").asText();
                    BigDecimal score = item.get("score").decimalValue();
                    String itemFeedback = item.has("feedback") && item.get("feedback").isTextual()
                            ? item.get("feedback").asText()
                            : "";
                    itemGrades.add(new AiGradeRawItem(taskItemId, score, itemFeedback));
                }
            }
        }

        return new AiGradeRawResult(totalScore, feedback, itemGrades);
    }

    private Map<String, Object> buildRequestPayload(AiGradeContext context) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("required", List.of("totalScore", "feedback", "itemGrades"));

        Map<String, Object> itemSchema = new LinkedHashMap<>();
        itemSchema.put("type", "object");
        itemSchema.put("additionalProperties", false);
        itemSchema.put("required", List.of("taskItemId", "score", "feedback"));
        itemSchema.put("properties", Map.of(
                "taskItemId", Map.of("type", "string"),
                "score", Map.of("type", "number"),
                "feedback", Map.of("type", "string")
        ));

        schema.put("properties", Map.of(
                "totalScore", Map.of("type", "number"),
                "feedback", Map.of("type", "string"),
                "itemGrades", Map.of("type", "array", "items", itemSchema)
        ));

        String userPrompt = buildUserPrompt(context);
        return Map.of(
                "model", properties.getModel(),
                "temperature", 0,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "response_format", Map.of(
                        "type", "json_object"
                )
        );
    }

    private String buildUserPrompt(AiGradeContext context) {
        try {
            String jsonContext = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context);
            return """
                请基于以下作业上下文进行评分，并返回一个 JSON 对象，包含以下三个字段：
                1. totalScore: 数字，总分
                2. feedback: 字符串，总体反馈
                3. itemGrades: 数组，每个元素包含 taskItemId（字符串）、score（数字）、feedback（字符串）

                不要重复原始上下文，只返回评分结果。

                作业上下文：
                """ + jsonContext;
        } catch (IOException ex) {
            throw new BadRequestException("构建 AI 批改上下文失败");
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://api.openai.com/v1";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public record AiGradeContext(
            UUID courseId,
            UUID assignmentId,
            String assignmentTitle,
            String assignmentDescription,
            BigDecimal assignmentTotalScore,
            LocalDateTime startAt,
            LocalDateTime dueAt,
            StudentInfo student,
            List<TaskItemContext> taskItems
    ) {
    }

    public record StudentInfo(UUID id, String username, String displayName) {
    }

    public record TaskItemContext(
            UUID taskItemId,
            Integer sortOrder,
            String questionType,
            String question,
            List<String> options,
            String referenceAnswer,
            BigDecimal maxScore,
            String studentAnswer
    ) {
    }

    public record AiGradeRawResult(
            BigDecimal totalScore,
            String feedback,
            List<AiGradeRawItem> itemGrades
    ) {
    }

    public record AiGradeRawItem(
            String taskItemId,
            BigDecimal score,
            String feedback
    ) {
    }
}
