package com.sallejoven.backend.service;

import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserCenter;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.entity.WeeklySession;
import com.sallejoven.backend.model.enums.AppNotificationReferenceType;
import com.sallejoven.backend.model.enums.AppNotificationType;
import com.sallejoven.backend.model.enums.WeeklySessionWarningType;
import com.sallejoven.backend.repository.UserCenterRepository;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ObservationNotificationService {

    private static final Logger log = LoggerFactory.getLogger(ObservationNotificationService.class);
    private static final DateTimeFormatter SESSION_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", new Locale("es", "ES"));

    private final UserCenterRepository userCenterRepository;
    private final AcademicStateService academicStateService;
    private final NotificationService notificationService;
    private final EmailService emailService;

    @Transactional
    public void notifyGeneralObservation(WeeklySession session, String observations, UserSalle actor) {
        if (session == null || observations == null || observations.isBlank()) {
            return;
        }

        List<UserSalle> recipients = resolveRecipients(session, actor);
        if (recipients.isEmpty()) {
            return;
        }

        String title = "Nueva observación general en sesión semanal";
        String message = buildGeneralObservationMessage(session);
        notificationService.createNotifications(
                recipients,
                AppNotificationType.WEEKLY_SESSION_GENERAL_OBSERVATION,
                title,
                message,
                AppNotificationReferenceType.WEEKLY_SESSION,
                session.getUuid());
        sendEmails(recipients, title, buildGeneralObservationEmail(session, observations));
    }

    @Transactional
    public void notifyPersonalWarnings(WeeklySession session, List<PersonalWarningNotificationItem> items, UserSalle actor) {
        if (session == null || items == null || items.isEmpty()) {
            return;
        }

        List<UserSalle> recipients = resolveRecipients(session, actor);
        if (recipients.isEmpty()) {
            return;
        }

        String title = items.size() == 1
                ? "Nuevo aviso personal en sesión semanal"
                : "Nuevos avisos personales en sesión semanal";
        String message = buildPersonalWarningMessage(session, items);
        notificationService.createNotifications(
                recipients,
                AppNotificationType.WEEKLY_SESSION_PERSONAL_WARNING,
                title,
                message,
                AppNotificationReferenceType.WEEKLY_SESSION,
                session.getUuid());
        sendEmails(recipients, title, buildPersonalWarningEmail(session, items));
    }

    private List<UserSalle> resolveRecipients(WeeklySession session, UserSalle actor) {
        GroupSalle group = session.getGroup();
        if (group == null || group.getCenter() == null || group.getCenter().getUuid() == null) {
            return List.of();
        }

        int year = academicStateService.getVisibleYear();
        UUID actorUuid = actor != null ? actor.getUuid() : null;

        Map<UUID, UserSalle> uniqueRecipients = new LinkedHashMap<>();
        for (UserCenter membership : userCenterRepository.findByCenter_UuidAndYearAndDeletedAtIsNull(group.getCenter().getUuid(), year)) {
            if (membership.getUser() == null || membership.getUser().getUuid() == null) {
                continue;
            }
            Integer userType = membership.getUserType();
            if (!Integer.valueOf(2).equals(userType) && !Integer.valueOf(3).equals(userType)) {
                continue;
            }
            if (membership.getUser().getDeletedAt() != null) {
                continue;
            }
            if (actorUuid != null && actorUuid.equals(membership.getUser().getUuid())) {
                continue;
            }
            uniqueRecipients.putIfAbsent(membership.getUser().getUuid(), membership.getUser());
        }
        return List.copyOf(uniqueRecipients.values());
    }

    private void sendEmails(List<UserSalle> recipients, String subject, String htmlContent) {
        for (UserSalle recipient : recipients) {
            if (recipient.getEmail() == null || recipient.getEmail().isBlank()) {
                continue;
            }
            try {
                emailService.sendEmailWithAttachments(recipient.getEmail(), subject, htmlContent, Map.of());
            } catch (Exception ex) {
                log.warn("Could not send observation notification email to {}", recipient.getEmail(), ex);
            }
        }
    }

    private String buildGeneralObservationMessage(WeeklySession session) {
        return "Se ha añadido o actualizado una observación general en la sesión \"" + session.getTitle() + "\".";
    }

    private String buildGeneralObservationEmail(WeeklySession session, String observations) {
        return "<p>Se ha añadido o actualizado una observación general en la sesión semanal <strong>"
                + escape(session.getTitle())
                + "</strong>.</p><p><strong>Fecha:</strong> "
                + formatSessionDate(session)
                + "</p><p><strong>Observación:</strong><br/>"
                + escape(observations)
                + "</p>";
    }

    private String buildPersonalWarningMessage(WeeklySession session, List<PersonalWarningNotificationItem> items) {
        if (items.size() == 1) {
            PersonalWarningNotificationItem item = items.get(0);
            return "Se ha registrado un aviso " + item.warningType().name().toLowerCase(Locale.ROOT)
                    + " para " + item.participantName()
                    + " en la sesión \"" + session.getTitle() + "\".";
        }
        return "Se han registrado " + items.size() + " avisos personales en la sesión \"" + session.getTitle() + "\".";
    }

    private String buildPersonalWarningEmail(WeeklySession session, List<PersonalWarningNotificationItem> items) {
        StringBuilder builder = new StringBuilder();
        builder.append("<p>Se han registrado avisos personales en la sesión semanal <strong>")
                .append(escape(session.getTitle()))
                .append("</strong>.</p>")
                .append("<p><strong>Fecha:</strong> ")
                .append(formatSessionDate(session))
                .append("</p><ul>");
        for (PersonalWarningNotificationItem item : items) {
            builder.append("<li><strong>")
                    .append(escape(item.participantName()))
                    .append("</strong>: ")
                    .append(item.warningType() == WeeklySessionWarningType.YELLOW ? "Amarilla" : "Roja")
                    .append(" — ")
                    .append(escape(item.comment()))
                    .append("</li>");
        }
        builder.append("</ul>");
        return builder.toString();
    }

    private String formatSessionDate(WeeklySession session) {
        if (session.getSessionDateTime() == null) {
            return "-";
        }
        return SESSION_DATE_FORMATTER.format(session.getSessionDateTime());
    }

    private String escape(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    public record PersonalWarningNotificationItem(
            UUID participantUuid,
            String participantName,
            WeeklySessionWarningType warningType,
            String comment
    ) {}
}
