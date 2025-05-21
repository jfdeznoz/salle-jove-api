// src/main/java/com/sallejoven/backend/service/EmailService.java
package com.sallejoven.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;

@Service
public class EmailService {

    @Value("${spring.mail.username}")
    private String mailSenderAddress;

    @Value("${app.email.notifications:true}")
    private boolean emailNotifications;

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Envía un correo HTML con múltiples adjuntos.
     *
     * @param to          dirección destino
     * @param subject     asunto
     * @param htmlContent cuerpo en HTML
     * @param attachments mapa nombreArchivo→bytes
     */
    public void sendEmailWithAttachments(String to, String subject, String htmlContent, Map<String, byte[]> attachments) throws MessagingException {
        if (!emailNotifications) {
            // En entornos de prueba, no envía nada
            return;
        }

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(new InternetAddress(mailSenderAddress));
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        for (Map.Entry<String, byte[]> entry : attachments.entrySet()) {
            helper.addAttachment(entry.getKey(), new ByteArrayResource(entry.getValue()));
        }

        mailSender.send(message);
    }
}