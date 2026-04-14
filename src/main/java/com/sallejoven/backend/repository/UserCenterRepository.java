package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.UserCenter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserCenterRepository extends JpaRepository<UserCenter, UUID> {

    default Optional<UserCenter> findByUuid(UUID uuid) {
        return findById(uuid);
    }

    List<UserCenter> findByUser_UuidAndYearAndDeletedAtIsNull(UUID userUuid, Integer year);

    boolean existsByUser_UuidAndCenter_UuidAndYearAndDeletedAtIsNullAndUserType(
            UUID userUuid, UUID centerUuid, Integer year, Integer userType
    );

    boolean existsByUser_UuidAndYearAndDeletedAtIsNullAndUserType(UUID userUuid, Integer year, Integer userType);

    List<UserCenter> findByCenter_UuidAndYearAndDeletedAtIsNull(UUID centerUuid, Integer year);

    Optional<UserCenter> findByUuidAndDeletedAtIsNull(UUID uuid);

    Optional<UserCenter> findByUser_UuidAndCenter_UuidAndYearAndDeletedAtIsNull(UUID userUuid,
                                                                                 UUID centerUuid,
                                                                                 Integer year);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM UserCenter uc WHERE uc.center.uuid = :centerUuid")
    void hardDeleteByCenterUuid(@Param("centerUuid") UUID centerUuid);

    @Query("select uc from UserCenter uc where uc.user.uuid = :userUuid")
    List<UserCenter> findAllByUserIncludingDeleted(@Param("userUuid") UUID userUuid);

    @Modifying
    @Query("""
        update UserCenter uc
           set uc.deletedAt = null
         where uc.user.uuid = :userUuid
           and uc.deletedAt is not null
    """)
    int reactivateByUser(@Param("userUuid") UUID userUuid);

    @Query("""
        select uc from UserCenter uc
         where uc.user.uuid = :userUuid
           and uc.center.uuid = :centerUuid
           and uc.year = :year
           and uc.deletedAt is null
    """)
    Optional<UserCenter> findActiveByUserCenterYear(@Param("userUuid") UUID userUuid,
                                                    @Param("centerUuid") UUID centerUuid,
                                                    @Param("year") Integer year);
}
