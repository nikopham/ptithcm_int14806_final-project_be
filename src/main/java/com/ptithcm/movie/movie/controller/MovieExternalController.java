package com.ptithcm.movie.movie.controller;

import com.ptithcm.movie.common.dto.ServiceResult;
import com.ptithcm.movie.movie.tmdb.TmdbService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/external/tmdb")
@RequiredArgsConstructor
public class MovieExternalController {

    private final TmdbService svc;

    @GetMapping("/search")
    public ServiceResult search(@RequestParam String q,
                                @RequestParam(defaultValue = "1") int page) {
        return ServiceResult.Success().data(svc.search(q, page));
    }

    @GetMapping("/{id}")
    public ServiceResult detail(@PathVariable int id) {
        return ServiceResult.Success().data(svc.getDetail(id));
    }
}