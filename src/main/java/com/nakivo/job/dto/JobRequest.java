package com.nakivo.job.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobRequest {

    @NotBlank(message = "Job type must not be blank")
    private String type;

    @NotNull(message = "Payload must not be null")
    private Object payload;
}