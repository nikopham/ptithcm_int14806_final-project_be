package com.ptithcm.movie.auth.service;

import com.ptithcm.movie.common.constant.ErrorCode;
import com.ptithcm.movie.common.dto.ServiceResult;
import com.ptithcm.movie.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    /* ------------------------------------------------------------------ */
    public ServiceResult sendRegisterVerification(User user, String link) {
        String subject = "Xác nhận tài khoản – Movie Streaming";
        String html = """
            <h2>Chào %s!</h2>
            <p>Cảm ơn bạn đã đăng ký. Nhấn vào liên kết bên dưới để kích hoạt tài khoản:</p>
            <p><a href="%s">%s</a></p>
            """.formatted(user.getUsername(), link, link);
        log.info("Sending email to {}", link);
        return send(user.getEmail(), subject, html,
                "Đã gửi link xác nhận vào email");
    }

    /* ------------------------------------------------------------------ */
    public ServiceResult sendPasswordReset(User user, String link) {
        String subject = "Đặt lại mật khẩu – Movie Streaming";
        String html = """
            <h2>Xin chào %s!</h2>
            <p>Bạn (hoặc ai đó) đã yêu cầu đặt lại mật khẩu. Nếu là bạn, hãy nhấn:</p>
            <p><a href="%s">%s</a></p>
            <p>Nếu không phải bạn, vui lòng bỏ qua email này.</p>
            """.formatted(user.getUsername(), link, link);

        return send(user.getEmail(), subject, html,
                "Đã gửi hướng dẫn đặt lại mật khẩu vào email");
    }

    /* ========= PRIVATE low-level helper ========== */
    private ServiceResult send(String to, String subject, String html, String okMsg) {
        try {
            log.info("Sending email to {}", to);
            var mime = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(mime, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(mime);

            return ServiceResult.Success()
                    .code(ErrorCode.SUCCESS)
                    .message(okMsg);

        } catch (Exception e) {
            log.error("Sending email to {} failed", to, e);
            return ServiceResult.Failure()
                    .code(ErrorCode.MAIL_SEND_ERROR)
                    .message("Không thể gửi email. Vui lòng thử lại sau");
        }
    }
}
