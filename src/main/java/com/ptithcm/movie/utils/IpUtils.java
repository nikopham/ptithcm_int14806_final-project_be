package com.ptithcm.movie.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public class IpUtils {

    public static String getClientIp(HttpServletRequest request) {
        String ip = null;

        // 1. Ưu tiên Header của Cloudflare (Nếu bạn dùng Cloudflare CDN cho web chính)
        ip = request.getHeader("CF-Connecting-IP");

        // 2. Nếu không có, check X-Forwarded-For (Chuẩn Proxy chung)
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Forwarded-For");
            // X-Forwarded-For có thể là chuỗi: "client_ip, proxy1, proxy2"
            // Ta chỉ lấy cái đầu tiên
            if (StringUtils.hasText(ip)) {
                ip = ip.split(",")[0].trim();
            }
        }

        // 3. Các header khác (Proxy-Client-IP, etc.) nếu cần...
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }

        // 4. Cuối cùng mới lấy RemoteAddr
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // Xử lý IPv6 localhost (0:0:0:0:0:0:0:1) thành 127.0.0.1 nếu test local
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            ip = "127.0.0.1";
        }

        return ip;
    }
}