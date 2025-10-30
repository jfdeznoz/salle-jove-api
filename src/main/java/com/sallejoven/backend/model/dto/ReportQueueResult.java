package com.sallejoven.backend.model.dto;

public record ReportQueueResult(long jobId, String resultKey, String outputPrefix, String environment) {}
