package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.entity.AcademicState;
import com.sallejoven.backend.model.types.ErrorCodes;
import com.sallejoven.backend.repository.AcademicStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class AcademicStateService {

    private final AcademicStateRepository repo;

    public int getVisibleYear() throws SalleException {
        return repo.findById((short)1)
                .orElseThrow(() -> new SalleException(ErrorCodes.ACADEMIC_STATE_NOT_INITIALIZED))
                .getVisibleYear();
    }

    @Transactional
    public int setVisibleYear(int year) {
        AcademicState st = repo.findById((short)1).orElseGet(AcademicState::new);
        st.setId((short)1);
        st.setVisibleYear(year);
        st.setPromotedAt(OffsetDateTime.now());
        repo.save(st);
        return year;
    }
}
