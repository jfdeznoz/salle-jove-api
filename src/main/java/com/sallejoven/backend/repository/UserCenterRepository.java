package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.UserCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

import java.util.List;

public interface UserCenterRepository extends JpaRepository<UserCenter, Long> {

    List<UserCenter> findByUser_IdAndYearAndDeletedAtIsNull(Long userId, Integer year);

    boolean existsByUser_IdAndCenter_IdAndYearAndDeletedAtIsNullAndUserType(
            Long userId, Long centerId, Integer year, Integer userType
    );

    boolean existsByUser_IdAndYearAndDeletedAtIsNullAndUserType(Long userId, Integer year, Integer userType);

    List<UserCenter> findByCenter_IdAndYearAndDeletedAtIsNull(Long centerId, Integer year);

    Optional<UserCenter> findByIdAndDeletedAtIsNull(Long id);

    Optional<UserCenter> findByUser_IdAndCenter_IdAndYearAndDeletedAtIsNull(Long userId, Long centerId, Integer year);
}
