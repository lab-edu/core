package edu.lab.core.course.learning;

import edu.lab.core.domain.AuditableEntity;
import edu.lab.core.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "assignment_submissions")
public class AssignmentSubmission extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser submittedBy;

    @Column(name = "answers_json", columnDefinition = "text")
    private String answersJson;

    @Column(name = "item_grades_json", columnDefinition = "text")
    private String itemGradesJson;

    @Column(name = "total_score", precision = 10, scale = 2)
    private BigDecimal totalScore;

    @Column(columnDefinition = "text")
    private String feedback;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graded_by")
    private AppUser gradedBy;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt;

    @Column(nullable = false)
    private boolean latest;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;
}