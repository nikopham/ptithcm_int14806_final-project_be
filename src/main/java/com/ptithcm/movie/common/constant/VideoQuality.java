package com.ptithcm.movie.common.constant;

public enum VideoQuality {

    FOUR_K("4K"),
    TWO_K("2K"),
    FHD("1080P"),
    HD("720P"),
    SD("480P"),      // Standard Definition
    LOW("360P");     // Low Resolution

    private final String displayName;

    VideoQuality(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}