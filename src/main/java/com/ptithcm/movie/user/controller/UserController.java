package com.ptithcm.movie.user.controller;

import com.ptithcm.movie.auth.security.UserPrincipal;
import com.ptithcm.movie.common.dto.ServiceResult;
import com.ptithcm.movie.user.dto.request.*;
import com.ptithcm.movie.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ServiceResult me(@AuthenticationPrincipal UserPrincipal currentUser) {
        return userService.getCurrentUser(currentUser);
    }

    @PutMapping(value = "/update/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ServiceResult> updateProfile(
            @PathVariable UUID id,
            @ModelAttribute UserUpdateRequest request
    ) {
        return ResponseEntity.ok(userService.updateProfile(id, request));
    }

    @PutMapping("/update/{id}/password")
    public ResponseEntity<ServiceResult> changePassword(
            @PathVariable UUID id,
            @RequestBody @Valid ChangePasswordRequest request
    ) {
        return ResponseEntity.ok(userService.changePassword(id, request));
    }

    @GetMapping("/search-viewers")
    public ResponseEntity<ServiceResult> searchViewers(
            @ModelAttribute UserSearchRequest request,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(userService.searchViewers(request, pageable));
    }

    @PatchMapping("/update/{id}/status")
    public ResponseEntity<ServiceResult> updateUserStatus(
            @PathVariable UUID id,
            @RequestBody @Valid UserStatusRequest request
    ) {
        return ResponseEntity.ok(userService.updateUserStatus(id, request));
    }

    @GetMapping("/search-admins")
    public ResponseEntity<ServiceResult> searchAdmins(
            @ModelAttribute AdminSearchRequest request,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(userService.searchAdmins(request, pageable));
    }

    @PostMapping(value = "/create-admin", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ServiceResult> createAdmin(
            @ModelAttribute @Valid CreateAdminRequest request
    ) {
        return ResponseEntity.ok(userService.createAdmin(request));
    }

    @PutMapping(value = "/update-admin/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ServiceResult> updateAdmin(
            @PathVariable UUID id,
            @ModelAttribute @Valid UpdateAdminRequest request
    ) {
        return ResponseEntity.ok(userService.updateAdmin(id, request));
    }

    @PutMapping("/update-admin/{id}/password")
    public ResponseEntity<ServiceResult> resetAdminPassword(
            @PathVariable UUID id,
            @RequestBody @Valid AdminPasswordResetRequest request
    ) {
        return ResponseEntity.ok(userService.resetAdminPassword(id, request.getNewPassword()));
    }

    @DeleteMapping("/delete-admin/{id}")
    public ResponseEntity<ServiceResult> deleteAdmin(@PathVariable UUID id,
                                                     @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(userService.deleteAdmin(id,currentUser));
    }

    @PutMapping(value = "/update-profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ServiceResult> updateUserProfile(
            @ModelAttribute @Valid UserProfileUpdateRequest request
    ) {
        return ResponseEntity.ok(userService.updateUserProfile(request));
    }

    @PutMapping("/update-profile-password")
    public ResponseEntity<ServiceResult> changePasswordUserProfile(
            @RequestBody @Valid ChangePasswordProfileRequest request
    ) {
        return ResponseEntity.ok(userService.changePasswordUserProfile(request));
    }
}