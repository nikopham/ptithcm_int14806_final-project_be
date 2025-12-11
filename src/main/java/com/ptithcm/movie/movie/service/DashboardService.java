package com.ptithcm.movie.movie.service;

import com.ptithcm.movie.comment.repository.MovieCommentRepository;
import com.ptithcm.movie.movie.dto.response.DashboardOverviewResponse;
import com.ptithcm.movie.movie.dto.response.GenreStatResponse;
import com.ptithcm.movie.movie.dto.response.MonthlyViewStat;
import com.ptithcm.movie.movie.repository.GenreRepository;
import com.ptithcm.movie.movie.repository.MovieRepository;
import com.ptithcm.movie.movie.repository.ViewingHistoryRepository;
import com.ptithcm.movie.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepo;
    private final MovieRepository movieRepo;
    private final MovieCommentRepository commentRepo;
    private final ViewingHistoryRepository historyRepo;
    private final GenreRepository genreRepo;

    public DashboardOverviewResponse getOverview() {
        return DashboardOverviewResponse.builder()
                .totalUsers(userRepo.count())
                .totalMovies(movieRepo.count())
                .totalComments(commentRepo.count())
                .totalViews(movieRepo.sumTotalViews())
                .build();
    }

    public List<Long> getMonthlyViews(Integer year) {
        int targetYear = (year != null) ? year : LocalDate.now().getYear();

        List<MonthlyViewStat> rawStats = historyRepo.countViewsByMonth(targetYear);

        long[] fullYearData = new long[12];

        for (MonthlyViewStat stat : rawStats) {
            int index = stat.getMonth() - 1;
            if (index >= 0 && index < 12) {
                fullYearData[index] = stat.getViewCount();
            }
        }

        return Arrays.stream(fullYearData).boxed().toList();
    }

    public List<GenreStatResponse> getTopGenres() {
        return genreRepo.findTop5Genres(PageRequest.of(0, 5));
    }
}
