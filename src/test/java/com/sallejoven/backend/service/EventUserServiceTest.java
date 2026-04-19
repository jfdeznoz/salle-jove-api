package com.sallejoven.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sallejoven.backend.model.entity.EventUser;
import com.sallejoven.backend.repository.EventUserRepository;
import com.sallejoven.backend.repository.UserGroupRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventUserServiceTest {

    @Mock EventUserRepository eventUserRepository;
    @Mock EventGroupService eventGroupService;
    @Mock UserGroupRepository userGroupRepository;
    @Mock AcademicStateService academicStateService;

    @InjectMocks EventUserService eventUserService;

    @Test
    void findConfirmedByEventIdForReport_returnsEmptyWithoutFetchWhenNoConfirmedParticipantsExist() {
        UUID eventUuid = UUID.randomUUID();

        when(eventUserRepository.findConfirmedUuids(eventUuid)).thenReturn(List.of());

        assertThat(eventUserService.findConfirmedByEventIdForReport(eventUuid)).isEmpty();

        verify(eventUserRepository).findConfirmedUuids(eventUuid);
        verify(eventUserRepository, never()).findByUuidInFetchForReport(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void findConfirmedByEventIdForReport_loadsParticipantsWithReportGraph() {
        UUID eventUuid = UUID.randomUUID();
        UUID participantUuid = UUID.randomUUID();
        EventUser eventUser = EventUser.builder().uuid(participantUuid).build();

        when(eventUserRepository.findConfirmedUuids(eventUuid)).thenReturn(List.of(participantUuid));
        when(eventUserRepository.findByUuidInFetchForReport(List.of(participantUuid))).thenReturn(List.of(eventUser));

        assertThat(eventUserService.findConfirmedByEventIdForReport(eventUuid)).containsExactly(eventUser);

        verify(eventUserRepository).findByUuidInFetchForReport(List.of(participantUuid));
    }
}
