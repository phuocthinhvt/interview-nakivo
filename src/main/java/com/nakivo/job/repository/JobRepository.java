package com.nakivo.job.repository;

import com.nakivo.job.enums.JobStatus;
import com.nakivo.job.model.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    // Automatically handles pagination and filtering by Status
    Page<Job> findByStatus(JobStatus status, Pageable pageable);
    List<Job> findByStatus(JobStatus status);
    // Standard synchronous CRUD operations are automatically provided
}