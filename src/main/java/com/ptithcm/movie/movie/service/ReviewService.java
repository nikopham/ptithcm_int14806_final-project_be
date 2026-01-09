package com.ptithcm.movie.movie.service;

import com.ptithcm.movie.auth.security.UserPrincipal;
import com.ptithcm.movie.comment.entity.MovieComment;
import com.ptithcm.movie.common.constant.ErrorCode;
import com.ptithcm.movie.common.dto.ServiceResult;
import com.ptithcm.movie.movie.dto.request.ReviewRequest;
import com.ptithcm.movie.movie.dto.request.ReviewSearchRequest;
import com.ptithcm.movie.movie.dto.response.ReviewResponse;
import com.ptithcm.movie.movie.entity.*;
import com.ptithcm.movie.movie.repository.MovieLikeRepository;
import com.ptithcm.movie.movie.repository.MovieRepository;
import com.ptithcm.movie.movie.repository.ReviewRepository;
import com.ptithcm.movie.movie.repository.ViewingHistoryRepository;
import com.ptithcm.movie.user.entity.Role;
import com.ptithcm.movie.user.entity.User;
import com.ptithcm.movie.user.repository.UserRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;
    private final MovieLikeRepository movieLikeRepository;
    private final ViewingHistoryRepository historyRepository;

    public ServiceResult searchReviews(ReviewSearchRequest request, Pageable pageable) {
        Specification<Review> spec = createReviewSpec(request);

        Page<Review> reviewPage = reviewRepository.findAll(spec, pageable);

        Page<ReviewResponse> responsePage = reviewPage.map(review -> {
            return ReviewResponse.builder()
                    .id(review.getId())
                    .movieId(review.getMovie().getId())
                    .movieTitle(review.getMovie().getTitle())
                    .moviePosterUrl(review.getMovie().getPosterUrl())
                    .userId(review.getUser().getId())
                    .username(review.getUser().getUsername())
                    .userAvatar(review.getUser().getAvatarUrl())
                    .rating(review.getRating())
                    .title(review.getTitle())
                    .body(review.getBody())
                    .isHidden(review.getIsHidden())
                    .createdAt(review.getCreatedAt())
                    .build();
        });

        return ServiceResult.Success().code(ErrorCode.SUCCESS).data(responsePage);
    }

    private Specification<Review> createReviewSpec(ReviewSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (Long.class != query.getResultType()) {
                root.fetch("movie", JoinType.LEFT);
                root.fetch("user", JoinType.LEFT);
            }

            if (StringUtils.hasText(request.getQuery())) {
                String searchKey = "%" + request.getQuery().toLowerCase() + "%";
                Join<Review, Movie> movieJoin = root.join("movie", JoinType.LEFT);
                Join<Review, User> userJoin = root.join("user", JoinType.LEFT);

                predicates.add(cb.or(
                        cb.like(cb.lower(movieJoin.get("title")), searchKey),
                        cb.like(cb.lower(userJoin.get("username")), searchKey)
                ));
            }

            if (request.getRating() != null) {
                int star = request.getRating();

                Predicate ge = cb.greaterThanOrEqualTo(
                        root.get("rating"),
                        BigDecimal.valueOf(star)
                );

                Predicate lt = cb.lessThan(
                        root.get("rating"),
                        BigDecimal.valueOf(star + 1)
                );

                predicates.add(cb.and(ge, lt));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public ServiceResult getReviewsByMovie(UUID movieId, Pageable pageable) {
        if (!movieRepository.existsById(movieId)) {
            return ServiceResult.Failure().code(404).message("Không tìm thấy phim" );
        }


        Page<Review> reviewPage = reviewRepository.findAllByMovieId(movieId, pageable);

        Page<ReviewResponse> responsePage = reviewPage.map(r -> ReviewResponse.builder()
                .id(r.getId())
                .rating(r.getRating())
                .title(r.getTitle())
                .body(r.getBody())
                .createdAt(r.getCreatedAt())
                .userId(r.getUser().getId())
                .isHidden(r.getIsHidden())
                .username(r.getUser().getUsername())
                .userAvatar(r.getUser().getAvatarUrl())
                .build());

        return ServiceResult.Success()
                .message("Lấy danh sách đánh giá thành công")
                .data(responsePage);
    }


    private User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        UUID userId = null;

        if (principal instanceof UserPrincipal userPrincipal) {
            User u = userPrincipal.getUser();
            userId = u.getId();
        }

        if (userId != null) {
            return userRepository.findById(userId).orElse(null);
        }
        return null;
    }

    @Transactional
    public ServiceResult createReview(ReviewRequest request) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return ServiceResult.Failure().code(401).message("Chưa đăng nhập");
        ViewingHistory history = historyRepository
                .findByUserIdAndMovieIdAndEpisodeId(currentUser.getId(), request.getMovieId(), request.getEpisodeId())
                .orElse(null);

        double requiredProgress = 0.70;

        if (history == null ||
                (double) history.getAccumulatedSeconds() / history.getTotalSeconds() < requiredProgress) {

            return ServiceResult.Failure().code(ErrorCode.FAILED).message("Bạn chưa xem đủ 70% phim để có thể đánh giá.");
        }
        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new RuntimeException("Phim không tồn tại"));

        if (reviewRepository.existsByUserIdAndMovieId(currentUser.getId(), movie.getId())) {
            return ServiceResult.Failure().code(400).message("Bạn đã đánh giá phim này rồi");
        }

        Review review = Review.builder()
                .user(currentUser)
                .movie(movie)
                .rating(request.getRating())
                .title(request.getTitle())
                .body(request.getBody())
                .build();

        Review savedReview = reviewRepository.save(review);

        updateMovieStats(movie.getId());

        return ServiceResult.Success()
                .message("Tạo đánh giá thành công")
                .data(mapToReviewResponse(savedReview));
    }

    @Transactional
    public ServiceResult updateReview(UUID reviewId, ReviewRequest request) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return ServiceResult.Failure().code(401).message("Chưa đăng nhập");

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá"));

        if (!review.getUser().getId().equals(currentUser.getId())) {
            return ServiceResult.Failure().code(403).message("Bạn chỉ có thể cập nhật đánh giá của chính mình");
        }

        review.setRating(request.getRating());
        review.setTitle(request.getTitle());
        review.setBody(request.getBody());

        Review updatedReview = reviewRepository.save(review);

        updateMovieStats(review.getMovie().getId());

        return ServiceResult.Success()
                .message("Cập nhật đánh giá thành công")
                .data(mapToReviewResponse(updatedReview));
    }

    private void updateMovieStats(UUID movieId) {
        Movie movie = movieRepository.findById(movieId).orElseThrow();

        Double avgRating = reviewRepository.getAverageRatingByMovieId(movieId);
        long count = reviewRepository.countByMovieId(movieId);

        movie.setAverageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0);
        movie.setReviewCount((int) count);

        movieRepository.save(movie);
    }

    private ReviewResponse mapToReviewResponse(Review r) {
        return ReviewResponse.builder()
                .id(r.getId())
                .movieId(r.getMovie().getId())
                .movieTitle(r.getMovie().getTitle())
                .userId(r.getUser().getId())
                .username(r.getUser().getUsername())
                .userAvatar(r.getUser().getAvatarUrl())
                .rating(r.getRating())
                .title(r.getTitle())
                .body(r.getBody())
                .createdAt(r.getCreatedAt())
                .build();
    }

    @Transactional
    public ServiceResult deleteReview(UUID id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá"));

        reviewRepository.delete(review);

        reviewRepository.flush();

        return ServiceResult.Success().code(ErrorCode.SUCCESS)
                .message("Xóa đánh giá thành công");
    }

    @Transactional
    public ServiceResult toggleHidden(UUID id) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return ServiceResult.Failure().code(ErrorCode.UNAUTHORIZED).message("Chưa đăng nhập");

        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá"));

        boolean isOwner = review.getUser().getId().equals(currentUser.getId());
        Role role = currentUser.getRole();
        boolean isAdmin = role != null && (role.getCode().equals("super_admin") || role.getCode().equals("comment_admin"));

        if (!isOwner && !isAdmin) {
            return ServiceResult.Failure().code(ErrorCode.FORBIDDEN).message("Bạn không có quyền thay đổi trạng thái đánh giá này");
        }

        review.setIsHidden(!review.getIsHidden());
        reviewRepository.save(review);

        Map<String, Object> data = new HashMap<>();
        data.put("id", review.getId());
        data.put("isHidden", review.getIsHidden());

        return ServiceResult.Success().message("Cập nhật trạng thái đánh giá").data(data);
    }
}
