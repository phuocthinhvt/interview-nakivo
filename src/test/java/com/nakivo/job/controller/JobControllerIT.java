package com.nakivo.job.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nakivo.job.enums.JobStatus;
import com.nakivo.job.model.Job;
import com.nakivo.job.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class JobControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();
    }

    // ==========================================
    // 1. CREATE JOB SUCCESSFULLY
    // ==========================================
    @Test
    void createJob_ShouldReturn201Created() throws Exception {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("recipient", "phuocthinh@nakivo.com");

        ObjectNode request = objectMapper.createObjectNode();
        request.put("type", "EMAIL");
        request.set("payload", payload);

        mockMvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.type", is("EMAIL")))
                .andExpect(jsonPath("$.data.status", is("PENDING")));
    }

    // ==========================================
    // 2. GET JOB BY ID
    // ==========================================
    @Test
    void getJobById_ShouldReturn200Ok_WhenJobExists() throws Exception {
        Job job = jobRepository.save(Job.builder()
                .type("NOTIFICATION")
                .status(JobStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .version(0L)
                .build());

        mockMvc.perform(get("/api/jobs/" + job.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(job.getId().intValue())))
                .andExpect(jsonPath("$.data.type", is("NOTIFICATION")));
    }

    // ==========================================
    // 3. RETURN PROPER ERROR WHEN JOB DOES NOT EXIST
    // ==========================================
    @Test
    void getJobById_ShouldReturn404NotFound_WhenJobDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/jobs/12345"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Job not founded with id :12345")));
    }

    // ==========================================
    // 4. LIST JOBS BY STATUS
    // ==========================================
    @Test
    void getJobs_ShouldReturnListJobs() throws Exception {
        jobRepository.save(Job.builder().type("JOB1").status(JobStatus.PENDING).createdAt(LocalDateTime.now()).version(0L).build());
        jobRepository.save(Job.builder().type("JOB2").status(JobStatus.COMPLETED).createdAt(LocalDateTime.now()).version(0L).build());
        jobRepository.save(Job.builder().type("JOB3").status(JobStatus.PENDING).createdAt(LocalDateTime.now()).version(0L).build());

        mockMvc.perform(get("/api/jobs?status=PENDING&page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].type", is("JOB3")))
                .andExpect(jsonPath("$.data[1].type", is("JOB1")));

    }

    // ==========================================
    // 5. PROCESS JOB SUCCESSFULLY
    // ==========================================
    @Test
    void processJob_ShouldReturn200Ok_AndCompleteJob() throws Exception {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("fail", false);

        Job job = jobRepository.save(Job.builder()
                .type("EMAIL")
                .payload(payload)
                .status(JobStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .retryCount(0)
                .version(0L)
                .build());

        mockMvc.perform(post("/api/jobs/process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", notNullValue()))
                .andExpect(jsonPath("$.message", notNullValue()));

        Thread.sleep(1500);

        Job processedJob = jobRepository.findById(job.getId()).orElseThrow();
        assertEquals(JobStatus.COMPLETED, processedJob.getStatus());
    }

    // ==========================================
    // 6. RETRY FAILED JOB
    // ==========================================
    @Test
    void processJob_ShouldIncrementRetryCount_WhenProcessingFails() throws Exception {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("fail", true);

        Job job = jobRepository.save(Job.builder()
                .type("EMAIL")
                .payload(payload)
                .status(JobStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .retryCount(0)
                .version(0L)
                .build());

        mockMvc.perform(post("/api/jobs/process"))
                .andExpect(status().isOk());

        Thread.sleep(1500);

        Job processedJob = jobRepository.findById(job.getId()).orElseThrow();
        assertEquals(JobStatus.PENDING, processedJob.getStatus());
        assertEquals(1, processedJob.getRetryCount());
    }

    // ==========================================
    // 7. MARK JOB AS FAILED AFTER MAX RETRY COUNT
    // ==========================================
    @Test
    void processJob_ShouldMarkAsFailed_WhenMaxRetryReached() throws Exception {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("fail", true);

        Job job = jobRepository.save(Job.builder()
                .type("EMAIL")
                .payload(payload)
                .status(JobStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .retryCount(2)
                .version(0L)
                .build());

        mockMvc.perform(post("/api/jobs/process"))
                .andExpect(status().isOk());

        Thread.sleep(1500);

        Job processedJob = jobRepository.findById(job.getId()).orElseThrow();
        assertEquals(JobStatus.FAILED, processedJob.getStatus());
        assertEquals(3, processedJob.getRetryCount());
    }

    // ==========================================
    // 8. AVOID DUPLICATE PROCESSING (CONCURRENT TEST)
    // ==========================================
    @Test
    void processJob_ShouldPreventDuplicateProcessing_UnderConcurrency() throws Exception {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("fail", false);

        Job job = jobRepository.save(Job.builder()
                .type("EMAIL")
                .payload(payload)
                .status(JobStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .retryCount(0)
                .version(0L)
                .build());

        int threadCount = 4;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        List<Integer> responseStatuses = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            executorService.execute(() -> {
                try {
                    latch.await();
                    int statusCode = mockMvc.perform(post("/api/jobs/process"))
                            .andReturn()
                            .getResponse()
                            .getStatus();
                    synchronized (responseStatuses) {
                        responseStatuses.add(statusCode);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        latch.countDown();
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(threadCount, responseStatuses.size());
        for (int statusCode : responseStatuses) {
            assertEquals(200, statusCode);
        }

        Job finalJobState = jobRepository.findById(job.getId()).orElseThrow();
        assertEquals(JobStatus.COMPLETED, finalJobState.getStatus());
        assertEquals(2, finalJobState.getVersion());
    }
}