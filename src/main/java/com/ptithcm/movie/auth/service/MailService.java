package com.ptithcm.movie.auth.service;

import com.ptithcm.movie.common.constant.ErrorCode;
import com.ptithcm.movie.common.dto.ServiceResult;
import com.ptithcm.movie.user.entity.User;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
public class MailService {

    private final SendGrid sendGrid;
    private final String fromEmail;

    public MailService(SendGrid sendGrid,
                       @Value("${spring.sendgrid.from-email}") String fromEmail) {
        this.sendGrid = sendGrid;
        this.fromEmail = fromEmail;
    }

    public ServiceResult sendRegisterVerification(User user, String link) {
        String subject = "Xác nhận tài khoản – Movie Streaming";
        String html = """
            <h2>Chào %s!</h2>
            <p>Cảm ơn bạn đã đăng ký. Nhấn vào liên kết bên dưới để kích hoạt tài khoản:</p>
            <p><a href="%s">%s</a></p>
            """.formatted(user.getUsername(), link, link);

        return send(user.getEmail(), subject, html, "Đã gửi link xác nhận vào email");
    }

    public ServiceResult sendPasswordReset(User user, String link) {
        String subject = "Đặt lại mật khẩu – Movie Streaming";
        String html = """
            <h2>Xin chào %s!</h2>
            <p>Bạn (hoặc ai đó) đã yêu cầu đặt lại mật khẩu. Nếu là bạn, hãy nhấn:</p>
            <p><a href="%s">%s</a></p>
            <p>Nếu không phải bạn, vui lòng bỏ qua email này.</p>
            """.formatted(user.getUsername(), link, link);

        return send(user.getEmail(), subject, html, "Đã gửi hướng dẫn đặt lại mật khẩu vào email");
    }

    private ServiceResult send(String toEmail, String subject, String htmlBody, String okMsg) {
        try {
            log.info("Sending email via SendGrid to {}", toEmail);

            Email from = new Email(fromEmail);
            Email to = new Email(toEmail);

            Content content = new Content("text/html", htmlBody);

            Mail mail = new Mail(from, subject, to, content);

            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGrid.api(request);

            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                log.info("Email sent successfully. Status: {}", response.getStatusCode());
                return ServiceResult.Success()
                        .code(ErrorCode.SUCCESS)
                        .message(okMsg);
            } else {
                log.error("SendGrid failed. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
                return ServiceResult.Failure()
                        .code(ErrorCode.MAIL_SEND_ERROR)
                        .message("Lỗi nhà cung cấp dịch vụ mail (SendGrid).");
            }

        } catch (IOException ex) {
            log.error("Sending email to {} failed due to IO Exception", toEmail, ex);
            return ServiceResult.Failure()
                    .code(ErrorCode.MAIL_SEND_ERROR)
                    .message("Không thể kết nối đến server gửi mail.");
        }
    }
}