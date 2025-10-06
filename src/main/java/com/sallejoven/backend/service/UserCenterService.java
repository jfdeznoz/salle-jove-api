package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.UserCenter;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.types.ErrorCodes;
import com.sallejoven.backend.repository.CenterRepository;
import com.sallejoven.backend.repository.UserCenterRepository;
import com.sallejoven.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserCenterService {

    private final UserCenterRepository userCenterRepo;
    private final UserRepository userRepo;
    private final CenterRepository centerRepo;
    private final AcademicStateService academicStateService;

    public UserCenter findByUserForCurrentYear(Long userId) throws SalleException {
        int year = academicStateService.getVisibleYear();
        return userCenterRepo.findByUser_IdAndYearAndDeletedAtIsNull(userId, year);
    }

    public List<UserCenter> findActiveByCenterForCurrentYear(Long centerId) throws SalleException {
        int year = academicStateService.getVisibleYear();
        return userCenterRepo.findByCenter_IdAndYearAndDeletedAtIsNull(centerId, year);
    }

    @Transactional
    public UserCenter addCenterRole(Long userId, Long centerId, Integer userType) throws SalleException {
        if (userType == null || (userType != 2 && userType != 3)) {
            throw new SalleException(ErrorCodes.USER_TYPE_CENTER_NOT_VALID);
        }
        int year = academicStateService.getVisibleYear();

        boolean exists = userCenterRepo.existsByUser_IdAndCenter_IdAndYearAndDeletedAtIsNullAndUserType(
                userId, centerId, year, userType
        );

        if (exists) {
            throw new SalleException(ErrorCodes.USER_TYPE_CENTER_EXISTS);
        }

        UserSalle user = userRepo.findById(userId)
                .orElseThrow(() -> new SalleException(ErrorCodes.USER_NOT_FOUND));
        Center center = centerRepo.findById(centerId)
                .orElseThrow(() -> new SalleException(ErrorCodes.CENTER_NOT_FOUND));

        UserCenter saved = UserCenter.builder()
                .user(user)
                .center(center)
                .userType(userType) // 2=GROUP_LEADER, 3=PASTORAL_DELEGATE
                .year(year)
                .build();

        return userCenterRepo.save(saved);
    }

    @Transactional
    public void softDelete(Long userCenterId) throws SalleException {
        UserCenter uc = userCenterRepo.findById(userCenterId)
                .orElseThrow(() -> new SalleException(ErrorCodes.USER_CENTER_NOT_FOUND));
        uc.setDeletedAt(LocalDateTime.now());
        userCenterRepo.save(uc);
    }

    @Transactional
    public UserCenter updateCenterRole(Long userCenterId, Integer userType) throws SalleException {
        if (userType == null || (userType != 2 && userType != 3)) {
            throw new SalleException(ErrorCodes.USER_TYPE_CENTER_NOT_VALID);
        }

        UserCenter uc = userCenterRepo.findByIdAndDeletedAtIsNull(userCenterId)
                .orElseThrow(() -> new SalleException(ErrorCodes.USER_CENTER_NOT_FOUND));

        int year = academicStateService.getVisibleYear();
        if (uc.getYear() != year) {
            throw new SalleException(ErrorCodes.USER_CENTER_NOT_FOUND);
        }

        if (uc.getUserType() != null && uc.getUserType().intValue() == userType.intValue()) {
            return uc;
        }

        boolean duplicate = userCenterRepo.existsByUser_IdAndCenter_IdAndYearAndDeletedAtIsNullAndUserType(
                uc.getUser().getId(), uc.getCenter().getId(), year, userType
        );

        if (duplicate) {
            throw new SalleException(ErrorCodes.USER_TYPE_CENTER_EXISTS);
        }

        uc.setUserType(userType);
        return userCenterRepo.save(uc);
    }

}
