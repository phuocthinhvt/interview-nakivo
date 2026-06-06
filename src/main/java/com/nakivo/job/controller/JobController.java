package com.nakivo.job.controller;

import com.nakivo.job.dto.JobRequest;
import com.nakivo.job.dto.Response;
import com.nakivo.job.enums.JobStatus;
import com.nakivo.job.service.JobService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jobs")
@AllArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping
    public ResponseEntity<Response> createJob(@Valid @RequestBody JobRequest request) {
        Response createdJob = jobService.createJob(request);
        return getResponse(createdJob, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response> getJobById(@PathVariable Long id) {
        Response job = jobService.getJobById(id);
        return getResponse(job, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<Response> getJobs(
            @RequestParam JobStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Response pagedJobs = jobService.getJobsByStatus(status, page, size);
        return getResponse(pagedJobs, HttpStatus.OK);
    }

    @PostMapping("/process")
    public ResponseEntity<Response> triggerProcessJobs() {
        Response process =jobService.processPendingJobs();
        return getResponse(process, HttpStatus.OK);
    }

    public <T> ResponseEntity<T>  getResponse(T object, HttpStatus status) {
        return ResponseEntity
                .status(status)
                .body(object);
    }
}