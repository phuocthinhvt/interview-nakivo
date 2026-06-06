package com.nakivo.job.service;

public interface JobExecutionService {
    void executeValidJob(Long jobId);
    void handleJobFailure(Long jobId, String errorMsg);
}