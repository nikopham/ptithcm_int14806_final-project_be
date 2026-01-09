package com.ptithcm.movie.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Data
public class UserProfileUpdateRequest {
    private UUID id;

    @Size(min = 3, message = "Username must be at least 3 characters")
    private String username;

    @Email(message = "Invalid email format")
    private String email;

    private MultipartFile avatar;
}