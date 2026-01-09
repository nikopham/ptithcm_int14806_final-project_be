package com.ptithcm.movie.config;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailConfig {

    private final JavaMailSender mailSender;

    public void sendVerification(String toEmail, String verifyLink) {
        try {
            var mime = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(mime, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("Verify your Movie account");
            helper.setText("""
                    <h2>Welcome to Movie Streaming!</h2>
                    <p>Click the link below to activate your account:</p>
                    <p><a href="%s">%s</a></p>
                    """.formatted(verifyLink, verifyLink), true);
            mailSender.send(mime);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send verification mail", e);
        }
    }
}

