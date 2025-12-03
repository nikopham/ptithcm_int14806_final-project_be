package com.ptithcm.movie.movie.dto.request;

import com.ptithcm.movie.common.constant.PersonJob;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class PersonRequest {
    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotNull(message = "Job is required")
    private PersonJob job;

    private MultipartFile avatar;
}