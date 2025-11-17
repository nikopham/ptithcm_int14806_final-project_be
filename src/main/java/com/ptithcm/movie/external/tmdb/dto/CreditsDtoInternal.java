package com.ptithcm.movie.external.tmdb.dto;

import java.util.List;

record CreditsDtoInternal(List<CastMemberDto> cast, List<CrewMemberDto> crew) {};
