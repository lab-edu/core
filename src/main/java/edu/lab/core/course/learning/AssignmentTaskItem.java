package edu.lab.core.course.learning;

import edu.lab.core.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "assignment_task_items")
public class AssignmentTaskItem extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @Column(nullable = false, length = 500)
    private String question;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 20)
    private LearningQuestionType questionType;

    @Column(name = "options_json", columnDefinition = "text")
    private String optionsJson;

    @Column(name = "reference_answer", columnDefinition = "text")
    private String referenceAnswer;

    @Column(name = "max_score", nullable = false, precision = 10, scale = 2)
    private BigDecimal maxScore;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    // 用于迁移：关联原学习任务
    @Column(name = "original_task_id")
    private UUID originalTaskId;
}