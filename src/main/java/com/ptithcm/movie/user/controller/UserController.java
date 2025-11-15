package com.ptithcm.movie.user.controller;

import com.ptithcm.movie.common.dto.ServiceResult;
import com.ptithcm.movie.auth.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.ptithcm.movie.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ServiceResult me(@AuthenticationPrincipal UserPrincipal currentUser) {
        return userService.getCurrentUser(currentUser);
    }
}