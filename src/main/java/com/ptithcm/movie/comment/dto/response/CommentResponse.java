package com.ptithcm.movie.comment.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class CommentResponse {
    private UUID id;
    private String body;
    private OffsetDateTime createdAt;
    private boolean isEdited;
    private UUID parentId;

    private UUID userId;
    private String username;
    private String userAvatar;

    private Long replyCount;
}
