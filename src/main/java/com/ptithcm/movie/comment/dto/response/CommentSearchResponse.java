package com.ptithcm.movie.comment.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class CommentSearchResponse {
    private UUID id;
    private String body;
    private OffsetDateTime createdAt;
    private boolean isEdited;
    private UUID parentId;

    private UUID userId;
    private String username;
    private String userAvatar;
    private String userRole;

    private Long replyCount;

    private MovieInfo movie;
    private boolean isHidden;

    @Data
    @Builder
    public static class MovieInfo {
        private UUID id;
        private String title;
        private String originalTitle;
        private String slug;
        private String posterUrl;
        private String backdropUrl;
        private Integer releaseYear;
        private Boolean isSeries;
    }
}

