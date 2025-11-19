package com.ptithcm.movie.movie.repository;

import com.ptithcm.movie.common.constant.MovieStatus;
import com.ptithcm.movie.movie.entity.Movie;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MovieRepository extends JpaRepository<Movie, UUID>,
        JpaSpecificationExecutor<Movie> {
    /**
     * Tạo một Specification để tìm title (không phân biệt hoa thường)
     * (Giả sử bạn đã có trigram index 'idx_movies_title_trgm')
     */
    static Specification<Movie> titleContains(String query) {
        if (query == null || query.isBlank()) {
            return null; // Bỏ qua nếu query rỗng
        }
        // (Đây là cách dùng ILIKE. Nếu muốn dùng Trigram, bạn cần @Query)
        return (root, cq, cb) ->
                cb.like(cb.lower(root.get("title")), "%" + query.toLowerCase() + "%");
    }

    /**
     * Tạo một Specification để lọc theo Status
     */
    static Specification<Movie> hasStatus(MovieStatus status) {
        if (status == null) {
            return null; // Bỏ qua nếu status là null (tương đương "all")
        }
        return (root, cq, cb) -> cb.equal(root.get("status"), status);
    }

    /**
     * Tạo một Specification để lọc theo Type (Movie/Series)
     */
    static Specification<Movie> isSeries(Boolean isSeries) {
        if (isSeries == null) {
            return null; // Bỏ qua nếu isSeries là null (tương đương "all")
        }
        return (root, cq, cb) -> cb.equal(root.get("isSeries"), isSeries);
    }

    @Query("SELECT m FROM Movie m " +
            "LEFT JOIN FETCH m.genres " +
            "LEFT JOIN FETCH m.countries " +
            "LEFT JOIN FETCH m.actors " +
            "LEFT JOIN FETCH m.directors " +
            "WHERE m.id = :id")
    Optional<Movie> findByIdWithDetails(@Param("id") UUID id);
}
