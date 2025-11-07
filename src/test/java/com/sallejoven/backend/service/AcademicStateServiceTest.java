package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.entity.AcademicState;
import com.sallejoven.backend.model.enums.ErrorCodes;
import com.sallejoven.backend.repository.AcademicStateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcademicStateServiceTest {

    @Mock AcademicStateRepository repo;
    @InjectMocks AcademicStateService service;

    // -------- getVisibleYear()

    @Test
    void getVisibleYear_returnsValue_whenStateExists() throws Exception {
        var st = new AcademicState();
        st.setId((short) 1);
        st.setVisibleYear(2025);
        when(repo.findById((short)1)).thenReturn(Optional.of(st));

        int result = service.getVisibleYear();

        assertThat(result).isEqualTo(2025);
        verify(repo).findById((short)1);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void getVisibleYear_throwsSalleException_whenMissing() {
        when(repo.findById((short)1)).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(service::getVisibleYear);

        assertThat(thrown).isInstanceOf(SalleException.class);
        var se = (SalleException) thrown;

        assertThat(se.getErrorCode())
                .isEqualTo(ErrorCodes.ACADEMIC_STATE_NOT_INITIALIZED.getErrorCode());
    }

    // -------- getVisibleYearOrNull()

    @Test
    void getVisibleYearOrNull_returnsNull_whenMissing() {
        when(repo.findById((short)1)).thenReturn(Optional.empty());

        Integer result = service.getVisibleYearOrNull();

        assertThat(result).isNull();
        verify(repo).findById((short)1);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void getVisibleYearOrNull_returnsValue_whenExists() {
        var st = new AcademicState();
        st.setId((short) 1);
        st.setVisibleYear(2024);
        when(repo.findById((short)1)).thenReturn(Optional.of(st));

        Integer result = service.getVisibleYearOrNull();

        assertThat(result).isEqualTo(2024);
        verify(repo).findById((short)1);
        verifyNoMoreInteractions(repo);
    }

    // -------- setVisibleYear(int)

    @Test
    void setVisibleYear_updatesExisting_andSaves_andReturnsYear() {
        var existing = new AcademicState();
        existing.setId((short) 1);
        existing.setVisibleYear(2023);
        when(repo.findById((short)1)).thenReturn(Optional.of(existing));

        int returned = service.setVisibleYear(2026);

        assertThat(returned).isEqualTo(2026);

        // Capturamos lo que se guarda para validar campos
        ArgumentCaptor<AcademicState> captor = ArgumentCaptor.forClass(AcademicState.class);
        verify(repo).findById((short)1);
        verify(repo).save(captor.capture());
        var saved = captor.getValue();

        assertThat(saved.getId()).isEqualTo((short)1);
        assertThat(saved.getVisibleYear()).isEqualTo(2026);
        assertThat(saved.getPromotedAt()).isNotNull();

        verifyNoMoreInteractions(repo);
    }

    @Test
    void setVisibleYear_createsWhenMissing_setsId1_setsFields_andSaves() {
        when(repo.findById((short)1)).thenReturn(Optional.empty());

        int returned = service.setVisibleYear(2027);

        assertThat(returned).isEqualTo(2027);

        ArgumentCaptor<AcademicState> captor = ArgumentCaptor.forClass(AcademicState.class);
        InOrder inOrder = inOrder(repo);
        inOrder.verify(repo).findById((short)1);
        inOrder.verify(repo).save(captor.capture());
        var saved = captor.getValue();

        assertThat(saved.getId()).isEqualTo((short)1);
        assertThat(saved.getVisibleYear()).isEqualTo(2027);
        assertThat(saved.getPromotedAt()).isNotNull();

        verifyNoMoreInteractions(repo);
    }

    // -------- isLocked()

    @Test
    void isLocked_returnsFalse_whenMissing() {
        when(repo.findById((short)1)).thenReturn(Optional.empty());

        boolean locked = service.isLocked();

        assertThat(locked).isFalse();
        verify(repo).findById((short)1);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void isLocked_returnsTrue_whenStateLocked() {
        var st = new AcademicState();
        st.setId((short) 1);
        st.setLocked(true);
        when(repo.findById((short)1)).thenReturn(Optional.of(st));

        boolean locked = service.isLocked();

        assertThat(locked).isTrue();
        verify(repo).findById((short)1);
        verifyNoMoreInteractions(repo);
    }

    // -------- setLocked(boolean)

    @Test
    void setLocked_updatesAndSaves_whenExists() {
        var st = new AcademicState();
        st.setId((short) 1);
        st.setLocked(false);
        when(repo.findById((short)1)).thenReturn(Optional.of(st));

        service.setLocked(true);

        ArgumentCaptor<AcademicState> captor = ArgumentCaptor.forClass(AcademicState.class);
        verify(repo).findById((short)1);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().isLocked()).isTrue();
        verifyNoMoreInteractions(repo);
    }

    @Test
    void setLocked_throwsIllegalState_whenMissing() {
        when(repo.findById((short)1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setLocked(true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Academic state not found");

        verify(repo).findById((short)1);
        verifyNoMoreInteractions(repo);
    }
}