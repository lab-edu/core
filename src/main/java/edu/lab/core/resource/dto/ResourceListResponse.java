package edu.lab.core.resource.dto;

import java.util.List;

public record ResourceListResponse(List<ResourceSummaryResponse> items) {
}