package com.ptithcm.movie.movie.controller;

import com.ptithcm.movie.common.dto.ServiceResult;
import com.ptithcm.movie.movie.dto.UpdateSeasonDto;
import com.ptithcm.movie.movie.service.SeasonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/seasons")
public class SeasonController {

    @Autowired
    private SeasonService seasonService;

    /**
     * API Cập nhật thông tin Season VÀ các Episodes của nó
     */
    @PutMapping("/update/{id}")
    public ServiceResult updateSeason(
            @PathVariable UUID id,
            @RequestBody UpdateSeasonDto dto
    ) {
        return seasonService.updateSeason(id, dto);
    }
}