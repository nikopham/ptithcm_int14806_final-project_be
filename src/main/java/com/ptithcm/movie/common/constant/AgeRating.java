package com.ptithcm.movie.common.constant;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AgeRating {
    P("P", "All ages"),
    K("K", "Under 13 years old (with parental supervision)"),
    T13("T13", "13 years and older"),
    T16("T16", "16 years and older"),
    T18("T18", "18 years and older");

    private final String code;
    private final String description;

    AgeRating(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * @JsonValue báo cho Jackson chỉ sử dụng "code" (ví dụ: "T13")
     * khi chuyển đổi (serialize) enum này sang JSON.
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
