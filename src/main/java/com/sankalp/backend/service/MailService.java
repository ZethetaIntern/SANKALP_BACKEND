package com.sankalp.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class MailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${BREVO_API_KEY:}")
    private String brevoApiKey;

    @Value("${MAIL_SENDER_EMAIL:khageswararaor@gmail.com}")
    private String senderEmail;

    public void sendOtpEmail(String toEmail, String otp) {

        // 1. If Brevo API key is configured, send via HTTPS REST API (Port 443 - never blocked by cloud hosts)
        if (brevoApiKey != null && !brevoApiKey.trim().isEmpty()) {
            boolean sent = sendViaBrevo(toEmail, otp);
            if (sent) {
                System.out.println("OTP email delivered via Brevo API to " + toEmail);
                return;
            }
        }

        // 2. Fallback to standard SMTP if available
        if (mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(toEmail);
                message.setSubject("Sankalp HRMS - OTP Verification");
                message.setText(
                        "Dear Employee,\n\n" +
                                "Your OTP for login is: " + otp +
                                "\n\nThis OTP is valid for 5 minutes." +
                                "\n\nDo not share it with anyone." +
                                "\n\nRegards,\nSankalp HRMS"
                );
                mailSender.send(message);
                System.out.println("Email sent successfully via SMTP to: " + toEmail);
            } catch (Exception e) {
                System.err.println("SMTP email delivery failed: " + e.getMessage());
            }
        }
    }

    private boolean sendViaBrevo(String toEmail, String otp) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            String safeSender = (senderEmail != null && !senderEmail.isBlank()) ? senderEmail.trim() : "khageswararaor@gmail.com";
            String jsonPayload = String.format(
                    "{\"sender\":{\"name\":\"Sankalp HRMS\",\"email\":\"%s\"},\"to\":[{\"email\":\"%s\"}],\"subject\":\"Sankalp HRMS - OTP Verification\",\"htmlContent\":\"<div style='font-family:Arial,sans-serif;padding:20px;border:1px solid #ddd;border-radius:8px;'><h2 style='color:#2563eb;'>Sankalp HRMS Portal</h2><p>Dear Employee,</p><p>Your one-time password (OTP) for login is:</p><h1 style='color:#1e40af;letter-spacing:4px;font-size:32px;'>%s</h1><p>This code is valid for 5 minutes. Do not share this OTP with anyone.</p><p style='color:#666;font-size:12px;'>Regards,<br>Sankalp HRMS Team</p></div>\"}",
                    safeSender, toEmail.trim(), otp.trim()
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .header("accept", "application/json")
                    .header("api-key", brevoApiKey.trim())
                    .header("content-type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Brevo API Status Code: " + response.statusCode());
            System.out.println("Brevo API Response Body: " + response.body());

            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            System.err.println("Failed to send email via Brevo API: " + e.getMessage());
            return false;
        }
    }
}