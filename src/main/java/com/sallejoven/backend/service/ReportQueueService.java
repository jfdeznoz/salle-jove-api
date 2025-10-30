// ReportQueueService.java
package com.sallejoven.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sallejoven.backend.model.dto.ReportQueueResult;
import com.sallejoven.backend.model.entity.Event;
import com.sallejoven.backend.model.entity.EventUser;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.enums.TshirtSizeEnum;
import com.sallejoven.backend.model.types.ReportType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportQueueService {

    private final EventService eventService;
    private final EventUserService eventUserService;
    private final AcademicStateService academicStateService;
    private final AuthService authService; // para el email del solicitante

    @Value("${salle.aws.bucket-name}")            private String bucket;           // sallejoven-events
    @Value("${salle.aws.region:eu-north-1}") private String awsRegion;
    @Value("${salle.reports.queueUrl}")  private String queueUrl;         // URL SQS
    @Value("${salle.aws.prefix:}")           private String s3Prefix;         // "" o "test/"
    @Value("${salle.reports.fromEmail}")     private String fromEmail;        // FROM_EMAIL en Lambda (informativo)
    @Value("${salle.reports.environment:}")  private String envOverride;      // "local"|"prod" opcional

    private final ObjectMapper om = new ObjectMapper();

    public ReportQueueResult enqueueEventReports(Long eventId, List<ReportType> types, boolean overwrite) throws Exception {
        Event event = eventService.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado ID: " + eventId));

        List<EventUser> participants = eventUserService.findConfirmedByEventIdOrdered(eventId);
        int year = academicStateService.getVisibleYear();

        // 1) jobId & rutas
        long jobId = Instant.now().toEpochMilli();
        String inputKeyNoPrefix = year + "/inputs/jobs/" + jobId + "/input.json";
        String outputPrefixNoPrefix = year + "/reports/event_" + eventId;

        // 2) subimos input.json a S3 (con prefijo si toca)
        String inputKeyWithPrefix = withPrefix(inputKeyNoPrefix); // test/... si procede
        byte[] body = om.writeValueAsBytes(buildInputJson(event, participants));
        putObject(bucket, inputKeyWithPrefix, "application/json", body);

        // 3) construimos y enviamos el mensaje a SQS (sin prefijo en las rutas; Lambda añade test/ si env=local)
        String requesterEmail = authService.getCurrentUserEmail();
        String environment = decideEnvironment(); // "local" si s3Prefix empieza por "test/", si no "prod"
        Map<String, Object> payload = buildSqsPayload(jobId, eventId, types, inputKeyNoPrefix,
                outputPrefixNoPrefix, overwrite, requesterEmail, environment);

        sendSqsMessage(queueUrl, om.writeValueAsString(payload));

        // 4) Devolvemos metadata útil
        String resultKey = withPrefix("jobs/" + jobId + "/result.json");
        return new ReportQueueResult(jobId, resultKey, outputPrefixNoPrefix, environment);
    }

    private Map<String, Object> buildInputJson(Event event, List<EventUser> eus) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("event", Map.of(
                "eventId", event.getId(),
                "name", Optional.ofNullable(event.getName()).orElse(""),
                "description", Optional.ofNullable(event.getDescription()).orElse(""),
                "eventDate", Optional.ofNullable(event.getEventDate()).map(Object::toString).orElse(""),
                "endDate", Optional.ofNullable(event.getEndDate()).map(Object::toString).orElse(""),
                "place", Optional.ofNullable(event.getPlace()).orElse("")
        ));

        List<Map<String, Object>> participants = eus.stream().map(eu -> {
            UserSalle u = eu.getUserGroup().getUser();
            GroupSalle g = eu.getUserGroup().getGroup();

            Integer sizeIdx = Optional.ofNullable(u.getTshirtSize()).map(TshirtSizeEnum::fromIndex)
                    .map(Enum::ordinal).orElse(null);

            return Map.<String, Object>of(
                    "fullName", (u.getName() == null ? "" : u.getName()) + " " + (u.getLastName() == null ? "" : u.getLastName()),
                    "email", Optional.ofNullable(u.getEmail()).orElse(""),
                    "fatherPhone", Optional.ofNullable(u.getFatherPhone()).orElse(""),
                    "motherPhone", Optional.ofNullable(u.getMotherPhone()).orElse(""),
                    "intolerances", Optional.ofNullable(u.getIntolerances()).orElse(""),
                    "chronicDiseases", Optional.ofNullable(u.getChronicDiseases()).orElse(""),
                    "imageAuthorization", Boolean.TRUE.equals(u.getImageAuthorization()),
                    "tshirtSize", sizeIdx,
                    "userType", Optional.ofNullable(eu.getUserGroup().getUserType()).orElse(0),
                    "group", Map.of(
                            "stage", Optional.ofNullable(g.getStage()).orElse(0),
                            "center", Map.of(
                                    "name", Optional.ofNullable(g.getCenter()).map(c -> Optional.ofNullable(c.getName()).orElse("")).orElse(""),
                                    "city", Optional.ofNullable(g.getCenter()).map(c -> Optional.ofNullable(c.getCity()).orElse("")).orElse("")
                            )
                    )
            );
        }).collect(Collectors.toList());

        m.put("participants", participants);
        return m;
    }

    private Map<String, Object> buildSqsPayload(long jobId, Long eventId, List<ReportType> types,
                                                String s3InputKeyNoPrefix, String outputPrefixNoPrefix,
                                                boolean overwrite, String notifyEmail, String environment) {
        return new LinkedHashMap<>(Map.of(
                "jobId", jobId,
                "eventId", eventId,
                "types", types.stream().map(Enum::name).collect(Collectors.toList()),
                "s3InputKey", s3InputKeyNoPrefix,
                "outputPrefix", outputPrefixNoPrefix,
                "overwrite", overwrite,
                "notifyEmail", notifyEmail,
                "environment", environment
        ));
    }

    private String decideEnvironment() {
        if (envOverride != null && !envOverride.isBlank()) return envOverride;
        return (s3Prefix != null && s3Prefix.startsWith("test")) ? "local" : "prod";
    }

    // ---------- AWS I/O ----------
    private void putObject(String bucket, String key, String contentType, byte[] bytes) {
        try (S3Client s3 = S3Client.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build()) {

            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromBytes(bytes)
            );
        }
    }

    private void sendSqsMessage(String queueUrl, String body) {
        try (SqsClient sqs = SqsClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build()) {

            sqs.sendMessage(SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(body)
                    .build());
        }
    }

    private String withPrefix(String path) {
        if (s3Prefix == null || s3Prefix.isBlank()) return normalize(path);
        String p = s3Prefix.endsWith("/") ? s3Prefix : (s3Prefix + "/");
        return normalize(p + path);
    }
    private String normalize(String p) {
        String x = p.replaceAll("/{2,}", "/");
        if (x.startsWith("/")) x = x.substring(1);
        return x;
    }

}
