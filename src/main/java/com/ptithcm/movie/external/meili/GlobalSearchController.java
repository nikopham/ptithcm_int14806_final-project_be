package com.ptithcm.movie.external.meili;

import com.ptithcm.movie.common.dto.ServiceResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class GlobalSearchController {

    private final SearchService searchService;

    @GetMapping("/fast")
    public ResponseEntity<ServiceResult> searchFast(@RequestParam String query) {
        return ResponseEntity.ok(searchService.searchMulti(query));
    }

    // API Admin: Chạy lại index cho toàn bộ dữ liệu (Dùng khi mới triển khai hoặc reset DB)
    @PostMapping("/sync-all")
    public ResponseEntity<String> syncAllData() {
        // Logic: Load all movies/people từ DB -> Loop -> gọi indexMovie/indexPerson
        searchService.syncAllData();
        return ResponseEntity.ok("Sync process started...");
    }
}
