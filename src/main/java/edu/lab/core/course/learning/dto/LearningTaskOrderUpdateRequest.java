package edu.lab.core.course.learning.dto;

import java.util.List;
import java.util.UUID;

public record LearningTaskOrderUpdateRequest(List<UUID> orderedTaskIds) {
}