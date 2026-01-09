package com.ptithcm.movie.external.smart;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserRatingDto { private String userId; private String movieId; private Double rating; }