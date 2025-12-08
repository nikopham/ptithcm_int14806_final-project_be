package com.ptithcm.movie.user.service;


import com.ptithcm.movie.auth.security.UserPrincipal;
import com.ptithcm.movie.common.constant.ErrorCode;
import com.ptithcm.movie.common.constant.GlobalConstant;
import com.ptithcm.movie.common.dto.ServiceResult;
import com.ptithcm.movie.external.cloudinary.CloudinaryService;
import com.ptithcm.movie.comment.repository.MovieCommentRepository;
import com.ptithcm.movie.movie.repository.MovieRepository;
import com.ptithcm.movie.movie.repository.ReviewRepository;
import com.ptithcm.movie.user.dto.UserProfileResponse;
import com.ptithcm.movie.user.dto.request.*;
import com.ptithcm.movie.user.dto.response.UserResponse;
import com.ptithcm.movie.user.entity.Role;
import com.ptithcm.movie.user.entity.User;
import com.ptithcm.movie.user.repository.RoleRepository;
import com.ptithcm.movie.user.repository.UserRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepo;
    private final CloudinaryService cloudinaryService;
    private final PasswordEncoder passwordEncoder;
    private final ReviewRepository reviewRepository;
    private final MovieCommentRepository commentRepository;
    private final MovieRepository movieRepository;
    private final RoleRepository roleRepository;

    @Transactional
    public ServiceResult createAdmin(CreateAdminRequest request) {

        if (userRepo.existsByEmailIgnoreCase(request.getEmail())) {
            return ServiceResult.Failure().code(400).message("Email already exists");
        }

        Role role = roleRepository.findByCode(request.getRoleCode())
                .orElseThrow(() -> new RuntimeException("Role not found"));

        String avatarUrl = null;
        if (request.getAvatar() != null && !request.getAvatar().isEmpty()) {
            try {
                avatarUrl = cloudinaryService.uploadImage(request.getAvatar());
            } catch (IOException e) {
                return ServiceResult.Failure().code(500).message("Failed to upload avatar");
            }
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .isActive(true)
                .emailVerified(true)
                .isImported(false)
                .avatarUrl(avatarUrl)
                .build();

        User savedUser = userRepo.save(user);

        UserResponse response = UserResponse.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .avatarUrl(savedUser.getAvatarUrl())
                .roleCode(savedUser.getRole().getCode())
                .isActive(savedUser.isActive())
                .emailVerified(savedUser.isEmailVerified())
                .createdAt(savedUser.getCreatedAt())
                .build();

        return ServiceResult.Success()
                .code(ErrorCode.SUCCESS)
                .message("Admin created successfully")
                .data(response);
    }

    public ServiceResult getCurrentUser(UserPrincipal principal) {

        if (principal == null)
            return ServiceResult.Failure()
                    .code(ErrorCode.UNAUTHORIZED)
                    .message("Không tìm thấy người dùng");

        User user = userRepo.findById(principal.getUser().getId())
                .orElse(principal.getUser());

        UserProfileResponse payload = new UserProfileResponse(user);

        return ServiceResult.Success()
                .code(ErrorCode.SUCCESS)
                .data(payload);
    }

    @Transactional
    public ServiceResult updateProfile(UUID userId, UserUpdateRequest request) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (StringUtils.hasText(request.getUsername())) {
            String newUsername = request.getUsername().trim();

            if (userRepo.existsByUsernameAndIdNot(newUsername, userId)) {
                return ServiceResult.Failure().code(ErrorCode.FAILED).message("Username is already taken");
            }
            user.setUsername(newUsername);
        }

        if (request.getAvatar() != null && !request.getAvatar().isEmpty()) {
            try {
                String avatarUrl = cloudinaryService.uploadImage(request.getAvatar());
                user.setAvatarUrl(avatarUrl);
            } catch (IOException e) {
                return ServiceResult.Failure().code(ErrorCode.FAILED).message("Error uploading avatar");
            }
        }

        User updatedUser = userRepo.save(user);
        return ServiceResult.Success().code(ErrorCode.SUCCESS).data(updatedUser).message("Profile updated successfully");
    }

    @Transactional
    public ServiceResult changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            return ServiceResult.Failure().code(ErrorCode.FAILED).message("Incorrect old password");
        }

        if (request.getOldPassword().equals(request.getNewPassword())) {
            return ServiceResult.Failure().code(ErrorCode.FAILED).message("New password cannot be the same as old password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepo.save(user);

        return ServiceResult.Success().code(ErrorCode.SUCCESS).message("Password changed successfully");
    }

    @Transactional
    public ServiceResult updateUserStatus(UUID id, UserStatusRequest request) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setActive(request.getActive());
        User savedUser = userRepo.save(user);

        UserResponse response = UserResponse.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .avatarUrl(savedUser.getAvatarUrl())
                .roleCode(savedUser.getRole().getCode())
                .emailVerified(savedUser.isEmailVerified())
                .isActive(savedUser.isActive())
                .isImported(savedUser.isImported())
                .createdAt(savedUser.getCreatedAt())
                .build();

        String action = request.getActive() ? "activated" : "deactivated";
        return ServiceResult.Success()
                .message("User has been " + action)
                .data(response);
    }

    public ServiceResult searchViewers(UserSearchRequest request, Pageable pageable) {
        Specification<User> spec = createViewerSpec(request);

        Page<User> userPage = userRepo.findAll(spec, pageable);

        List<UUID> userIds = userPage.getContent().stream()
                .map(User::getId)
                .toList();

        Map<UUID, Long> reviewCounts = new HashMap<>();
        Map<UUID, Long> commentCounts = new HashMap<>();

        if (!userIds.isEmpty()) {
            List<Object[]> rCounts = reviewRepository.countMoviesReviewedByUserIds(userIds);
            for (Object[] row : rCounts) {
                reviewCounts.put((UUID) row[0], (Long) row[1]);
            }

            List<Object[]> cCounts = commentRepository.countMoviesCommentedByUserIds(userIds);
            for (Object[] row : cCounts) {
                commentCounts.put((UUID) row[0], (Long) row[1]);
            }
        }

        Page<UserResponse> responsePage = userPage.map(user -> UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .roleCode(user.getRole().getCode()) // Lấy code role
                .emailVerified(user.isEmailVerified())
                .isActive(user.isActive())
                .isImported(user.isImported())
                .createdAt(user.getCreatedAt())
                .reviewCount(reviewCounts.getOrDefault(user.getId(), 0L))
                .commentCount(commentCounts.getOrDefault(user.getId(), 0L))
                .build());

        return ServiceResult.Success()
                .message("Search viewers successfully")
                .data(responsePage);
    }

    private Specification<User> createViewerSpec(UserSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<User, Role> roleJoin = root.join("role", JoinType.INNER);
            predicates.add(cb.equal(roleJoin.get("code"), "viewer"));

            if (StringUtils.hasText(request.getQuery())) {
                String searchKey = "%" + request.getQuery().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")), searchKey),
                        cb.like(cb.lower(root.get("email")), searchKey)
                ));
            }

            if (request.getIsActive() != null) {
                predicates.add(cb.equal(root.get("isActive"), request.getIsActive()));
            }

            if (request.getEmailVerified() != null) {
                predicates.add(cb.equal(root.get("emailVerified"), request.getEmailVerified()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public ServiceResult searchAdmins(AdminSearchRequest request, Pageable pageable) {
        Specification<User> spec = createAdminSpec(request);
        Page<User> userPage = userRepo.findAll(spec, pageable);

        List<UUID> userIds = userPage.getContent().stream()
                .map(User::getId)
                .toList();

        Map<UUID, Long> createdCounts = new HashMap<>();
        Map<UUID, Long> updatedCounts = new HashMap<>();
        Map<UUID, Long> commentCounts = new HashMap<>();

        if (!userIds.isEmpty()) {
            List<Object[]> cMovies = movieRepository.countMoviesCreatedByUserIds(userIds);
            for (Object[] row : cMovies) createdCounts.put((UUID) row[0], (Long) row[1]);

            List<Object[]> uMovies = movieRepository.countMoviesUpdatedByUserIds(userIds);
            for (Object[] row : uMovies) updatedCounts.put((UUID) row[0], (Long) row[1]);

            List<Object[]> comments = commentRepository.countCommentsByUserIds(userIds);
            for (Object[] row : comments) commentCounts.put((UUID) row[0], (Long) row[1]);
        }

        Page<UserResponse> responsePage = userPage.map(user -> UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .roleCode(user.getRole().getCode())
                .emailVerified(user.isEmailVerified())
                .isActive(user.isActive())
                .createdAt(user.getCreatedAt())
                .createdMovieCount(createdCounts.getOrDefault(user.getId(), 0L))
                .updatedMovieCount(updatedCounts.getOrDefault(user.getId(), 0L))
                .adminCommentCount(commentCounts.getOrDefault(user.getId(), 0L))
                .build());

        return ServiceResult.Success()
                .message("Search admins successfully")
                .data(responsePage);
    }

    private Specification<User> createAdminSpec(AdminSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<User, Role> roleJoin = root.join("role", JoinType.INNER);

            if (StringUtils.hasText(request.getRoleCode())) {
                predicates.add(cb.equal(roleJoin.get("code"), request.getRoleCode()));
            } else {
                predicates.add(roleJoin.get("code").in(Arrays.asList("movie_admin", "comment_admin")));
            }

            if (StringUtils.hasText(request.getQuery())) {
                String searchKey = "%" + request.getQuery().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")), searchKey),
                        cb.like(cb.lower(root.get("email")), searchKey)
                ));
            }

            if (request.getIsActive() != null) {
                predicates.add(cb.equal(root.get("isActive"), request.getIsActive()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Transactional
    public ServiceResult updateAdmin(UUID id, UpdateAdminRequest request) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if ("super_admin".equals(user.getRole().getCode())) {
            return ServiceResult.Failure().code(ErrorCode.BAD_CREDENTIALS).message("Cannot modify SUPER_ADMIN");
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String newEmail = request.getEmail().trim();

            if (!newEmail.equalsIgnoreCase(user.getEmail())) {
                if (userRepo.existsByEmailAndIdNot(newEmail, id)) {
                    return ServiceResult.Failure()
                            .code(ErrorCode.BAD_CREDENTIALS)
                            .message("Email '" + newEmail + "' is already taken by another user");
                }
                user.setEmail(newEmail);
            }
        }

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            String newName = request.getUsername().trim();
            user.setUsername(newName);
        }

        if (request.getAvatar() != null && !request.getAvatar().isEmpty()) {
            try {
                String newAvatarUrl = cloudinaryService.uploadImage(request.getAvatar());
                user.setAvatarUrl(newAvatarUrl);
            } catch (IOException e) {
                return ServiceResult.Failure().message("Failed to upload avatar");
            }
        }

        if (request.getRoleCode() != null) {
            Role newRole = roleRepository.findByCode(request.getRoleCode())
                    .orElseThrow(() -> new RuntimeException("Role not found"));
            user.setRole(newRole);
        }

        if (request.getIsActive() != null) {
            user.setActive(request.getIsActive());
        }

        User savedUser = userRepo.save(user);

        UserResponse response = UserResponse.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .avatarUrl(savedUser.getAvatarUrl())
                .roleCode(savedUser.getRole().getCode())
                .isActive(savedUser.isActive())
                .emailVerified(savedUser.isEmailVerified())
                .createdAt(savedUser.getCreatedAt())
                .build();

        return ServiceResult.Success().code(ErrorCode.SUCCESS).message("Admin updated successfully").data(response);
    }

    @Transactional
    public ServiceResult resetAdminPassword(UUID id, String newPassword) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));

        userRepo.save(user);

        return ServiceResult.Success()
                .message("Password reset successfully");
    }

    @Transactional
    public ServiceResult deleteAdmin(UUID adminId, UserPrincipal userPrincipal) {
        User admin = userRepo.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if (GlobalConstant.SUPER_ADMIN.equals(admin.getRole().getCode())) {
            return ServiceResult.Failure()
                    .code(ErrorCode.BAD_CREDENTIALS)
                    .message("Cannot delete SUPER_ADMIN account");
        }

        ServiceResult result = getCurrentUser(userPrincipal);
        UserProfileResponse u = (UserProfileResponse) result.getData();

        if (u.getId().equals(adminId.toString())) {
            return ServiceResult.Failure()
                    .code(ErrorCode.BAD_CREDENTIALS)
                    .message("You cannot delete your own account");
        }

        if (movieRepository.existsByCreatedBy_Id(adminId)) {
            return ServiceResult.Failure()
                    .code(ErrorCode.BAD_CREDENTIALS)
                    .message("Cannot delete this admin. They have created movies. Please deactivate instead.");
        }

        if (movieRepository.existsByUpdatedBy_Id(adminId)) {
            return ServiceResult.Failure()
                    .code(ErrorCode.BAD_CREDENTIALS)
                    .message("Cannot delete this admin. They have updated movies. Please deactivate instead.");
        }

        if (commentRepository.existsByUser_Id(adminId)) {
            return ServiceResult.Failure()
                    .code(ErrorCode.BAD_CREDENTIALS)
                    .message("Cannot delete this admin. They have posted comments.");
        }

        if (admin.getAvatarUrl() != null) {
            cloudinaryService.deleteImage(admin.getAvatarUrl());
        }

        userRepo.delete(admin);

        return ServiceResult.Success().message("Admin deleted successfully");
    }

    @Transactional
    public ServiceResult updateUserProfile(UserProfileUpdateRequest request) {
        User currentUser = userRepo.findById(request.getId())
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if(currentUser.getId() == null){
            return ServiceResult.Failure().code(ErrorCode.BAD_CREDENTIALS).message("Can't find user");
        }


        if (request.getAvatar() != null && !request.getAvatar().isEmpty()) {
            try {
                String avatarUrl = cloudinaryService.uploadImage(request.getAvatar());
                currentUser.setAvatarUrl(avatarUrl);
            } catch (IOException e) {
                return ServiceResult.Failure().message("Failed to upload avatar");
            }
        }
        currentUser.setUsername(request.getUsername());

        User savedUser = userRepo.save(currentUser);

        UserResponse response = UserResponse.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .avatarUrl(savedUser.getAvatarUrl())
                .roleCode(savedUser.getRole().getCode())
                .isActive(savedUser.isActive())
                .emailVerified(savedUser.isEmailVerified())
                .createdAt(savedUser.getCreatedAt())
                .build();

        return ServiceResult.Success()
                .message("Profile updated successfully")
                .data(response);
    }

    @Transactional
    public ServiceResult changePasswordUserProfile(ChangePasswordProfileRequest request) {
        User currentUser = userRepo.findById(request.getId())
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if(currentUser.getId() == null){
            return ServiceResult.Failure().code(ErrorCode.BAD_CREDENTIALS).message("Can't find user");
        }

        if ( currentUser.getPasswordHash() != null && !passwordEncoder.matches(request.getCurrentPw(), currentUser.getPasswordHash())) {
            return ServiceResult.Failure().code(400).message("Incorrect current password");
        }

        if (request.getCurrentPw().equals(request.getNewPw())) {
            return ServiceResult.Failure().code(400).message("New password must be different from current password");
        }

        currentUser.setPasswordHash(passwordEncoder.encode(request.getNewPw()));
        userRepo.save(currentUser);

        return ServiceResult.Success().message("Password changed successfully");
    }
}