package edu.lab.core.experiment.dto;

import java.util.List;

public record ExperimentListResponse(List<ExperimentSummaryResponse> items) {
}