package com.ptithcm.movie.user.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserStatusRequest {
    @NotNull(message = "Active status is required")
    private Boolean active;
}
