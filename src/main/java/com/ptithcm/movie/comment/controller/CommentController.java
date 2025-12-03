package com.ptithcm.movie.comment.controller;

import com.ptithcm.movie.comment.dto.request.CommentRequest;
import com.ptithcm.movie.comment.service.CommentService;
import com.ptithcm.movie.common.dto.ServiceResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/movies/{movieId}/comments")
    public ResponseEntity<ServiceResult> getMovieComments(
            @PathVariable UUID movieId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(commentService.getCommentsByMovie(movieId, pageable));
    }

    @PostMapping("/comments")
    public ResponseEntity<ServiceResult> createComment(
            @RequestBody @Valid CommentRequest request
    ) {
        return ResponseEntity.ok(commentService.createComment(request));
    }

    @PutMapping("/comments/{id}")
    public ResponseEntity<ServiceResult> editComment(
            @PathVariable UUID id,
            @RequestBody @Valid CommentRequest request
    ) {
        return ResponseEntity.ok(commentService.editComment(id, request));
    }
}