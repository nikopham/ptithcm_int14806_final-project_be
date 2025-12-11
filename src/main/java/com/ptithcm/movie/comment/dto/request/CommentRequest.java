package com.ptithcm.movie.comment.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class CommentRequest {

    private UUID movieId;

    private UUID episodeId;

    private UUID parentId;

    @NotBlank(message = "Content is required")
    private String body;
}
