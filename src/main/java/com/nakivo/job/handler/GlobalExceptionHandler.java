package com.nakivo.job.handler;

import com.nakivo.job.dto.Response;
import com.nakivo.job.exception.InternalException;
import com.nakivo.job.exception.NoContentException;
import com.nakivo.job.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Response> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("[JOB_NOT_FOUND] Job not founded in database", ex);
        Response errorResponse = Response.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler({InternalException.class})
    public ResponseEntity<Response> handleInternalException(InternalException ex) {
        log.error("[INTERNAL_EXCEPTION] Core logic failure encountered: ", ex);
        Response errorResponse = Response.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler({NoContentException.class})
    public ResponseEntity<Response> handleNoContentException(NoContentException ex) {
        log.warn("[NO_CONTENT] No Pending Job founded", ex);
        Response errorResponse = Response.builder()
                .status(HttpStatus.NO_CONTENT.value())
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Response> handleValidationExceptions(MethodArgumentNotValidException ex) {
        
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("Validation failed");

        log.warn("[VALIDATION_FAILED] Incoming request parameters payload rejected: {}", errorMessage);

        Response response = Response.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message(errorMessage)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response> handleUncaughtGlobalException(Exception ex) {
        log.error("[SYSTEM_EXCEPTION] Uncaught system crash detected: ", ex);
        Response errorResponse = Response.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("An unexpected internal server error occurred. Please contact the system administrator.")
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

}