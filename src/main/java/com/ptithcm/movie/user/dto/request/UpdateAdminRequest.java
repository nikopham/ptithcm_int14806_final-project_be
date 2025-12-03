package com.ptithcm.movie.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UpdateAdminRequest {
    @Email(message = "Invalid email format")
    private String email;

    @Size(min = 3, message = "Username must be at least 3 characters")
    private String username;

    private MultipartFile avatar;

    @Pattern(regexp = "^(movie_admin|comment_admin)$", message = "Invalid role code")
    private String roleCode;

    private Boolean isActive;
}
