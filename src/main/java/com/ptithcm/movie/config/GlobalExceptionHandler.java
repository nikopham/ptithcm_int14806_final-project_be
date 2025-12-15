package com.ptithcm.movie.config;

import com.ptithcm.movie.common.constant.ErrorCode;
import com.ptithcm.movie.common.dto.ServiceResult;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // Trả về 400
    public ServiceResult handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        // Cố gắng lấy thông điệp lỗi gốc (ví dụ: "movies_tmdb_id_key")
        String message = ex.getMostSpecificCause().getMessage();

        // Rút gọn thông điệp
        if (message.contains("movies_tmdb_id_key")) {
            message = "A movie with this TMDb ID already exists.";
        } else if (message.contains("movies_imdb_id_key")) {
            message = "A movie with this IMDb ID already exists.";
        } else {
            message = "Database constraint violation.";
        }

        return ServiceResult.Failure().message(message);
    }

    /**
     * Bắt lỗi I/O (ví dụ: Cloudinary upload thất bại)
     */
    @ExceptionHandler(IOException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // Trả về 500
    public ServiceResult handleIOException(IOException ex) {
        return ServiceResult.Failure().message("File upload failed: " + ex.getMessage());
    }

    /**
     * Bắt lỗi gọi API (ví dụ: TMDb service bị sập)
     */
    @ExceptionHandler(WebClientResponseException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE) // Trả về 503
    public ServiceResult handleWebClientException(WebClientResponseException ex) {
        return ServiceResult.Failure()
                .message("External API call failed: " + ex.getStatusText());
    }

    /**
     * Bắt tất cả các RuntimeException khác
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // Trả về 500
    public ServiceResult handleRuntimeException(RuntimeException ex) {
        return ServiceResult.Failure().code(ErrorCode.INTERNAL_SERVER).message("An unexpected error occurred: " + ex.getMessage());
    }


    // 2. Bắt lỗi Validation (@Valid failed)
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ServiceResult> handleValidationException(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ServiceResult.Failure()
                        .code(ErrorCode.INVALIDATE_REQUEST)
                        .message("Invalid request data"));
    }

    // 3. Bắt tất cả lỗi còn lại (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ServiceResult> handleGlobalException(Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ServiceResult.Failure()
                        .code(ErrorCode.FAILED)
                        .message("Internal Server Error: " + ex.getMessage()));
    }
}
