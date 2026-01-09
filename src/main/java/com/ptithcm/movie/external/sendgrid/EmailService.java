package com.ptithcm.movie.external.sendgrid;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private SendGrid sendGrid;

    @Value("${spring.sendgrid.from-email}")
    private String fromEmail;

    public void sendTextEmail(String toEmail, String subject, String body) {
        // 1. Tạo đối tượng Email người gửi và người nhận
        Email from = new Email(fromEmail);
        Email to = new Email(toEmail);

        // 2. Tạo nội dung (Content)
        // Nếu muốn gửi HTML thì thay "text/plain" bằng "text/html"
        Content content = new Content("text/plain", body);

        // 3. Tạo đối tượng Mail hoàn chỉnh
        Mail mail = new Mail(from, subject, to, content);

        // 4. Tạo Request gửi đến API SendGrid
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            // 5. Thực hiện gửi
            Response response = sendGrid.api(request);

            // Kiểm tra kết quả (Status code 2xx là thành công)
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                logger.info("Email sent successfully to: {}", toEmail);
            } else {
                logger.error("Failed to send email. Status Code: {}", response.getStatusCode());
                logger.error("Response Body: {}", response.getBody());
            }

        } catch (IOException ex) {
            logger.error("Error sending email via SendGrid API", ex);
            // Có thể throw exception ra ngoài để Controller xử lý nếu muốn
        }
    }
}