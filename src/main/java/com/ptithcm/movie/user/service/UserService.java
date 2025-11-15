package com.ptithcm.movie.user.service;


import com.ptithcm.movie.auth.security.UserPrincipal;
import com.ptithcm.movie.common.constant.ErrorCode;
import com.ptithcm.movie.common.dto.ServiceResult;
import com.ptithcm.movie.user.dto.UserProfileResponse;
import com.ptithcm.movie.user.entity.User;
import com.ptithcm.movie.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepo;

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
}