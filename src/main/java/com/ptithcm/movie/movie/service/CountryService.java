package com.ptithcm.movie.movie.service;

import com.ptithcm.movie.common.constant.ErrorCode;
import com.ptithcm.movie.common.dto.ServiceResult;
import com.ptithcm.movie.movie.dto.response.CountryResponse;
import com.ptithcm.movie.movie.entity.Country;
import com.ptithcm.movie.movie.repository.CountryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CountryService {

    private final CountryRepository countryRepository;

    public ServiceResult getPublishedCountries() {
        List<Country> countries = countryRepository.findAllWithPublishedMovies();
        List<CountryResponse> response = countries.stream()
                .map(c -> CountryResponse.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .isoCode(c.getIsoCode())
                        .build())
                .toList();
        return ServiceResult.Success().data(response);
    }

    public ServiceResult getAllCountries() {
        List<Country> countries = countryRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));

        List<CountryResponse> response = countries.stream()
                .map(c -> CountryResponse.builder()
                        .id(c.getId())
                        .isoCode(c.getIsoCode())
                        .name(c.getName())
                        .build())
                .toList();

        return ServiceResult.Success().code(ErrorCode.SUCCESS).data(response);
    }
}