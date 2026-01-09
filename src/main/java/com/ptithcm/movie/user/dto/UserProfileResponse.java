package com.ptithcm.movie.user.dto;

import com.ptithcm.movie.user.entity.User;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserProfileResponse {

    private final String id;
    private final String username;
    private final String email;
    private final String avatarUrl;
    private final List<String> roles;
    private boolean hasPassword;

    public UserProfileResponse(User user) {
        this.id        = user.getId().toString();
        this.username  = user.getUsername();
        this.email     = user.getEmail();
        this.avatarUrl = user.getAvatarUrl();
        this.roles     = List.of(user.getRole().getCode());

        this.hasPassword = user.getPasswordHash() != null && !user.getPasswordHash().isEmpty();
    }
}