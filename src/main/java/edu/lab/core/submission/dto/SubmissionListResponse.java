package edu.lab.core.submission.dto;

import java.util.List;

public record SubmissionListResponse(List<SubmissionDetailResponse> items) {
}