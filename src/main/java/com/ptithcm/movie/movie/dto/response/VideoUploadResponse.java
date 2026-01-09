package com.ptithcm.movie.movie.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VideoUploadResponse {
    private String uploadUrl; // Link PUT file lên S3
    private String videoKey;  // Key lưu vào DB (VD: raw/uuid.mp4)
}
