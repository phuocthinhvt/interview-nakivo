package com.nakivo.job.model; // Example package name following the company context

import com.fasterxml.jackson.databind.JsonNode;
import com.nakivo.job.enums.JobStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "jobs",
        indexes = {
                @Index(name = "idx_jobs_status_created_at", columnList = "status, created_at DESC")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type", nullable = false, length = 100)
    private String type;

    // Use LOB or LONGTEXT for flexible, large payload string (e.g., JSON structure)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload")
    private JsonNode payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private JobStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // Inside Job.java Entity

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L; // Gán mặc định là 0L để tránh bị gán thành null khi dùng Builder
}