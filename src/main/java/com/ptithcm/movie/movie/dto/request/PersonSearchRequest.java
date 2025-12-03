package com.ptithcm.movie.movie.dto.request;

import com.ptithcm.movie.common.constant.PersonJob;
import lombok.Data;

@Data
public class PersonSearchRequest {
    private String query;
    private PersonJob job;
}