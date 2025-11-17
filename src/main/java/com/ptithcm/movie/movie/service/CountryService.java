package com.ptithcm.movie.movie.service;

import com.ptithcm.movie.common.dto.ServiceResult;
import com.ptithcm.movie.movie.entity.Country;
import com.ptithcm.movie.movie.repository.CountryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CountryService {
    private final CountryRepository countryRepository;
    /**
     * Logic nghiệp vụ để lấy tất cả Countries
     */
    public ServiceResult getAllCountries() {
        try {
            List<Country> countries = countryRepository.findAll();
            return ServiceResult.Success()
                    .message("Countries fetched successfully")
                    .data(countries); // <-- 2. Đặt data vào
        } catch (Exception e) {
            return ServiceResult.Failure()
                    .message("Failed to fetch countries: " + e.getMessage());
        }
    }
}

