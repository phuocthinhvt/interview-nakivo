package com.nakivo.job.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nakivo.job.dto.JobRequest;
import com.nakivo.job.dto.Response;
import com.nakivo.job.enums.JobStatus;
import com.nakivo.job.exception.InternalException;
import com.nakivo.job.exception.NoContentException;
import com.nakivo.job.exception.ResourceNotFoundException;
import com.nakivo.job.model.Job;
import com.nakivo.job.repository.JobRepository;
import com.nakivo.job.service.JobExecutionService;
import com.nakivo.job.service.JobService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.nakivo.job.constants.ConfigConstant.*;

@Service
@AllArgsConstructor
public class JobServiceImpl implements JobService {

    private static final Logger log = LoggerFactory.getLogger(JobServiceImpl.class);
    private final JobRepository jobRepository;
    private final ObjectMapper objectMapper;
    private final JobExecutionService jobExecutionService;

    @Transactional
    @Override
    public Response createJob(JobRequest request) {
        try {
            log.info("Processing job creation with payload: {}", objectMapper.writeValueAsString(request));

            JsonNode jsonPayload = objectMapper.valueToTree(request.getPayload());
            Job job = Job.builder()
                    .type(request.getType())
                    .payload(jsonPayload)
                    .status(JobStatus.PENDING)
                    .retryCount(0)
                    .build();

            Job savedJob = jobRepository.save(job);
            log.info("Successfully persisted job with jobId: {}", savedJob.getId());

            return Response.builder().data(savedJob).build();
        } catch (JsonProcessingException e) {
            throw new InternalException(e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Response getJobById(Long id) {
        log.info("get job from DB for ID: {}", id);
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(JOB_NOT_FOUND + id));
        return Response.builder().data(job).build();
    }

    @Transactional(readOnly = true)
    @Override
    public Response getJobsByStatus(JobStatus status, int page, int size) {
        log.info("get jobs with status: {}, page: {}, size: {}", status, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Job> jobs = jobRepository.findByStatus(status, pageable);
        return Response.builder().data(jobs).build();
    }

    @Override
    public Response processPendingJobs() {
        log.info("Starting background processing for pending jobs...");
        List<Job> pendingJobs = jobRepository.findByStatus(JobStatus.PENDING);
        if (pendingJobs.isEmpty()) {
            throw new NoContentException(NO_PENDING_JOB);
        }

        for (Job job : pendingJobs) {
            try {
                boolean isValidJob = executePendingJob(job.getId());

                if (isValidJob) {
                    CompletableFuture.runAsync(() -> jobExecutionService.executeValidJob(job.getId()));
                }
            } catch (ObjectOptimisticLockingFailureException e) {
                log.warn("[CONCURRENCY_CONTROL] Job ID {} was already processed by another thread. Skipping.", job.getId());
            } catch (Exception e) {
                log.error("Unexpected error for Job ID: {}", job.getId(), e);
            }
        }

        return Response.builder()
                .status(HttpStatus.OK.value())
                .message(PROCESS_PENDING_JOB_SUCCESSFUL)
                .build();
    }

    @Transactional
    public boolean executePendingJob(Long jobId) {

        Job job = jobRepository.findById(jobId).orElse(null);
        if (job == null || job.getStatus() != JobStatus.PENDING) {
            return false;
        }

        job.setStatus(JobStatus.PROCESSING);
        jobRepository.saveAndFlush(job);
        return true;
    }
}