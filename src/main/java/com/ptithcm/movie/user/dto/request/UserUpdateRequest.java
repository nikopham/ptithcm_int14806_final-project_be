package com.ptithcm.movie.user.dto.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UserUpdateRequest {
    private String username;
    private MultipartFile avatar;
}
