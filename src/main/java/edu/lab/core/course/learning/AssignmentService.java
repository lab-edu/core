package edu.lab.core.course.learning;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.lab.core.common.exception.BadRequestException;
import edu.lab.core.common.exception.ForbiddenException;
import edu.lab.core.common.exception.NotFoundException;
import edu.lab.core.course.Course;
import edu.lab.core.course.CourseMemberRepository;
import edu.lab.core.course.CourseMemberRole;
import edu.lab.core.course.CourseRepository;
import edu.lab.core.course.learning.dto.AssignmentCreateRequest;
import edu.lab.core.course.learning.dto.AssignmentGradeRequest;
import edu.lab.core.course.learning.dto.AssignmentResponse;
import edu.lab.core.course.learning.dto.AssignmentSubmissionRequest;
import edu.lab.core.course.learning.dto.AssignmentSubmissionResponse;
import edu.lab.core.course.learning.dto.AssignmentUpdateRequest;
import edu.lab.core.security.AuthenticatedUser;
import edu.lab.core.user.AppUser;
import edu.lab.core.user.UserRepository;
import edu.lab.core.user.dto.UserSummaryResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
    private final ObjectMapper objectMapper;

    @Transactional
    public AssignmentResponse createAssignment(UUID courseId, AssignmentCreateRequest request, AuthenticatedUser authUser) {
        AssignmentContext context = requireTeacherContext(courseId, authUser.id());
        validateTimeWindow(request.startAt(), request.dueAt());

        Assignment assignment = new Assignment();
        assignment.setCourse(context.course());
        assignment.setCreatedBy(context.user());
        assignment.setTitle(normalizeTitle(request.title()));
        assignment.setDescription(normalizeNullableText(request.description()));
        assignment.setStartAt(request.startAt());
        assignment.setDueAt(request.dueAt());
        assignment.setRequired(Boolean.TRUE.equals(request.required()));
        assignment.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        assignment.setPublished(Boolean.TRUE.equals(request.published()));
        assignment.setNotifyOnStart(Boolean.TRUE.equals(request.notifyOnStart()));
        assignment.setNotifyBeforeDue24h(Boolean.TRUE.equals(request.notifyBeforeDue24h()));
        assignment.setNotifyOnDue(Boolean.TRUE.equals(request.notifyOnDue()));

        List<AssignmentTaskItem> taskItems = buildTaskItemsFromCreateRequest(assignment, request.taskItems());
        boolean autoCalc = request.autoCalculateTotal() == null || request.autoCalculateTotal();
        assignment.setAutoCalculateTotal(autoCalc);
        assignment.setTotalScore(resolveTotalScore(autoCalc, request.totalScore(), taskItems));

        Assignment savedAssignment = assignmentRepository.save(assignment);
        List<AssignmentTaskItem> savedTaskItems = assignmentTaskItemRepository.saveAll(taskItems);

        return convertToAssignmentResponse(savedAssignment, savedTaskItems.stream()
                .sorted(Comparator.comparingInt(AssignmentTaskItem::getSortOrder))
                .map(this::convertToTaskItemResponse)
                .toList());
    }

    @Transactional
    public AssignmentResponse updateAssignment(UUID courseId, UUID assignmentId, AssignmentUpdateRequest request, AuthenticatedUser authUser) {
        Assignment assignment = assignmentRepository.findWithCourseById(assignmentId)
                .orElseThrow(() -> new NotFoundException("Assignment not found"));

        if (!assignment.getCourse().getId().equals(courseId)) {
            throw new BadRequestException("Assignment does not belong to this course");
        }

        AppUser user = requireUser(authUser.id());
        if (!assignment.getCreatedBy().getId().equals(user.getId()) && !isTeacher(assignment.getCourse(), user)) {
            throw new ForbiddenException("Only assignment creator or course teachers can update");
        }

        validateTimeWindow(request.startAt(), request.dueAt());

        assignment.setTitle(normalizeTitle(request.title()));
        assignment.setDescription(normalizeNullableText(request.description()));
        assignment.setStartAt(request.startAt());
        assignment.setDueAt(request.dueAt());
        assignment.setRequired(request.required() != null ? request.required() : assignment.isRequired());
        assignment.setSortOrder(request.sortOrder() != null ? request.sortOrder() : assignment.getSortOrder());
        assignment.setPublished(request.published() != null ? request.published() : assignment.isPublished());
        assignment.setNotifyOnStart(request.notifyOnStart() != null ? request.notifyOnStart() : assignment.isNotifyOnStart());
        assignment.setNotifyBeforeDue24h(request.notifyBeforeDue24h() != null ? request.notifyBeforeDue24h() : assignment.isNotifyBeforeDue24h());
        assignment.setNotifyOnDue(request.notifyOnDue() != null ? request.notifyOnDue() : assignment.isNotifyOnDue());

        List<AssignmentTaskItem> taskItems;
        if (request.taskItems() != null) {
            assignmentTaskItemRepository.deleteByAssignmentId(assignmentId);
            taskItems = buildTaskItemsFromUpdateRequest(assignment, request.taskItems());
            taskItems = assignmentTaskItemRepository.saveAll(taskItems);
        } else {
            taskItems = assignmentTaskItemRepository.findByAssignmentIdOrderBySortOrderAscCreatedAtAsc(assignmentId);
        }

        boolean autoCalc = request.autoCalculateTotal() != null
                ? request.autoCalculateTotal()
                : assignment.isAutoCalculateTotal();
        assignment.setAutoCalculateTotal(autoCalc);
        assignment.setTotalScore(resolveTotalScore(autoCalc, request.totalScore(), taskItems));

        Assignment updatedAssignment = assignmentRepository.save(assignment);
        List<AssignmentResponse.AssignmentTaskItemResponse> taskItemResponses = taskItems.stream()
                .sorted(Comparator.comparingInt(AssignmentTaskItem::getSortOrder))
                .map(this::convertToTaskItemResponse)
                .toList();
        return convertToAssignmentResponse(updatedAssignment, taskItemResponses);
    }

    @Transactional
    public void deleteAssignment(UUID courseId, UUID assignmentId, AuthenticatedUser authUser) {
        Assignment assignment = assignmentRepository.findWithCourseById(assignmentId)
                .orElseThrow(() -> new NotFoundException("Assignment not found"));

        if (!assignment.getCourse().getId().equals(courseId)) {
            throw new BadRequestException("Assignment does not belong to this course");
        }

        AppUser user = requireUser(authUser.id());

        if (!assignment.getCreatedBy().getId().equals(user.getId()) && !isCourseOwner(assignment.getCourse(), user)) {
            throw new ForbiddenException("Only assignment creator or course owner can delete");
        }

        assignmentRepository.delete(assignment);
    }

    @Transactional(readOnly = true)
    public AssignmentResponse getAssignment(UUID courseId, UUID assignmentId, AuthenticatedUser authUser) {
        Assignment assignment = assignmentRepository.findWithCourseById(assignmentId)
                .orElseThrow(() -> new NotFoundException("Assignment not found"));

        if (!assignment.getCourse().getId().equals(courseId)) {
            throw new BadRequestException("Assignment does not belong to this course");
        }

        AppUser user = requireUser(authUser.id());
        if (!isCourseAccessible(assignment.getCourse(), user)) {
            throw new ForbiddenException("You are not a member of this course");
        }

        if (!assignment.isPublished() && !isTeacher(assignment.getCourse(), user)) {
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

        AppUser user = requireUser(authUser.id());
        if (!isCourseAccessible(course, user)) {
            throw new ForbiddenException("You are not a member of this course");
        }

        boolean teacher = isTeacher(course, user);

        List<Assignment> assignments;
        if (teacher) {
            assignments = assignmentRepository.findByCourseIdOrderBySortOrderAscCreatedAtAsc(courseId);
        } else {
            assignments = assignmentRepository.findPublishedByCourseId(courseId);
        }

        if (assignments.isEmpty()) {
            return List.of();
        }

        List<UUID> assignmentIds = assignments.stream().map(Assignment::getId).toList();
        Map<UUID, List<AssignmentTaskItem>> taskItemsByAssignmentId = assignmentTaskItemRepository
                .findByAssignmentIdsOrderByAssignmentIdAndSortOrderAsc(assignmentIds)
                .stream()
                .collect(Collectors.groupingBy(
                        item -> item.getAssignment().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return assignments.stream()
                .map(assignment -> {
                    List<AssignmentTaskItem> taskItems = taskItemsByAssignmentId.getOrDefault(assignment.getId(), List.of());
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

        if (assignment.getStartAt() != null && assignment.getStartAt().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Assignment has not started yet");
        }

        if (assignment.getDueAt() != null && assignment.getDueAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Assignment is past due");
        }

        AppUser user = requireUser(authUser.id());
        if (!isCourseAccessible(assignment.getCourse(), user)) {
            throw new ForbiddenException("You are not a member of this course");
        }
        if (isTeacher(assignment.getCourse(), user)) {
            throw new ForbiddenException("Teachers cannot submit assignments");
        }

        Map<UUID, AssignmentTaskItem> taskItemsById = assignmentTaskItemRepository
                .findByAssignmentIdOrderBySortOrderAscCreatedAtAsc(assignmentId)
                .stream()
                .collect(Collectors.toMap(AssignmentTaskItem::getId, item -> item));
        Map<UUID, String> normalizedAnswers = normalizeAnswers(request.answers(), taskItemsById);

        assignmentSubmissionRepository.markLatestFalseByAssignmentIdAndUserId(assignmentId, user.getId());

        AssignmentSubmission submission = new AssignmentSubmission();
        submission.setAssignment(assignment);
        submission.setSubmittedBy(user);
        submission.setAnswersJson(writeJson(normalizedAnswers, "Failed to convert answers to JSON"));
        submission.setItemGradesJson(null);
        submission.setLatest(true);
        submission.setSubmittedAt(LocalDateTime.now());

        AssignmentSubmission savedSubmission = assignmentSubmissionRepository.save(submission);

        return convertToSubmissionResponse(savedSubmission);
    }

    @Transactional
    public AssignmentSubmissionResponse gradeSubmission(UUID courseId, UUID submissionId,
            AssignmentGradeRequest request, AuthenticatedUser authUser) {
        AssignmentSubmission submission = assignmentSubmissionRepository.findWithRelationsById(submissionId)
                .orElseThrow(() -> new NotFoundException("Submission not found"));

        if (!submission.getAssignment().getCourse().getId().equals(courseId)) {
            throw new BadRequestException("Submission does not belong to this course");
        }

        AppUser user = requireUser(authUser.id());

        if (!isTeacher(submission.getAssignment().getCourse(), user)) {
            throw new ForbiddenException("Only course teachers can grade submissions");
        }

        Map<UUID, AssignmentTaskItem> taskItemsById = assignmentTaskItemRepository
                .findByAssignmentIdOrderBySortOrderAscCreatedAtAsc(submission.getAssignment().getId())
                .stream()
                .collect(Collectors.toMap(AssignmentTaskItem::getId, item -> item));

        Map<UUID, ItemGradeSnapshot> gradeMap = new HashMap<>();
        for (AssignmentGradeRequest.AssignmentItemGrade itemGrade : request.itemGrades()) {
            AssignmentTaskItem taskItem = taskItemsById.get(itemGrade.taskItemId());
            if (taskItem == null) {
                throw new BadRequestException("Grading item does not belong to this assignment");
            }
            if (itemGrade.score().compareTo(taskItem.getMaxScore()) > 0) {
                throw new BadRequestException("Item score cannot exceed max score");
            }
            gradeMap.put(itemGrade.taskItemId(), new ItemGradeSnapshot(itemGrade.score(), normalizeNullableText(itemGrade.feedback())));
        }

        BigDecimal totalScore = request.totalScore();
        if (totalScore == null) {
            totalScore = gradeMap.values().stream()
                    .map(ItemGradeSnapshot::score)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        if (totalScore.compareTo(submission.getAssignment().getTotalScore()) > 0) {
            throw new BadRequestException("Total score cannot exceed assignment total score");
        }

        submission.setTotalScore(totalScore);
        submission.setFeedback(normalizeNullableText(request.feedback()));
        submission.setItemGradesJson(writeJson(gradeMap, "Failed to convert item grades to JSON"));
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

        AppUser user = requireUser(authUser.id());
        if (!isCourseAccessible(assignment.getCourse(), user)) {
            throw new ForbiddenException("You are not a member of this course");
        }

        boolean teacher = isTeacher(assignment.getCourse(), user);

        List<AssignmentSubmission> submissions;
        if (teacher) {
            submissions = assignmentSubmissionRepository.findByAssignmentIdOrderBySubmittedAtDesc(assignmentId);
        } else {
            submissions = assignmentSubmissionRepository.findByAssignmentIdAndUserIdOrderBySubmittedAtDesc(assignmentId, user.getId());
        }

        return submissions.stream()
                .map(this::convertToSubmissionResponse)
                .toList();
    }

    private String writeJson(Object value, String errorMessage) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BadRequestException(errorMessage);
        }
    }

    private AssignmentContext requireTeacherContext(UUID courseId, UUID userId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found"));
        AppUser user = requireUser(userId);
        if (!isTeacher(course, user)) {
            throw new ForbiddenException("Only course teachers can create assignments");
        }
        return new AssignmentContext(course, user);
    }

    private AppUser requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private boolean isTeacher(Course course, AppUser user) {
        return isCourseOwner(course, user) || courseMemberRepository.existsByCourseAndUserAndMemberRoleIn(course, user,
                List.of(CourseMemberRole.TEACHER));
    }

    private boolean isCourseOwner(Course course, AppUser user) {
        return course.getOwner().getId().equals(user.getId());
    }

    private boolean isCourseAccessible(Course course, AppUser user) {
        if (isCourseOwner(course, user)) {
            return true;
        }
        return courseMemberRepository.existsByCourseIdAndUserId(course.getId(), user.getId());
    }

    private void validateTimeWindow(LocalDateTime startAt, LocalDateTime dueAt) {
        if (startAt != null && dueAt != null && dueAt.isBefore(startAt)) {
            throw new BadRequestException("Due time must be after start time");
        }
    }

    private BigDecimal resolveTotalScore(boolean autoCalc, BigDecimal requestTotalScore, List<AssignmentTaskItem> taskItems) {
        if (autoCalc) {
            return taskItems.stream()
                    .map(AssignmentTaskItem::getMaxScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        if (requestTotalScore == null) {
            throw new BadRequestException("totalScore is required when autoCalculateTotal is false");
        }
        return requestTotalScore;
    }

    private List<AssignmentTaskItem> buildTaskItemsFromCreateRequest(Assignment assignment,
            List<AssignmentCreateRequest.AssignmentTaskItemRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BadRequestException("At least one task item is required");
        }
        List<AssignmentTaskItem> items = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            AssignmentCreateRequest.AssignmentTaskItemRequest request = requests.get(i);
            items.add(buildTaskItem(
                    assignment,
                    request.question(),
                    request.questionType(),
                    request.options(),
                    request.referenceAnswer(),
                    request.maxScore(),
                    request.sortOrder(),
                    i,
                    null
            ));
        }
        return items;
    }

    private List<AssignmentTaskItem> buildTaskItemsFromUpdateRequest(Assignment assignment,
            List<AssignmentUpdateRequest.AssignmentTaskItemUpdateRequest> requests) {
        if (requests.isEmpty()) {
            throw new BadRequestException("At least one task item is required");
        }
        List<AssignmentTaskItem> items = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            AssignmentUpdateRequest.AssignmentTaskItemUpdateRequest request = requests.get(i);
            items.add(buildTaskItem(
                    assignment,
                    request.question(),
                    request.questionType(),
                    request.options(),
                    request.referenceAnswer(),
                    request.maxScore(),
                    request.sortOrder(),
                    i,
                    null
            ));
        }
        return items;
    }

    private AssignmentTaskItem buildTaskItem(
            Assignment assignment,
            String question,
            LearningQuestionType questionType,
            List<String> options,
            String referenceAnswer,
            BigDecimal maxScore,
            Integer sortOrder,
            int fallbackSortOrder,
            UUID originalTaskId) {
        AssignmentTaskItem taskItem = new AssignmentTaskItem();
        taskItem.setAssignment(assignment);
        taskItem.setQuestion(question.trim());
        taskItem.setQuestionType(questionType);
        taskItem.setOptionsJson(convertOptionsToJson(questionType, options));
        taskItem.setReferenceAnswer(normalizeNullableText(referenceAnswer));
        taskItem.setMaxScore(maxScore);
        taskItem.setSortOrder(sortOrder != null ? sortOrder : fallbackSortOrder);
        taskItem.setOriginalTaskId(originalTaskId);
        return taskItem;
    }

    private String normalizeTitle(String title) {
        String normalized = title == null ? "" : title.trim();
        if (normalized.isEmpty()) {
            throw new BadRequestException("Assignment title cannot be blank");
        }
        return normalized;
    }

    private String normalizeNullableText(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String convertOptionsToJson(LearningQuestionType questionType, List<String> options) {
        List<String> normalized = options == null
                ? List.of()
                : options.stream().map(String::trim).filter(opt -> !opt.isEmpty()).toList();
        if ((questionType == LearningQuestionType.SINGLE_CHOICE || questionType == LearningQuestionType.MULTIPLE_CHOICE)
                && normalized.size() < 2) {
            throw new BadRequestException("Choice questions require at least 2 options");
        }
        if (questionType == LearningQuestionType.SHORT_ANSWER) {
            normalized = List.of();
        }
        return writeJson(normalized, "Failed to convert options to JSON");
    }

    private List<String> convertJsonToOptions(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private Map<UUID, String> normalizeAnswers(
            List<AssignmentSubmissionRequest.AssignmentAnswer> answers,
            Map<UUID, AssignmentTaskItem> taskItemsById) {
        if (answers == null || answers.isEmpty()) {
            throw new BadRequestException("Submission answers cannot be empty");
        }
        Map<UUID, String> answerMap = new LinkedHashMap<>();
        for (AssignmentSubmissionRequest.AssignmentAnswer answer : answers) {
            AssignmentTaskItem taskItem = taskItemsById.get(answer.taskItemId());
            if (taskItem == null) {
                throw new BadRequestException("Answer task item does not belong to this assignment");
            }
            answerMap.put(answer.taskItemId(), answer.answer().trim());
        }
        Set<UUID> requiredTaskIds = taskItemsById.keySet();
        if (!answerMap.keySet().containsAll(requiredTaskIds)) {
            throw new BadRequestException("All assignment task items must be answered");
        }
        return answerMap;
    }

    private Map<UUID, String> convertJsonToAnswers(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructMapType(Map.class, UUID.class, String.class));
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    private Map<UUID, ItemGradeSnapshot> convertJsonToGradeMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(
                    json,
                    objectMapper.getTypeFactory().constructMapType(Map.class, UUID.class, ItemGradeSnapshot.class)
            );
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
        Map<UUID, ItemGradeSnapshot> gradeMap = convertJsonToGradeMap(submission.getItemGradesJson());

        List<AssignmentSubmissionResponse.AssignmentAnswerResponse> answerResponses = new ArrayList<>();
        List<AssignmentTaskItem> taskItems = assignmentTaskItemRepository
                .findByAssignmentIdOrderBySortOrderAscCreatedAtAsc(submission.getAssignment().getId());

        for (AssignmentTaskItem item : taskItems) {
            ItemGradeSnapshot grade = gradeMap.get(item.getId());
            answerResponses.add(new AssignmentSubmissionResponse.AssignmentAnswerResponse(
                    item.getId(),
                    item.getQuestion(),
                    item.getQuestionType(),
                    convertJsonToOptions(item.getOptionsJson()),
                    answers.getOrDefault(item.getId(), ""),
                    item.getMaxScore(),
                    grade == null ? null : grade.score(),
                    grade == null ? null : grade.feedback()
            ));
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

    private record AssignmentContext(Course course, AppUser user) {
    }

    private record ItemGradeSnapshot(BigDecimal score, String feedback) {
    }
}
