package com.ptithcm.movie.common.constant;

public enum VideoUploadStatus {
    PENDING,    // Mới tạo, chưa upload video
    PROCESSING, // Đang transcode trên Worker
    READY,      // Đã xong, có thể xem
    FAILED      // Lỗi transcode
}
