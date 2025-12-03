package com.ptithcm.movie.movie.dto.request;

import com.ptithcm.movie.common.constant.AgeRating;
import com.ptithcm.movie.common.constant.MovieStatus;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Data
public class MovieUpdateRequest {
    private String title;
    private String originalTitle;
    private String description;

    private Integer releaseYear;
    private Integer durationMin;

    private AgeRating ageRating;
    private MovieStatus status;

    private String videoUrl;

    // Relationships (Gửi list rỗng để xóa hết, gửi null để giữ nguyên, gửi list có ID để thay thế)
    private List<Integer> countryIds;
    private List<Integer> genreIds;

    private UUID directorId;
    private List<UUID> actorIds;

    // File ảnh (Optional - chỉ gửi khi muốn thay ảnh)
    private MultipartFile posterImage;
    private MultipartFile backdropImage;
}
