package edu.lab.core.course.learning;

import edu.lab.core.common.exception.BadRequestException;
import edu.lab.core.common.exception.ForbiddenException;
import edu.lab.core.common.exception.NotFoundException;
import edu.lab.core.course.Course;
import edu.lab.core.course.CourseRepository;
import edu.lab.core.course.CourseMemberRepository;
import edu.lab.core.course.CourseMemberRole;
import edu.lab.core.course.learning.dto.AssignmentCreateRequest;
import edu.lab.core.course.learning.dto.AssignmentResponse;
import edu.lab.core.course.learning.dto.AssignmentSubmissionRequest;
import edu.lab.core.course.learning.dto.AssignmentSubmissionResponse;
import edu.lab.core.course.learning.dto.AssignmentUpdateRequest;
import edu.lab.core.course.learning.dto.AssignmentGradeRequest;
import edu.lab.core.notification.HomeworkReminderService;
import edu.lab.core.security.AuthenticatedUser;
import edu.lab.core.storage.FileStorageService;
import edu.lab.core.user.AppUser;
import edu.lab.core.user.UserRepository;
import edu.lab.core.user.dto.UserSummaryResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentTaskItemRepository assignmentTaskItemRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CourseMemberRepository courseMemberRepository;
    private final HomeworkReminderService homeworkReminderService;
    private final ObjectMapper objectMapper;
    private final FileStorageService fileStorageService;

    @Transactional
    public AssignmentResponse createAssignment(UUID courseId, AssignmentCreateRequest request, AuthenticatedUser authUser) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found"));

        AppUser user = userRepository.findById(authUser.id())
                .orElseThrow(() -> new NotFoundException("User not found"));

        // 验证用户是否是课程教师或管理员
        if (!courseMemberRepository.existsByCourseAndUserAndMemberRoleIn(course, user,
                List.of(CourseMemberRole.OWNER, CourseMemberRole.TEACHER))) {
            throw new ForbiddenException("Only course teachers can create assignments");
        }

        Assignment assignment = new Assignment();
        assignment.setCourse(course);
        assignment.setCreatedBy(user);
        assignment.setTitle(request.title());
        assignment.setDescription(request.description());

        // 计算总分：如果autoCalculateTotal为true，则汇总题目分值；否则使用提供的总分
        if (request.autoCalculateTotal()) {
            BigDecimal total = request.taskItems().stream()
                    .map(AssignmentCreateRequest.AssignmentTaskItemRequest::maxScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assignment.setTotalScore(total);
        } else {
            assignment.setTotalScore(request.totalScore());
        }

        assignment.setStartAt(request.startAt());
        assignment.setDueAt(request.dueAt());
        assignment.setRequired(request.required());
        assignment.setSortOrder(request.sortOrder());
        assignment.setPublished(request.published());
        assignment.setNotifyOnStart(request.notifyOnStart());
        assignment.setNotifyBeforeDue24h(request.notifyBeforeDue24h());
        assignment.setNotifyOnDue(request.notifyOnDue());
        assignment.setAutoCalculateTotal(request.autoCalculateTotal());

        Assignment savedAssignment = assignmentRepository.save(assignment);

        // 创建题目项
        List<AssignmentResponse.AssignmentTaskItemResponse> taskItemResponses = new ArrayList<>();
        int itemSortOrder = 0;
        for (AssignmentCreateRequest.AssignmentTaskItemRequest itemRequest : request.taskItems()) {
            AssignmentTaskItem taskItem = new AssignmentTaskItem();
            taskItem.setAssignment(savedAssignment);
            taskItem.setQuestion(itemRequest.question());
            taskItem.setQuestionType(itemRequest.questionType());
            taskItem.setOptionsJson(convertOptionsToJson(itemRequest.options()));
            taskItem.setReferenceAnswer(itemRequest.referenceAnswer());
            taskItem.setMaxScore(itemRequest.maxScore());
            taskItem.setSortOrder(itemSortOrder++);

            AssignmentTaskItem savedItem = assignmentTaskItemRepository.save(taskItem);
            taskItemResponses.add(convertToTaskItemResponse(savedItem));
        }

        // 设置提醒
        if (request.published()) {
            // homeworkReminderService.scheduleAssignmentReminders(savedAssignment); // TODO: implement
        }

        return convertToAssignmentResponse(savedAssignment, taskItemResponses);
    }

    @Transactional
    public AssignmentResponse updateAssignment(UUID courseId, UUID assignmentId, AssignmentUpdateRequest request, AuthenticatedUser authUser) {
        Assignment assignment = assignmentRepository.findWithCourseById(assignmentId)
                .orElseThrow(() -> new NotFoundException("Assignment not found"));

        if (!assignment.getCourse().getId().equals(courseId)) {
            throw new BadRequestException("Assignment does not belong to this course");
        }

        AppUser user = userRepository.findById(authUser.id())
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!assignment.getCreatedBy().getId().equals(user.getId()) &&
            !courseMemberRepository.existsByCourseAndUserAndMemberRoleIn(assignment.getCourse(), user,
                    List.of(CourseMemberRole.OWNER, CourseMemberRole.TEACHER))) {
            throw new ForbiddenException("Only assignment creator or course teachers can update");
        }

        assignment.setTitle(request.title());
        assignment.setDescription(request.description());

        if (request.autoCalculateTotal()) {
            // 重新计算总分
            List<AssignmentTaskItem> existingItems = assignmentTaskItemRepository.findByAssignmentIdOrderBySortOrderAscCreatedAtAsc(assignmentId);
            BigDecimal total = existingItems.stream()
                    .map(AssignmentTaskItem::getMaxScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assignment.setTotalScore(total);
        } else {
            assignment.setTotalScore(request.totalScore());
        }

        assignment.setStartAt(request.startAt());
        assignment.setDueAt(request.dueAt());
        assignment.setRequired(request.required());
        assignment.setSortOrder(request.sortOrder());
        assignment.setPublished(request.published());
        assignment.setNotifyOnStart(request.notifyOnStart());
        assignment.setNotifyBeforeDue24h(request.notifyBeforeDue24h());
        assignment.setNotifyOnDue(request.notifyOnDue());
        assignment.setAutoCalculateTotal(request.autoCalculateTotal());

        // 更新题目项（简化：删除所有现有项，创建新项）
        assignmentTaskItemRepository.deleteByAssignmentId(assignmentId);
        List<AssignmentResponse.AssignmentTaskItemResponse> taskItemResponses = new ArrayList<>();
        int itemSortOrder = 0;
        for (AssignmentUpdateRequest.AssignmentTaskItemUpdateRequest itemRequest : request.taskItems()) {
            AssignmentTaskItem taskItem = new AssignmentTaskItem();
            taskItem.setAssignment(assignment);
            taskItem.setQuestion(itemRequest.question());
            taskItem.setQuestionType(itemRequest.questionType());
            taskItem.setOptionsJson(convertOptionsToJson(itemRequest.options()));
            taskItem.setReferenceAnswer(itemRequest.referenceAnswer());
            taskItem.setMaxScore(itemRequest.maxScore());
            taskItem.setSortOrder(itemSortOrder++);

            AssignmentTaskItem savedItem = assignmentTaskItemRepository.save(taskItem);
            taskItemResponses.add(convertToTaskItemResponse(savedItem));
        }

        Assignment updatedAssignment = assignmentRepository.save(assignment);

        // 更新提醒
        // homeworkReminderService.updateAssignmentReminders(updatedAssignment); // TODO: implement

        return convertToAssignmentResponse(updatedAssignment, taskItemResponses);
    }

    @Transactional
    public void deleteAssignment(UUID courseId, UUID assignmentId, AuthenticatedUser authUser) {
        Assignment assignment = assignmentRepository.findWithCourseById(assignmentId)
                .orElseThrow(() -> new NotFoundException("Assignment not found"));

        if (!assignment.getCourse().getId().equals(courseId)) {
            throw new BadRequestException("Assignment does not belong to this course");
        }

        AppUser user = userRepository.findById(authUser.id())
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!assignment.getCreatedBy().getId().equals(user.getId()) &&
            !courseMemberRepository.existsByCourseAndUserAndMemberRoleIn(assignment.getCourse(), user,
                    List.of(CourseMemberRole.OWNER))) {
            throw new ForbiddenException("Only assignment creator or course owner can delete");
        }

        // 删除提醒
        // homeworkReminderService.cancelAssignmentReminders(assignmentId); // TODO: implement

        assignmentRepository.delete(assignment);
    }

    @Transactional(readOnly = true)
    public AssignmentResponse getAssignment(UUID courseId, UUID assignmentId, AuthenticatedUser authUser) {
        Assignment assignment = assignmentRepository.findWithCourseById(assignmentId)
                .orElseThrow(() -> new NotFoundException("Assignment not found"));

        if (!assignment.getCourse().getId().equals(courseId)) {
            throw new BadRequestException("Assignment does not belong to this course");
        }

        // 检查权限：学生只能查看已发布的作业
        AppUser user = userRepository.findById(authUser.id())
                .orElseThrow(() -> new NotFoundException("User not found"));

        boolean isTeacher = courseMemberRepository.existsByCourseAndUserAndMemberRoleIn(assignment.getCourse(), user,
                List.of(CourseMemberRole.OWNER, CourseMemberRole.TEACHER));

        if (!assignment.isPublished() && !isTeacher) {
            throw new ForbiddenException("Assignment is not published");
        }

        List<AssignmentTaskItem> taskItems = assignmentTaskItemRepository.findByAssignmentIdOrderBySortOrderAscCreatedAtAsc(assignmentId);
        List<AssignmentResponse.AssignmentTaskItemResponse> taskItemResponses = taskItems.stream()
                .map(this::convertToTaskItemResponse)
                .toList();

        return convertToAssignmentResponse(assignment, taskItemResponses);
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> listAssignments(UUID courseId, AuthenticatedUser authUser) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found"));

        AppUser user = userRepository.findById(authUser.id())
                .orElseThrow(() -> new NotFoundException("User not found"));

        boolean isTeacher = courseMemberRepository.existsByCourseAndUserAndMemberRoleIn(course, user,
                List.of(CourseMemberRole.OWNER, CourseMemberRole.TEACHER));

        List<Assignment> assignments;
        if (isTeacher) {
            assignments = assignmentRepository.findByCourseIdOrderBySortOrderAscCreatedAtAsc(courseId);
        } else {
            assignments = assignmentRepository.findPublishedByCourseId(courseId);
        }

        return assignments.stream()
                .map(assignment -> {
                    List<AssignmentTaskItem> taskItems = assignmentTaskItemRepository.findByAssignmentIdOrderBySortOrderAscCreatedAtAsc(assignment.getId());
                    List<AssignmentResponse.AssignmentTaskItemResponse> taskItemResponses = taskItems.stream()
                            .map(this::convertToTaskItemResponse)
                            .toList();
                    return convertToAssignmentResponse(assignment, taskItemResponses);
                })
                .toList();
    }

    @Transactional
    public AssignmentSubmissionResponse submitAssignment(UUID courseId, UUID assignmentId,
            AssignmentSubmissionRequest request, AuthenticatedUser authUser) {
        Assignment assignment = assignmentRepository.findWithCourseById(assignmentId)
                .orElseThrow(() -> new NotFoundException("Assignment not found"));

        if (!assignment.getCourse().getId().equals(courseId)) {
            throw new BadRequestException("Assignment does not belong to this course");
        }

        if (!assignment.isPublished()) {
            throw new BadRequestException("Assignment is not published");
        }

        if (assignment.getDueAt() != null && assignment.getDueAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Assignment is past due");
        }

        AppUser user = userRepository.findById(authUser.id())
                .orElseThrow(() -> new NotFoundException("User not found"));

        // 标记之前的提交为非最新
        List<AssignmentSubmission> previousSubmissions = assignmentSubmissionRepository
                .findByAssignmentIdAndUserIdOrderBySubmittedAtDesc(assignmentId, user.getId());
        for (AssignmentSubmission submission : previousSubmissions) {
            submission.setLatest(false);
            assignmentSubmissionRepository.save(submission);
        }

        // 创建新提交
        AssignmentSubmission submission = new AssignmentSubmission();
        submission.setAssignment(assignment);
        submission.setSubmittedBy(user);
        submission.setAnswersJson(convertAnswersToJson(request.answers()));
        submission.setLatest(true);
        submission.setSubmittedAt(LocalDateTime.now());

        AssignmentSubmission savedSubmission = assignmentSubmissionRepository.save(submission);

        return convertToSubmissionResponse(savedSubmission);
    }

    @Transactional
    public AssignmentSubmissionResponse gradeSubmission(UUID courseId, UUID submissionId,
            AssignmentGradeRequest request, AuthenticatedUser authUser) {
        AssignmentSubmission submission = assignmentSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new NotFoundException("Submission not found"));

        if (!submission.getAssignment().getCourse().getId().equals(courseId)) {
            throw new BadRequestException("Submission does not belong to this course");
        }

        AppUser user = userRepository.findById(authUser.id())
                .orElseThrow(() -> new NotFoundException("User not found"));

        // 验证用户是否是课程教师
        if (!courseMemberRepository.existsByCourseAndUserAndMemberRoleIn(submission.getAssignment().getCourse(), user,
                List.of(CourseMemberRole.OWNER, CourseMemberRole.TEACHER))) {
            throw new ForbiddenException("Only course teachers can grade submissions");
        }

        // 计算总分（如果提供则使用，否则汇总题目得分）
        BigDecimal totalScore = request.totalScore();
        if (totalScore == null) {
            totalScore = request.itemGrades().stream()
                    .map(AssignmentGradeRequest.AssignmentItemGrade::score)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        submission.setTotalScore(totalScore);
        submission.setFeedback(request.feedback());
        submission.setGradedBy(user);
        submission.setGradedAt(LocalDateTime.now());

        AssignmentSubmission gradedSubmission = assignmentSubmissionRepository.save(submission);

        return convertToSubmissionResponse(gradedSubmission);
    }

    @Transactional(readOnly = true)
    public List<AssignmentSubmissionResponse> listSubmissions(UUID courseId, UUID assignmentId, AuthenticatedUser authUser) {
        Assignment assignment = assignmentRepository.findWithCourseById(assignmentId)
                .orElseThrow(() -> new NotFoundException("Assignment not found"));

        if (!assignment.getCourse().getId().equals(courseId)) {
            throw new BadRequestException("Assignment does not belong to this course");
        }

        AppUser user = userRepository.findById(authUser.id())
                .orElseThrow(() -> new NotFoundException("User not found"));

        // 教师可以查看所有提交，学生只能查看自己的提交
        boolean isTeacher = courseMemberRepository.existsByCourseAndUserAndMemberRoleIn(assignment.getCourse(), user,
                List.of(CourseMemberRole.OWNER, CourseMemberRole.TEACHER));

        List<AssignmentSubmission> submissions;
        if (isTeacher) {
            submissions = assignmentSubmissionRepository.findByAssignmentIdOrderBySubmittedAtDesc(assignmentId);
        } else {
            submissions = assignmentSubmissionRepository.findByAssignmentIdAndUserIdOrderBySubmittedAtDesc(assignmentId, user.getId());
        }

        return submissions.stream()
                .map(this::convertToSubmissionResponse)
                .toList();
    }

    // 辅助方法
    private String convertOptionsToJson(List<String> options) {
        try {
            return objectMapper.writeValueAsString(options);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert options to JSON", e);
        }
    }

    private List<String> convertJsonToOptions(String json) {
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private String convertAnswersToJson(List<AssignmentSubmissionRequest.AssignmentAnswer> answers) {
        try {
            Map<UUID, String> answerMap = new HashMap<>();
            for (AssignmentSubmissionRequest.AssignmentAnswer answer : answers) {
                answerMap.put(answer.taskItemId(), answer.answer());
            }
            return objectMapper.writeValueAsString(answerMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert answers to JSON", e);
        }
    }

    private Map<UUID, String> convertJsonToAnswers(String json) {
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructMapType(Map.class, UUID.class, String.class));
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    private AssignmentResponse.AssignmentTaskItemResponse convertToTaskItemResponse(AssignmentTaskItem item) {
        return new AssignmentResponse.AssignmentTaskItemResponse(
                item.getId(),
                item.getQuestion(),
                item.getQuestionType(),
                convertJsonToOptions(item.getOptionsJson()),
                item.getReferenceAnswer(),
                item.getMaxScore(),
                item.getSortOrder(),
                item.getOriginalTaskId()
        );
    }

    private AssignmentResponse convertToAssignmentResponse(Assignment assignment,
            List<AssignmentResponse.AssignmentTaskItemResponse> taskItems) {
        return new AssignmentResponse(
                assignment.getId(),
                assignment.getCourse().getId(),
                assignment.getTitle(),
                assignment.getDescription(),
                assignment.getTotalScore(),
                assignment.getStartAt(),
                assignment.getDueAt(),
                assignment.isRequired(),
                assignment.getSortOrder(),
                assignment.isPublished(),
                assignment.isNotifyOnStart(),
                assignment.isNotifyBeforeDue24h(),
                assignment.isNotifyOnDue(),
                assignment.isAutoCalculateTotal(),
                UserSummaryResponse.from(assignment.getCreatedBy()),
                assignment.getCreatedAt(),
                assignment.getUpdatedAt(),
                taskItems
        );
    }

    private AssignmentSubmissionResponse convertToSubmissionResponse(AssignmentSubmission submission) {
        Map<UUID, String> answers = convertJsonToAnswers(submission.getAnswersJson());

        // 获取题目信息并构建答案响应
        List<AssignmentSubmissionResponse.AssignmentAnswerResponse> answerResponses = new ArrayList<>();
        if (!answers.isEmpty()) {
            List<AssignmentTaskItem> taskItems = assignmentTaskItemRepository
                    .findByAssignmentIdOrderBySortOrderAscCreatedAtAsc(submission.getAssignment().getId());

            Map<UUID, AssignmentTaskItem> taskItemMap = new HashMap<>();
            for (AssignmentTaskItem item : taskItems) {
                taskItemMap.put(item.getId(), item);
            }

            for (Map.Entry<UUID, String> entry : answers.entrySet()) {
                AssignmentTaskItem item = taskItemMap.get(entry.getKey());
                if (item != null) {
                    answerResponses.add(new AssignmentSubmissionResponse.AssignmentAnswerResponse(
                            item.getId(),
                            item.getQuestion(),
                            item.getQuestionType(),
                            convertJsonToOptions(item.getOptionsJson()),
                            entry.getValue(),
                            item.getMaxScore(),
                            null, // 得分需要从批改记录中获取（简化）
                            null  // 反馈需要从批改记录中获取
                    ));
                }
            }
        }

        return new AssignmentSubmissionResponse(
                submission.getId(),
                submission.getAssignment().getId(),
                answerResponses,
                submission.getTotalScore(),
                submission.getFeedback(),
                submission.getGradedBy() != null ? UserSummaryResponse.from(submission.getGradedBy()) : null,
                submission.getGradedAt(),
                submission.isLatest(),
                UserSummaryResponse.from(submission.getSubmittedBy()),
                submission.getSubmittedAt(),
                submission.getCreatedAt(),
                submission.getUpdatedAt()
        );
    }
}