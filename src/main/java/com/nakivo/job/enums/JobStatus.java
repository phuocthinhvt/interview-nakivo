package com.nakivo.job.enums;

public enum JobStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED // Added FAILED status as a tech lead practice for handling job errors
}