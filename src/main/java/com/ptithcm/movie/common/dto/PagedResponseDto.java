package com.ptithcm.movie.common.dto;

import lombok.Data;
import org.springframework.data.domain.Page;
import java.util.List;

@Data
public class PagedResponseDto<T> {

    private List<T> content; // Danh sách item (ví dụ: MovieItemDto)
    private int page;        // Trang hiện tại (số)
    private int size;        // Kích thước trang
    private long totalElements; // Tổng số item
    private int totalPages;    // Tổng số trang
    private boolean last;      // Có phải trang cuối không

    /**
     * Constructor để "biến đổi" từ Page<Entity> của Spring Data
     */
    public PagedResponseDto(Page<T> page) {
        this.content = page.getContent();
        this.page = page.getNumber();
        this.size = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.last = page.isLast();
    }
}
