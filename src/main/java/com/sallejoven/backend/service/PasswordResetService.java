package com.sallejoven.backend.service;

import com.sallejoven.backend.model.entity.PasswordResetToken;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.repository.PasswordResetTokenRepository;
import com.sallejoven.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value; // <--- Spring @Value
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;      // <---
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.mail.internet.MimeMessage;                     // <---
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;                           // <---
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;                                     // <---

@Service
public class PasswordResetService {

    private final UserRepository userRepo;                  // Debe ser repo de UserSalle
    private final PasswordResetTokenRepository tokenRepo;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;          // Inyectado

    @Value("${app.reset.base-url:https://tu-app/#/reset-password}")
    private String resetBaseUrl;

    public PasswordResetService(UserRepository userRepo,
                                PasswordResetTokenRepository tokenRepo,
                                JavaMailSender mailSender,
                                PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.tokenRepo = tokenRepo;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void startReset(String emailRaw) {
        final String email = emailRaw == null ? "" : emailRaw.trim().toLowerCase();

        // (Opcional) rate limit por email/IP

        // Genera SIEMPRE un token (para no filtrar existencia)
        final String token = generateToken();
        final Instant now = Instant.now();
        final Instant exp = now.plus(Duration.ofMinutes(10));

        PasswordResetToken prt = new PasswordResetToken();
        prt.setToken(token);
        prt.setEmail(email);
        prt.setCreatedAt(now);
        prt.setExpiresAt(exp);
        prt.setUsed(false);
        tokenRepo.save(prt);    

        // Enlace
        String link = resetBaseUrl + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);

        sendEmail(email, link);
    }

    @Transactional
    public void confirmReset(String token, String newPassword) {
        PasswordResetToken prt = tokenRepo.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "token_invalid"));

        if (prt.isUsed() || prt.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "token_expired_or_used");
        }

        UserSalle user = userRepo.findByEmail(prt.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "user_not_found"));

        final String np = newPassword == null ? "" : newPassword.trim();
        if (np.length() < 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "weak_password");
        }

        user.setPassword(passwordEncoder.encode(np));
        userRepo.save(user);

        // Marca token como usado
        prt.setUsed(true);
        tokenRepo.save(prt);

        // (Opcional) invalidar sesiones activas propias
    }

    private String generateToken() {
        byte[] bytes = new byte[48]; // 384 bits
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void sendEmail(String to, String link) {
        final String logoUrl = "https://salle-joven.s3.eu-west-3.amazonaws.com/logo.png";

        final String blue   = "#3B5998";
        final String yellow = "#F2B01E";

        // OJO: todos los % del HTML están escapados como %%
        String html = String.format("""
                <!DOCTYPE html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8">
                  <title>Restablecer contraseña</title>
                  <meta name="viewport" content="width=device-width,initial-scale=1.0"/>
                </head>
                <body style="margin:0; padding:0; background-color:#f6f7fb; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background-color:#f6f7fb; padding:32px 16px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="max-width:600px; background:#ffffff; border-radius:16px; overflow:hidden; box-shadow:0 6px 20px rgba(0,0,0,0.06);">
                          <tr>
                            <td style="height:6px; background:%2$s;"></td>
                          </tr>
                          <tr>
                            <td align="center" style="padding:28px 24px 8px 24px;">
                              <img src="%1$s" width="140" alt="Salle Joven" style="display:block; max-width:200px; height:auto; margin:0 auto;"/>
                            </td>
                          </tr>
                          <tr>
                            <td align="center" style="padding:0 24px 4px 24px;">
                              <h1 style="margin:0; font-size:22px; line-height:1.3; color:%3$s; font-weight:800;">
                                Restablece tu contraseña
                              </h1>
                            </td>
                          </tr>
                          <tr>
                            <td align="center" style="padding:6px 32px 0 32px;">
                              <p style="margin:0; font-size:15px; line-height:1.6; color:#424242;">
                                Hemos recibido una solicitud para cambiar tu contraseña. Haz clic en el botón para continuar.
                              </p>
                            </td>
                          </tr>
                          <tr>
                            <td align="center" style="padding:22px 32px 8px 32px;">
                              <a href="%4$s"
                                 style="display:inline-block; background:%3$s; color:#ffffff; text-decoration:none; 
                                        padding:12px 22px; border-radius:10px; font-weight:700; font-size:15px;">
                                Restablecer contraseña
                              </a>
                            </td>
                          </tr>
                          <tr>
                            <td align="center" style="padding:16px 32px 8px 32px;">
                              <p style="margin:0; font-size:12px; line-height:1.5; color:#6b7280;">
                                Si el botón no funciona, copia y pega este enlace en tu navegador:
                              </p>
                            </td>
                          </tr>
                          <tr>
                            <td align="center" style="padding:6px 32px 22px 32px;">
                              <a href="%4$s" style="word-break:break-all; font-size:12px; color:%3$s; text-decoration:underline;">%4$s</a>
                            </td>
                          </tr>
                          <tr>
                            <td align="center" style="padding:10px 24px 26px 24px;">
                              <p style="margin:0; font-size:12px; line-height:1.6; color:#9aa0a6;">
                                Si no solicitaste este cambio, puedes ignorar este correo.
                              </p>
                            </td>
                          </tr>
                          <tr>
                            <td align="center" style="background:#fafafa; border-top:1px solid #eef0f4; padding:16px 24px;">
                              <p style="margin:0; font-size:11px; color:#9aa0a6;">
                                © %5$d Salle Joven
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """,
                // %1$s  %2$s   %3$s    %4$s              %5$d
                logoUrl, yellow, blue, link, java.time.Year.now().getValue()
        );

        try {
            jakarta.mail.internet.MimeMessage message = mailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper =
                    new org.springframework.mail.javamail.MimeMessageHelper(message, "UTF-8");
            helper.setTo(to);
            helper.setSubject("Restablecer tu contraseña · Salle Joven");
            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            // Log interno; no revelar al cliente
        }
    }
}