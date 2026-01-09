package com.ptithcm.movie.movie.dto.request;

import com.ptithcm.movie.common.constant.PersonJob;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class PersonRequest {
    @NotBlank(message = "Full name is required")
    private String fullName;

    private List<String> job;

    private MultipartFile avatar;
}