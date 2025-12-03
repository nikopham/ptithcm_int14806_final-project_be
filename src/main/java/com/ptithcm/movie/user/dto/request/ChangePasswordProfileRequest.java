package com.ptithcm.movie.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class ChangePasswordProfileRequest {
    private UUID id;

    @NotBlank(message = "Current password is required")
    private String currentPw;

    @NotBlank(message = "New password is required")
    @Size(min = 6, message = "New password must be at least 6 characters")
    private String newPw;
}
