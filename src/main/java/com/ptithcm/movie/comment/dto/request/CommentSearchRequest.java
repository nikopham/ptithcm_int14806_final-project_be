package com.ptithcm.movie.comment.dto.request;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class CommentSearchRequest {
    private UUID userId;
    private UUID movieId;
    private String movieTitle;
    private Boolean isHidden; // optional filter; if null, return all

    // Optional time range filters (future-proof). Not required by user ask but harmless.
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime from;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime to;
}

