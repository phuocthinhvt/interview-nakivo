package com.nakivo.job.service;

import com.nakivo.job.dto.JobRequest;
import com.nakivo.job.dto.Response;
import com.nakivo.job.enums.JobStatus;
import com.nakivo.job.model.Job;
import org.springframework.data.domain.Page;

import java.util.List;


public interface JobService {
    Response createJob(JobRequest request);
    Response getJobById(Long id);
    Response getJobsByStatus(JobStatus status, int page, int size);
    Response processPendingJobs();
}
