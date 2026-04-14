package com.sallejoven.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sallejoven.backend.model.dto.ReportQueueResult;
import com.sallejoven.backend.model.entity.Event;
import com.sallejoven.backend.model.entity.EventUser;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.enums.TshirtSizeEnum;
import com.sallejoven.backend.model.enums.ReportType;
import com.sallejoven.backend.repository.projection.SeguroRow;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportQueueService {

    private final EventService eventService;
    private final EventUserService eventUserService;
    private final AcademicStateService academicStateService;
    private final AuthService authService;
    private final AuthorityService authorityService;
    private final UserGroupService userGroupService;

    @Value("${salle.aws.bucket-name}")            private String bucket;
    @Value("${salle.aws.region:eu-north-1}") private String awsRegion;
    @Value("${salle.reports.queueUrl}")  private String queueUrl;
    @Value("${salle.aws.prefix:}")           private String s3Prefix;
    @Value("${salle.reports.fromEmail}")     private String fromEmail;
    @Value("${salle.reports.environment:}")  private String envOverride;

    private final ObjectMapper om = new ObjectMapper();

    public ReportQueueResult enqueueEventReports(UUID eventUuid, List<ReportType> types, boolean overwrite) throws Exception {
        Event event = eventService.findById(eventUuid)
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado ID: " + eventUuid));

        int year = academicStateService.getVisibleYear();
        Set<String> currentAuth = authorityService.getCurrentAuth();
        boolean fullAccess = currentAuth.contains("ROLE_ADMIN");
        Set<UUID> scopedCenterUuids = fullAccess
                ? Set.of()
                : authorityService.extractCenterIdsForYear(currentAuth, year);
        List<EventUser> participants = filterParticipantsByScope(
                eventUserService.findConfirmedByEventIdOrdered(eventUuid),
                fullAccess,
                scopedCenterUuids
        );

        // 1) jobId & rutas
        long jobId = Instant.now().toEpochMilli();
        String inputKeyNoPrefix = year + "/inputs/jobs/" + jobId + "/input.json";
        String outputPrefixNoPrefix = buildOutputPrefix(year, eventUuid, fullAccess, scopedCenterUuids);

        // 2) subimos input.json a S3 (con prefijo si toca)
        String inputKeyWithPrefix = withPrefix(inputKeyNoPrefix); // test/... si procede
        byte[] body = om.writeValueAsBytes(buildInputJson(event, participants, fullAccess, scopedCenterUuids));
        putObject(bucket, inputKeyWithPrefix, "application/json", body);

        // 3) construimos y enviamos el mensaje a SQS (sin prefijo en las rutas; Lambda añade test/ si env=local)
        String requesterEmail = authService.getCurrentUserEmail();
        String environment = decideEnvironment(); // "local" si s3Prefix empieza por "test/", si no "prod"
        Map<String, Object> payload = buildSqsPayload(jobId, eventUuid.toString(), types, inputKeyNoPrefix,
                outputPrefixNoPrefix, overwrite, requesterEmail, environment);

        sendSqsMessage(queueUrl, om.writeValueAsString(payload));

        // 4) Devolvemos metadata útil
        String resultKey = withPrefix("jobs/" + jobId + "/result.json");
        return new ReportQueueResult(jobId, resultKey, outputPrefixNoPrefix, environment);
    }

    public ReportQueueResult enqueueGeneralSeguroReport() throws Exception {
        int year = academicStateService.getVisibleYear();
        long jobId = Instant.now().toEpochMilli();

        // 1) Construir input.json con las filas del seguro
        var rows = userGroupService.findSeguroRowsForCurrentYear(); // tu proyección SeguroRow
        var input = buildSeguroInputJson(rows, year);

        String inputKeyNoPrefix = year + "/inputs/jobs/" + jobId + "/seguro.json";
        String inputKeyWithPref = withPrefix(inputKeyNoPrefix);
        byte[] body = om.writeValueAsBytes(input);
        putObject(bucket, inputKeyWithPref, "application/json", body);

        // 2) Payload SQS
        String outputPrefixNoPrefix = year + "/reports/general";
        String environment = decideEnvironment();
        String requesterEmail = authService.getCurrentUserEmail();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("jobId", jobId);
        payload.put("eventId", 0); // no aplica
        payload.put("types", List.of("SEGURO_GENERAL")); // ← clave para la Lambda
        payload.put("s3InputKey", inputKeyNoPrefix);
        payload.put("outputPrefix", outputPrefixNoPrefix);
        payload.put("overwrite", true);
        payload.put("notifyEmail", requesterEmail);
        payload.put("environment", environment);

        sendSqsMessage(queueUrl, om.writeValueAsString(payload));

        // 3) Result.json que escribirá la Lambda
        String resultKey = withPrefix("jobs/" + jobId + "/result.json");
        return new ReportQueueResult(jobId, resultKey, outputPrefixNoPrefix, environment);
    }

    private Map<String, Object> buildSeguroInputJson(List<SeguroRow> rows, int year) {
        List<Map<String, Object>> list = rows.stream().map(sr -> Map.<String,Object>of(
                "fullName", ((sr.getName()==null?"":sr.getName()) + " " + (sr.getLastName()==null?"":sr.getLastName())).trim(),
                "birthDate", sr.getBirthDate()!=null ? sr.getBirthDate().toString() : "",
                "dni",       sr.getDni()==null ? "" : sr.getDni(),
                "centersGroups", sr.getCentersGroups()==null ? "" : sr.getCentersGroups(),
                "userType", Optional.ofNullable(sr.getUserType()).orElse(0)
        )).toList();

        return Map.of(
                "meta", Map.of("title", "Informe de seguro general", "year", year),
                "rows", list
        );
    }

    private Map<String, Object> buildInputJson(
            Event event,
            List<EventUser> eus,
            boolean fullAccess,
            Set<UUID> scopedCenterUuids
    ) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("event", Map.of(
                "eventId", event.getId(),
                "eventUuid", event.getUuid(),
                "name", Optional.ofNullable(event.getName()).orElse(""),
                "description", Optional.ofNullable(event.getDescription()).orElse(""),
                "eventDate", Optional.ofNullable(event.getEventDate()).map(Object::toString).orElse(""),
                "endDate", Optional.ofNullable(event.getEndDate()).map(Object::toString).orElse(""),
                "place", Optional.ofNullable(event.getPlace()).orElse("")
        ));

        List<Map<String, Object>> participants = eus.stream().map(eu -> {
            UserSalle u = eu.getUser();
            GroupSalle g = resolveGroupForReport(u, fullAccess, scopedCenterUuids);

            Integer sizeIdx = Optional.ofNullable(u.getTshirtSize()).map(TshirtSizeEnum::fromIndex)
                    .map(Enum::ordinal).orElse(-1);

            Map<String, Object> centerMap = new LinkedHashMap<>();
            if (g != null && g.getCenter() != null) {
                centerMap.put("uuid", g.getCenter().getUuid());
                centerMap.put("name", Optional.ofNullable(g.getCenter().getName()).orElse(""));
                centerMap.put("city", Optional.ofNullable(g.getCenter().getCity()).orElse(""));
            } else {
                centerMap.put("uuid", null);
                centerMap.put("name", "");
                centerMap.put("city", "");
            }

            Map<String, Object> part = new LinkedHashMap<>();
            part.put("fullName", (u.getName() == null ? "" : u.getName()) + " " + (u.getLastName() == null ? "" : u.getLastName()));
            part.put("email", Optional.ofNullable(u.getEmail()).orElse(""));
            part.put("phone", Optional.ofNullable(u.getPhone()).orElse(""));
            part.put("fatherPhone", Optional.ofNullable(u.getFatherPhone()).orElse(""));
            part.put("motherPhone", Optional.ofNullable(u.getMotherPhone()).orElse(""));
            part.put("intolerances", Optional.ofNullable(u.getIntolerances()).orElse(""));
            part.put("chronicDiseases", Optional.ofNullable(u.getChronicDiseases()).orElse(""));
            part.put("imageAuthorization", Boolean.TRUE.equals(u.getImageAuthorization()));
            part.put("tshirtSize", sizeIdx);
            part.put("userType", g == null ? 0 : Optional.ofNullable(u.getGroups().stream()
                    .filter(ug -> ug.getDeletedAt() == null && ug.getGroup() != null && ug.getGroup().getUuid().equals(g.getUuid()))
                    .map(ug -> ug.getUserType())
                    .findFirst()
                    .orElse(0)).orElse(0));
            part.put("group", Map.of(
                    "stage", g != null ? Optional.ofNullable(g.getStage()).orElse(0) : 0,
                    "center", centerMap
            ));
            return part;
        }).collect(Collectors.toList());

        m.put("participants", participants);
        return m;
    }

    private List<EventUser> filterParticipantsByScope(
            List<EventUser> participants,
            boolean fullAccess,
            Set<UUID> scopedCenterUuids
    ) {
        if (fullAccess) {
            return participants;
        }
        return participants.stream()
                .filter(eventUser -> resolveGroupForReport(eventUser.getUser(), false, scopedCenterUuids) != null)
                .toList();
    }

    private GroupSalle resolveGroupForReport(UserSalle user, boolean fullAccess, Set<UUID> scopedCenterUuids) {
        if (user == null || user.getGroups() == null) {
            return null;
        }
        return user.getGroups().stream()
                .filter(userGroup -> userGroup.getDeletedAt() == null)
                .map(userGroup -> userGroup.getGroup())
                .filter(group -> group != null)
                .filter(group -> fullAccess
                        || (group.getCenter() != null && scopedCenterUuids.contains(group.getCenter().getUuid())))
                .findFirst()
                .orElse(null);
    }

    private String buildOutputPrefix(int year, UUID eventUuid, boolean fullAccess, Set<UUID> scopedCenterUuids) {
        String basePrefix = year + "/reports/event_" + eventUuid;
        if (fullAccess) {
            return basePrefix;
        }
        String scopeKey = scopedCenterUuids.stream()
                .sorted()
                .map(UUID::toString)
                .map(uuid -> uuid.replace("-", ""))
                .collect(Collectors.joining("_"));
        return basePrefix + "/center_scope_" + scopeKey;
    }

    private Map<String, Object> buildSqsPayload(long jobId, String eventUuid, List<ReportType> types,
                                                String s3InputKeyNoPrefix, String outputPrefixNoPrefix,
                                                boolean overwrite, String notifyEmail, String environment) {
        return new LinkedHashMap<>(Map.of(
                "jobId", jobId,
                "eventUuid", eventUuid,
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
