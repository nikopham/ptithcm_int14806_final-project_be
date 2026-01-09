package com.ptithcm.movie.comment.service;

import com.ptithcm.movie.auth.security.UserPrincipal;
import com.ptithcm.movie.comment.dto.request.CommentRequest;
import com.ptithcm.movie.comment.dto.request.CommentSearchRequest;
import com.ptithcm.movie.comment.dto.response.CommentResponse;
import com.ptithcm.movie.comment.dto.response.CommentSearchResponse;
import com.ptithcm.movie.comment.entity.MovieComment;
import com.ptithcm.movie.comment.repository.MovieCommentRepository;
import com.ptithcm.movie.common.constant.ErrorCode;
import com.ptithcm.movie.common.dto.ServiceResult;
import com.ptithcm.movie.external.smart.ToxicCheckResponse;
import com.ptithcm.movie.movie.entity.Movie;
import com.ptithcm.movie.movie.entity.ViewingHistory;
import com.ptithcm.movie.movie.repository.MovieRepository;
import com.ptithcm.movie.movie.repository.ViewingHistoryRepository;
import com.ptithcm.movie.user.entity.Role;
import com.ptithcm.movie.user.entity.User;
import com.ptithcm.movie.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ptithcm.movie.external.smart.ContentModerationService;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final MovieCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final ViewingHistoryRepository historyRepository;
    private final ContentModerationService moderationService;

    @Transactional(readOnly = true)
    public ServiceResult getCommentsByMovie(UUID movieId, Pageable pageable) {
        Page<MovieComment> commentPage = commentRepository.findRootCommentsByMovieId(movieId, pageable);

        List<UUID> parentIds = commentPage.getContent().stream()
                .map(MovieComment::getId)
                .toList();

        Map<UUID, Long> replyCounts = new HashMap<>();
        if (!parentIds.isEmpty()) {
            List<Object[]> counts = commentRepository.countRepliesByParentIds(parentIds);
            for (Object[] row : counts) {
                replyCounts.put((UUID) row[0], (Long) row[1]);
            }
        }

        Page<CommentResponse> response = commentPage.map(c -> CommentResponse.builder()
                .id(c.getId())
                .body(c.getBody())
                .createdAt(c.getCreatedAt())
                .isEdited(c.isEdited())
                .parentId(c.getParent() != null ? c.getParent().getId() : null)
                // User Info
                .userId(c.getUser().getId())
                .username(c.getUser().getUsername())
                .userAvatar(c.getUser().getAvatarUrl())
                .userRole(c.getUser().getRole() != null ? c.getUser().getRole().getCode() : "viewer")
                // Reply Count
                .replyCount(replyCounts.getOrDefault(c.getId(), 0L))
                .build());

        return ServiceResult.Success()
                .message("Lấy danh sách bình luận thành công")
                .data(response);
    }

    // New: search comments across criteria
    @Transactional(readOnly = true)
    public ServiceResult searchComments(CommentSearchRequest request, Pageable pageable) {
        Specification<MovieComment> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (request.getUserId() != null) {
                predicates.add(cb.equal(root.get("user").get("id"), request.getUserId()));
            }
            if (request.getMovieId() != null) {
                predicates.add(cb.equal(root.get("movie").get("id"), request.getMovieId()));
            }
            if (request.getIsHidden() != null) {
                predicates.add(cb.equal(root.get("isHidden"), request.getIsHidden()));
            }
            if (request.getMovieTitle() != null && !request.getMovieTitle().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("movie").get("title")), "%" + request.getMovieTitle().toLowerCase() + "%"));
            }
            if (request.getFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), request.getFrom()));
            }
            if (request.getTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), request.getTo()));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Page<MovieComment> page = commentRepository.findAll(spec, sortedPageable);

        // Batch reply counts for returned page
        List<UUID> parentIds = page.getContent().stream().map(MovieComment::getId).toList();
        Map<UUID, Long> replyCounts = new HashMap<>();
        if (!parentIds.isEmpty()) {
            for (Object[] row : commentRepository.countRepliesByParentIds(parentIds)) {
                replyCounts.put((UUID) row[0], (Long) row[1]);
            }
        }




        Page<CommentSearchResponse> responsePage = page.map(c -> CommentSearchResponse.builder()
                .id(c.getId())
                .body(c.getBody())
                .createdAt(c.getCreatedAt())
                .isEdited(c.isEdited())
                .isHidden(c.isHidden())
                .parentId(c.getParent() != null ? c.getParent().getId() : null)
                .userId(c.getUser().getId())
                .username(c.getUser().getUsername())
                .userAvatar(c.getUser().getAvatarUrl())
                .userRole(c.getUser().getRole() != null ? c.getUser().getRole().getCode() : "viewer")
                .replyCount(replyCounts.getOrDefault(c.getId(), 0L))
                .movie(CommentSearchResponse.MovieInfo.builder()
                        .id(c.getMovie().getId())
                        .title(c.getMovie().getTitle())
                        .originalTitle(c.getMovie().getOriginalTitle())
                        .slug(c.getMovie().getSlug())
                        .posterUrl(c.getMovie().getPosterUrl())
                        .backdropUrl(c.getMovie().getBackdropUrl())
                        .releaseYear(c.getMovie().getReleaseDate() != null ? c.getMovie().getReleaseDate().getYear() : null)
                        .isSeries(c.getMovie().isSeries())
                        .build())
                .build());


        return ServiceResult.Success()
                .code(ErrorCode.SUCCESS)
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
    public ServiceResult createComment(CommentRequest request) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return ServiceResult.Failure().code(401).message("Chưa đăng nhập");
        ToxicCheckResponse aiResult = moderationService.analyzeContent(request.getBody());
        boolean isToxic = aiResult.getIsToxic();

        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new RuntimeException("Movie not found"));

        MovieComment comment = MovieComment.builder()
                .user(currentUser)
                .movie(movie)
                .body(request.getBody())
                .isEdited(false)
                .isHidden(false)
                .build();
        if (isToxic) {
            comment.setHidden(true);
        } else {
            //
        }
        if (aiResult.getConfidence() != null) {
            comment.setSentimentScore(BigDecimal.valueOf(aiResult.getConfidence()));
        } else {
            comment.setSentimentScore(BigDecimal.ZERO);
        }


        if (request.getParentId() != null) {
            MovieComment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Bình luận đang trả lời không tồn tại"));

            if (!parent.getMovie().getId().equals(movie.getId())) {
                return ServiceResult.Failure().code(400).message("Bình luận đang trả lời không hợp lệ");
            }

            if (request.getParentId() != null) {
                MovieComment parentNode = commentRepository.findById(request.getParentId())
                        .orElseThrow(() -> new RuntimeException("Bình luận đang trả lời không tồn tại"));

                if (!parentNode.getMovie().getId().equals(movie.getId())) {
                    return ServiceResult.Failure().code(400).message("Bình luận đang trả lời không hợp lệ");
                }

                if (parentNode.getParent() == null) {

                    comment.setParent(parentNode);
                } else {
                    comment.setParent(parentNode.getParent());
                    String newBody = "@" + parentNode.getUser().getUsername() + " " + request.getBody();
                    comment.setBody(newBody);
                }
            }

            comment.setParent(parent);
        }

        MovieComment savedComment = commentRepository.save(comment);
        if (isToxic) {
            return ServiceResult.Failure()
                    .code(ErrorCode.FAILED)
                    .message(String.format(
                            "Nhận diện bình luận có nội dung không phù hợp. Bình luận của bạn đã được kiểm duyệt và không được hiển thị"
                    ));

        }
        return ServiceResult.Success()
                .message("Tạo bình luận thành công")
                .data(mapToCommentResponse(savedComment));
    }

    @Transactional
    public ServiceResult editComment(UUID commentId, CommentRequest request) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return ServiceResult.Failure().code(401).message("Chưa đăng nhập");

        MovieComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bình luận"));

        if (!comment.getUser().getId().equals(currentUser.getId())) {
            return ServiceResult.Failure().code(403).message("Bạn chỉ có thể chỉnh sửa bình luận của chính mình");
        }

        comment.setBody(request.getBody());
        comment.setEdited(true);

        MovieComment updatedComment = commentRepository.save(comment);

        return ServiceResult.Success()
                .message("Câp nhật bình luận thành công")
                .data(mapToCommentResponse(updatedComment));
    }

    // New: toggle isHidden for a comment (admin or owner)
    @Transactional
    public ServiceResult toggleHidden(UUID commentId) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return ServiceResult.Failure().code(ErrorCode.UNAUTHORIZED).message("Chưa đăng nhập");

        MovieComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bình luận"));

        boolean isOwner = comment.getUser().getId().equals(currentUser.getId());
        Role role = currentUser.getRole();
        boolean isAdmin = role != null && (role.getCode().equals("super_admin") || role.getCode().equals("comment_admin"));

        if (!isOwner && !isAdmin) {
            return ServiceResult.Failure().code(ErrorCode.FORBIDDEN).message("Bạn không có quyền thay đổi trạng thái bình luận này");
        }

        comment.setHidden(!comment.isHidden());
        commentRepository.save(comment);

        Map<String, Object> data = new HashMap<>();
        data.put("id", comment.getId());
        data.put("isHidden", comment.isHidden());

        return ServiceResult.Success().message("Cập nhật trạng thái bình luận").data(data);
    }

    private CommentResponse mapToCommentResponse(MovieComment c) {
        return CommentResponse.builder()
                .id(c.getId())
                .body(c.getBody())
                .createdAt(c.getCreatedAt())
                .isEdited(c.isEdited())
                .parentId(c.getParent() != null ? c.getParent().getId() : null)
                .userId(c.getUser().getId())
                .username(c.getUser().getUsername())
                .userAvatar(c.getUser().getAvatarUrl())
                .replyCount(0L)
                .build();
    }

    private void analyzeAndSetToxicStatus(MovieComment comment) {
        // Gọi API AI Server (Python) hoặc thư viện
        // double score = aiService.analyze(comment.getBody());
        // comment.setSentimentScore(BigDecimal.valueOf(score));
        // if (score < -0.8) { comment.setIsToxic(true); comment.setIsHidden(true); }
    }
}