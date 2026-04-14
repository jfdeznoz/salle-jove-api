package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.UserCenter;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.enums.ErrorCodes;
import com.sallejoven.backend.repository.CenterRepository;
import com.sallejoven.backend.repository.UserCenterRepository;
import com.sallejoven.backend.repository.UserRepository;
import com.sallejoven.backend.utils.ReferenceParser;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserCenterService {

    private final UserCenterRepository userCenterRepo;
    private final UserRepository userRepo;
    private final CenterRepository centerRepo;
    private final AcademicStateService academicStateService;

    public List<UserCenter> findByUserForCurrentYear(UUID userUuid) {
        int year = academicStateService.getVisibleYear();
        return userCenterRepo.findByUser_UuidAndYearAndDeletedAtIsNull(userUuid, year);
    }

    public UserCenter findByReference(String reference) {
        return ReferenceParser.asUuid(reference)
                .flatMap(userCenterRepo::findByUuid)
                .orElseThrow(() -> new SalleException(ErrorCodes.USER_CENTER_NOT_FOUND));
    }

    public List<UserCenter> findActiveByCenterForCurrentYear(UUID centerUuid) {
        int year = academicStateService.getVisibleYear();
        return userCenterRepo.findByCenter_UuidAndYearAndDeletedAtIsNull(centerUuid, year);
    }

    @Transactional
    public UserCenter findByUserAndCenter(UUID userUuid, UUID centerUuid) {
        int year = academicStateService.getVisibleYear();
        return userCenterRepo.findByUser_UuidAndCenter_UuidAndYearAndDeletedAtIsNull(userUuid, centerUuid, year)
                .orElseThrow(() -> new SalleException(ErrorCodes.USER_CENTER_NOT_FOUND));
    }

    @Transactional
    public UserCenter addCenterRole(UUID userUuid, UUID centerUuid, Integer userType) {
        if (userType == null || (userType != 2 && userType != 3)) {
            throw new SalleException(ErrorCodes.USER_TYPE_CENTER_NOT_VALID);
        }
        int year = academicStateService.getVisibleYear();

        boolean exists = userCenterRepo.existsByUser_UuidAndCenter_UuidAndYearAndDeletedAtIsNullAndUserType(
                userUuid,
                centerUuid,
                year,
                userType
        );
        if (exists) {
            throw new SalleException(ErrorCodes.USER_TYPE_CENTER_EXISTS);
        }

        UserSalle user = userRepo.findById(userUuid)
                .orElseThrow(() -> new SalleException(ErrorCodes.USER_NOT_FOUND));
        Center center = centerRepo.findById(centerUuid)
                .orElseThrow(() -> new SalleException(ErrorCodes.CENTER_NOT_FOUND));

        UserCenter saved = UserCenter.builder()
                .user(user)
                .center(center)
                .userType(userType)
                .year(year)
                .build();

        return userCenterRepo.save(saved);
    }

    @Transactional
    public void softDelete(UUID userCenterUuid) {
        UserCenter userCenter = userCenterRepo.findById(userCenterUuid)
                .orElseThrow(() -> new SalleException(ErrorCodes.USER_CENTER_NOT_FOUND));
        userCenter.setDeletedAt(LocalDateTime.now());
        userCenterRepo.save(userCenter);
    }

    @Transactional
    public void hardDeleteAllForCenter(UUID centerUuid) {
        userCenterRepo.hardDeleteByCenterUuid(centerUuid);
    }

    @Transactional
    public UserCenter updateCenterRole(UUID userCenterUuid, Integer userType) {
        if (userType == null || (userType != 2 && userType != 3)) {
            throw new SalleException(ErrorCodes.USER_TYPE_CENTER_NOT_VALID);
        }

        UserCenter userCenter = userCenterRepo.findByUuidAndDeletedAtIsNull(userCenterUuid)
                .orElseThrow(() -> new SalleException(ErrorCodes.USER_CENTER_NOT_FOUND));

        int year = academicStateService.getVisibleYear();
        if (userCenter.getYear() != year) {
            throw new SalleException(ErrorCodes.USER_CENTER_NOT_FOUND);
        }

        if (userType.equals(userCenter.getUserType())) {
            return userCenter;
        }

        boolean duplicate = userCenterRepo.existsByUser_UuidAndCenter_UuidAndYearAndDeletedAtIsNullAndUserType(
                userCenter.getUser().getUuid(),
                userCenter.getCenter().getUuid(),
                year,
                userType
        );
        if (duplicate) {
            throw new SalleException(ErrorCodes.USER_TYPE_CENTER_EXISTS);
        }

        userCenter.setUserType(userType);
        return userCenterRepo.save(userCenter);
    }
}
