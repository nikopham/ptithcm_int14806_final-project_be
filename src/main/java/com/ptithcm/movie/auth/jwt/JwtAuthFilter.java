package com.ptithcm.movie.auth.jwt;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.ptithcm.movie.auth.security.CustomUserDetailsService;
import com.ptithcm.movie.config.JwtConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider provider;
    private final CustomUserDetailsService userDetailsService;
    private final JwtConfig jwtConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain)
            throws ServletException, IOException {

        // 1. Lấy token (Ưu tiên Cookie -> Sau đó mới đến Header)
        String token = getJwtFromRequest(req);

        // 2. Nếu tìm thấy token
        if (token != null) {
            try {
                // Verify token (Library JWT sẽ ném Exception nếu token hết hạn hoặc sai chữ ký)
                DecodedJWT jwt = provider.verify(token);

                // Lấy thông tin user (Email hoặc UserID tùy cách bạn gen token)
                String subject = jwt.getSubject();

                // Load UserDetails từ DB
                var userDetails = userDetailsService.loadUserByUsername(subject);

                // Tạo đối tượng Authentication chuẩn của Spring Security
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));

                // Set vào Security Context để các Controller sau này biết là ai đang request
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (JWTVerificationException ex) {
                // Log debug thôi, đừng log info kẻo rác log
                logger.debug("JWT Verification Failed: " + ex.getMessage());
                SecurityContextHolder.clearContext();
            } catch (Exception e) {
                // Bắt các lỗi khác (VD: Không tìm thấy user trong DB)
                logger.error("User Auth Failed", e);
                SecurityContextHolder.clearContext();
            }
        }

        // 3. Cho phép request đi tiếp (dù có auth hay không)
        chain.doFilter(req, res);
    }

    /**
     * Hàm tiện ích để trích xuất JWT từ Request
     * Ưu tiên 1: Lấy từ HttpOnly Cookie "accessToken" (Cho Web Browser)
     * Ưu tiên 2: Lấy từ Header Authorization (Cho Postman/Mobile App)
     */


    private String getJwtFromRequest(HttpServletRequest request) {
        // 1. Tìm trong Cookie
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (jwtConfig.getAccessCookie().equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // 2. Fallback: Tìm trong Header (Bearer Token)
        // Giữ lại cái này rất tiện để bạn test bằng Postman mà không cần set cookie
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }

        return null;
    }

}
