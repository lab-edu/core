package edu.lab.core.course.learning.dto;

import java.util.List;
import java.util.UUID;

public record LearningUnitOrderUpdateRequest(List<UUID> orderedUnitIds) {
}