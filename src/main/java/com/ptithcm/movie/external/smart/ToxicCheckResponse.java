package com.ptithcm.movie.external.smart;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToxicCheckResponse {
    @JsonProperty("is_toxic")
    private Boolean isToxic;

    private Double confidence;
}
