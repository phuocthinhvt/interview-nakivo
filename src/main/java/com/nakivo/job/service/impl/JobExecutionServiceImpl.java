package com.nakivo.job.service.impl;

import com.nakivo.job.enums.JobStatus;
import com.nakivo.job.model.Job;
import com.nakivo.job.repository.JobRepository;
import com.nakivo.job.service.JobExecutionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.nakivo.job.constants.ConfigConstant.MAX_RETRY;
import static com.nakivo.job.constants.ConfigConstant.PROCESS_PENDING_JOB_FAILED;

@Service
@RequiredArgsConstructor
public class JobExecutionServiceImpl implements JobExecutionService {

    private static final Logger log = LoggerFactory.getLogger(JobExecutionServiceImpl.class);
    private final JobRepository jobRepository;


    @Override
    @Transactional
    public void executeValidJob(Long jobId) {
        log.info("Starting background processing execution for Job ID: {}", jobId);

        Job job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.warn("Job ID {} no longer exists in database. Aborting.", jobId);
            return;
        }

        try {
            boolean isProcessFailed = false;
            if (job.getPayload() != null && job.getPayload().has("fail")) {
                isProcessFailed = job.getPayload().get("fail").asBoolean();
            }

            if (isProcessFailed) {
                throw new RuntimeException(PROCESS_PENDING_JOB_FAILED);
            }

            job.setStatus(JobStatus.COMPLETED);
            job.setErrorMessage(null);
            jobRepository.save(job);
            log.info("Job ID {} successfully processed and marked as COMPLETED", jobId);

        } catch (Exception e) {
            this.handleJobFailure(jobId, e.getMessage());
        }
    }

    @Override
    public void handleJobFailure(Long jobId, String errorMsg) {
        jobRepository.findById(jobId).ifPresent(job -> {
            int currentRetry = job.getRetryCount() + 1;
            job.setRetryCount(currentRetry);
            job.setErrorMessage(errorMsg);

            if (currentRetry >= MAX_RETRY) {
                job.setStatus(JobStatus.FAILED);
                log.error("Job ID {} failed after reaching max retries ({}/{}).", jobId, currentRetry, MAX_RETRY);
            } else {
                job.setStatus(JobStatus.PENDING);
                log.warn("Job ID {} failed execution. Retry count incremented to: {}", jobId, currentRetry);
            }
            jobRepository.save(job);
        });
    }
}