package com.sallejoven.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sallejoven.backend.model.entity.Event;
import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.EventUser;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.enums.ReportType;
import com.sallejoven.backend.repository.projection.SeguroRow;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportQueueServiceTest {

    @Mock EventService eventService;
    @Mock EventUserService eventUserService;
    @Mock AcademicStateService academicStateService;
    @Mock AuthService authService;
    @Mock AuthorityService authorityService;
    @Mock UserGroupService userGroupService;

    @InjectMocks ReportQueueService reportQueueService;

    @Test
    void resolveMembershipForReport_ignoresOtherYearsAndChoosesDeterministicCurrentMembership() {
        UserSalle user = user("Ana", "Zulu");
        UserGroup previousYear = membership(user, "Beta", 0, 2024, 0);
        UserGroup currentYearLaterCenter = membership(user, "Gamma", 0, 2025, 0);
        UserGroup currentYearAlphabeticallyFirst = membership(user, "Alpha", 2, 2025, 1);
        user.setGroups(new HashSet<>(List.of(previousYear, currentYearLaterCenter, currentYearAlphabeticallyFirst)));

        UserGroup selected = reportQueueService.resolveMembershipForReport(user, 2025, true, Set.of());

        assertThat(selected).isSameAs(currentYearAlphabeticallyFirst);
    }

    @Test
    void prepareParticipantsForReport_ordersByVisibleScopedMembershipInsteadOfHiddenOne() {
        UUID visibleCenterUuid = UUID.randomUUID();

        UserSalle userSortedByHiddenCenterIfBuggy = user("Lucas", "Bravo");
        UserGroup hiddenMembership = membership(userSortedByHiddenCenterIfBuggy, UUID.randomUUID(), "Alpha", 0, 2025, 0);
        UserGroup visibleLateMembership = membership(userSortedByHiddenCenterIfBuggy, visibleCenterUuid, "Beta", 2, 2025, 0);
        userSortedByHiddenCenterIfBuggy.setGroups(new HashSet<>(List.of(hiddenMembership, visibleLateMembership)));

        UserSalle userVisibleFirst = user("Mario", "Alonso");
        UserGroup visibleEarlyMembership = membership(userVisibleFirst, visibleCenterUuid, "Beta", 1, 2025, 0);
        userVisibleFirst.setGroups(new HashSet<>(List.of(visibleEarlyMembership)));

        EventUser second = EventUser.builder().uuid(UUID.randomUUID()).user(userSortedByHiddenCenterIfBuggy).build();
        EventUser first = EventUser.builder().uuid(UUID.randomUUID()).user(userVisibleFirst).build();

        List<EventUser> ordered = reportQueueService.prepareParticipantsForReport(
                List.of(second, first),
                2025,
                false,
                Set.of(visibleCenterUuid)
        );

        assertThat(ordered).containsExactly(first, second);
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildInputJson_matchesLambdaEventContract() throws Exception {
        UUID centerUuid = UUID.randomUUID();
        UserSalle user = user("Ana", "Perez");
        user.setEmail("ana@example.com");
        user.setPhone("600000001");
        user.setFatherPhone("600000002");
        user.setMotherPhone("600000003");
        user.setIntolerances("Gluten");
        user.setChronicDiseases("Asthma");
        user.setImageAuthorization(true);
        user.setTshirtSize(2);

        UserGroup membership = membership(user, centerUuid, "La Salle", 1, 2025, 0);
        membership.getGroup().getCenter().setCity("Sevilla");
        user.setGroups(new HashSet<>(List.of(membership)));

        Event event = Event.builder()
                .uuid(UUID.randomUUID())
                .name("Convivencia")
                .description("Salida de grupo")
                .eventDate(LocalDate.of(2026, 4, 19))
                .endDate(LocalDate.of(2026, 4, 20))
                .place("Sevilla")
                .build();

        EventUser eventUser = EventUser.builder()
                .uuid(UUID.randomUUID())
                .event(event)
                .user(user)
                .status(1)
                .build();

        Method method = ReportQueueService.class.getDeclaredMethod(
                "buildInputJson",
                Event.class,
                List.class,
                int.class,
                boolean.class,
                Set.class
        );
        method.setAccessible(true);

        Map<String, Object> payload = (Map<String, Object>) method.invoke(
                reportQueueService,
                event,
                List.of(eventUser),
                2025,
                true,
                Set.of()
        );

        assertThat(payload).containsKeys("event", "participants");

        Map<String, Object> eventMap = (Map<String, Object>) payload.get("event");
        assertThat(eventMap).containsEntry("eventUuid", event.getUuid());
        assertThat(eventMap).containsEntry("name", "Convivencia");
        assertThat(eventMap).doesNotContainKey("eventId");

        List<Map<String, Object>> participants = (List<Map<String, Object>>) payload.get("participants");
        assertThat(participants).hasSize(1);

        Map<String, Object> participant = participants.getFirst();
        assertThat(participant).containsEntry("fullName", "Ana Perez");
        assertThat(participant).containsEntry("userType", 0);
        assertThat(participant).containsEntry("tshirtSize", 2);
        assertThat(participant).containsEntry("imageAuthorization", true);

        Map<String, Object> group = (Map<String, Object>) participant.get("group");
        assertThat(group).containsEntry("stage", 1);

        Map<String, Object> center = (Map<String, Object>) group.get("center");
        assertThat(center).containsEntry("uuid", centerUuid);
        assertThat(center).containsEntry("name", "La Salle");
        assertThat(center).containsEntry("city", "Sevilla");
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildSqsPayload_matchesLambdaQueueContract() throws Exception {
        Method method = ReportQueueService.class.getDeclaredMethod(
                "buildSqsPayload",
                long.class,
                String.class,
                List.class,
                String.class,
                String.class,
                boolean.class,
                String.class,
                String.class
        );
        method.setAccessible(true);

        Map<String, Object> payload = (Map<String, Object>) method.invoke(
                reportQueueService,
                1713555555555L,
                "8069533f-58e9-fdf8-9bd3-192dbf68a3cd",
                List.of(ReportType.ASISTENCIA, ReportType.CAMISETAS),
                "2026/inputs/jobs/1713555555555/input.json",
                "2026/reports/event_8069533f-58e9-fdf8-9bd3-192dbf68a3cd",
                false,
                "admin@example.com",
                "prod"
        );

        assertThat(payload).containsEntry("jobId", 1713555555555L);
        assertThat(payload).containsEntry("eventUuid", "8069533f-58e9-fdf8-9bd3-192dbf68a3cd");
        assertThat(payload).containsEntry("s3InputKey", "2026/inputs/jobs/1713555555555/input.json");
        assertThat(payload).containsEntry("outputPrefix", "2026/reports/event_8069533f-58e9-fdf8-9bd3-192dbf68a3cd");
        assertThat(payload).containsEntry("overwrite", false);
        assertThat(payload).containsEntry("notifyEmail", "admin@example.com");
        assertThat(payload).containsEntry("environment", "prod");
        assertThat((List<String>) payload.get("types")).containsExactly("ASISTENCIA", "CAMISETAS");
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildSeguroInputJson_matchesLambdaSeguroContract() throws Exception {
        SeguroRow row = new SeguroRow() {
            @Override
            public UUID getUserUuid() {
                return UUID.randomUUID();
            }

            @Override
            public String getName() {
                return "Ana";
            }

            @Override
            public String getLastName() {
                return "Perez";
            }

            @Override
            public LocalDate getBirthDate() {
                return LocalDate.of(2010, 5, 1);
            }

            @Override
            public String getDni() {
                return "12345678A";
            }

            @Override
            public Integer getUserType() {
                return 1;
            }

            @Override
            public String getCentersGroups() {
                return "La Salle (Sevilla) - GENESARET 1";
            }
        };

        Method method = ReportQueueService.class.getDeclaredMethod("buildSeguroInputJson", List.class, int.class);
        method.setAccessible(true);

        Map<String, Object> payload = (Map<String, Object>) method.invoke(
                reportQueueService,
                List.of(row),
                2026
        );

        Map<String, Object> meta = (Map<String, Object>) payload.get("meta");
        assertThat(meta).containsEntry("title", "Informe de seguro general");
        assertThat(meta).containsEntry("year", 2026);

        List<Map<String, Object>> rows = (List<Map<String, Object>>) payload.get("rows");
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst()).containsEntry("fullName", "Ana Perez");
        assertThat(rows.getFirst()).containsEntry("dni", "12345678A");
        assertThat(rows.getFirst()).containsEntry("centersGroups", "La Salle (Sevilla) - GENESARET 1");
        assertThat(rows.getFirst()).containsEntry("userType", 1);
    }

    private UserSalle user(String name, String lastName) {
        return UserSalle.builder()
                .uuid(UUID.randomUUID())
                .name(name)
                .lastName(lastName)
                .groups(new HashSet<>())
                .build();
    }

    private UserGroup membership(UserSalle user, String centerName, int stage, int year, int userType) {
        return membership(user, UUID.randomUUID(), centerName, stage, year, userType);
    }

    private UserGroup membership(UserSalle user, UUID centerUuid, String centerName, int stage, int year, int userType) {
        Center center = new Center();
        center.setUuid(centerUuid);
        center.setName(centerName);

        GroupSalle group = GroupSalle.builder()
                .uuid(UUID.randomUUID())
                .center(center)
                .stage(stage)
                .build();

        return UserGroup.builder()
                .uuid(UUID.randomUUID())
                .user(user)
                .group(group)
                .year(year)
                .userType(userType)
                .build();
    }
}
