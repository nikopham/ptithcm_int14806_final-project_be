package com.ptithcm.movie.movie.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VideoUploadRequest {
    @NotBlank(message = "File name is required")
    private String fileName; // VD: "inception.mp4"

    @NotBlank(message = "Content type is required")
    private String contentType; // VD: "video/mp4"
}