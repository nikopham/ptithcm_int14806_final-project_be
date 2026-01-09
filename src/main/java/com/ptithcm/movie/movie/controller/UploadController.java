package com.ptithcm.movie.movie.controller;

import com.ptithcm.movie.common.dto.ServiceResult;
import com.ptithcm.movie.external.cloudflare.CloudflareService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
public class UploadController {

    private final CloudflareService cloudflareService;

    @PostMapping("/video-url")
    public ResponseEntity<ServiceResult> getVideoUploadUrl() {
        return ResponseEntity.ok(ServiceResult.Success()
                .data(cloudflareService.getDirectUploadUrl()));
    }
}

