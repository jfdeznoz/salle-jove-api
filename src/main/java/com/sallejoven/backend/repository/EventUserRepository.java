package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.EventUser;
import com.sallejoven.backend.repository.projection.AttendanceTotalsProjection;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventUserRepository extends JpaRepository<EventUser, UUID> {

    @Query("""
        SELECT DISTINCT eu
        FROM EventUser eu
        JOIN UserGroup ug
          ON ug.user.uuid = eu.user.uuid
         AND ug.group.uuid = :groupUuid
         AND ug.year = :year
         AND ug.deletedAt IS NULL
        WHERE eu.event.uuid = :eventUuid
          AND eu.deletedAt IS NULL
          AND eu.user.deletedAt IS NULL
    """)
    List<EventUser> findByEventUuidAndGroupUuid(@Param("eventUuid") UUID eventUuid,
                                                @Param("groupUuid") UUID groupUuid,
                                                @Param("year") Integer year);

    @Query("""
        SELECT DISTINCT eu
        FROM EventUser eu
        JOIN UserGroup ug
          ON ug.user.uuid = eu.user.uuid
         AND ug.year = :year
         AND ug.deletedAt IS NULL
        JOIN ug.group g
        WHERE eu.event.uuid = :eventUuid
          AND eu.deletedAt IS NULL
          AND eu.user.deletedAt IS NULL
        ORDER BY g.center.name ASC, g.stage ASC
    """)
    List<EventUser> findByEventUuidOrdered(@Param("eventUuid") UUID eventUuid,
                                           @Param("year") Integer year);

    @Query("""
        SELECT eu
        FROM EventUser eu
        WHERE eu.uuid = :uuid
          AND eu.deletedAt IS NULL
    """)
    Optional<EventUser> findById(@Param("uuid") UUID uuid);

    @Modifying
    @Query("""
        UPDATE EventUser eu
           SET eu.deletedAt = CURRENT_TIMESTAMP
         WHERE eu.event.uuid = :eventUuid
           AND eu.deletedAt IS NULL
    """)
    void softDeleteByEventUuid(@Param("eventUuid") UUID eventUuid);

    @Query("""
        SELECT eu.user.uuid
        FROM EventUser eu
        WHERE eu.event.uuid = :eventUuid
    """)
    List<UUID> findUserUuidsByEvent(@Param("eventUuid") UUID eventUuid);

    @Modifying
    @Query("""
        UPDATE EventUser eu
           SET eu.status = :status
         WHERE eu.event.uuid = :eventUuid
           AND eu.user.uuid = :userUuid
           AND eu.user.uuid IN (
                SELECT ug.user.uuid
                FROM UserGroup ug
                WHERE ug.user.uuid = :userUuid
                  AND ug.group.uuid = :groupUuid
                  AND ug.year = :year
                  AND ug.deletedAt IS NULL
           )
           AND eu.deletedAt IS NULL
    """)
    int updateStatusByEventUserAndGroup(@Param("eventUuid") UUID eventUuid,
                                        @Param("userUuid") UUID userUuid,
                                        @Param("groupUuid") UUID groupUuid,
                                        @Param("year") Integer year,
                                        @Param("status") int status);

    @Modifying
    @Query("""
        UPDATE EventUser eu
           SET eu.deletedAt = CURRENT_TIMESTAMP
         WHERE eu.event.uuid = :eventUuid
           AND eu.user.uuid IN :userUuids
           AND eu.deletedAt IS NULL
    """)
    int softDeleteByEventUuidAndUserUuids(@Param("eventUuid") UUID eventUuid,
                                          @Param("userUuids") Collection<UUID> userUuids);

    @Modifying
    @Query("UPDATE EventUser eu SET eu.deletedAt = :when WHERE eu.user.uuid IN :userUuids AND eu.deletedAt IS NULL")
    int softDeleteByUserUuidIn(@Param("userUuids") List<UUID> userUuids, @Param("when") LocalDateTime when);

    @Query("""
    select eu.uuid
    from EventUser eu
    where eu.event.uuid = :eventUuid
      and eu.deletedAt is null
      and eu.status = 1
      and eu.user.deletedAt is null
""")
    List<UUID> findConfirmedUuids(@Param("eventUuid") UUID eventUuid);

    @Query("""
        select distinct eu
        from EventUser eu
        join fetch eu.user u
        left join fetch u.groups ug
        left join fetch ug.group g
        left join fetch g.center
        where eu.uuid in :uuids
          and eu.deletedAt is null
    """)
    List<EventUser> findByUuidInFetchForReport(@Param("uuids") List<UUID> uuids);

    @Query("""
        SELECT COUNT(eu.uuid) AS total,
               COALESCE(SUM(CASE WHEN eu.status = 1 THEN 1 ELSE 0 END), 0) AS attended
        FROM EventUser eu
        JOIN eu.user u
        JOIN eu.event e
        WHERE u.uuid = :userUuid
          AND eu.deletedAt IS NULL
          AND u.deletedAt IS NULL
          AND e.deletedAt IS NULL
          AND EXISTS (
                SELECT 1
                FROM UserGroup ug
                JOIN EventGroup eg
                  ON eg.groupSalle.uuid = ug.group.uuid
                WHERE ug.user.uuid = u.uuid
                  AND ug.year = :year
                  AND ug.deletedAt IS NULL
                  AND eg.event.uuid = e.uuid
                  AND eg.deletedAt IS NULL
          )
    """)
    AttendanceTotalsProjection findUserAttendanceStats(@Param("userUuid") UUID userUuid,
                                                       @Param("year") Integer year);

    @Query("""
        SELECT COUNT(eu.uuid) AS total,
               COALESCE(SUM(CASE WHEN eu.status = 1 THEN 1 ELSE 0 END), 0) AS attended
        FROM EventUser eu
        JOIN eu.user u
        JOIN eu.event e
        WHERE eu.deletedAt IS NULL
          AND u.deletedAt IS NULL
          AND e.deletedAt IS NULL
          AND EXISTS (
                SELECT 1
                FROM UserGroup ug
                JOIN EventGroup eg
                  ON eg.groupSalle.uuid = ug.group.uuid
                WHERE ug.user.uuid = u.uuid
                  AND ug.year = :year
                  AND ug.deletedAt IS NULL
                  AND ug.group.center.uuid = :centerUuid
                  AND eg.event.uuid = e.uuid
                  AND eg.deletedAt IS NULL
          )
    """)
    AttendanceTotalsProjection findCenterAttendanceTotals(@Param("centerUuid") UUID centerUuid,
                                                          @Param("year") Integer year);

    @Query("""
        SELECT COUNT(eu.uuid) AS total,
               COALESCE(SUM(CASE WHEN eu.status = 1 THEN 1 ELSE 0 END), 0) AS attended
        FROM EventUser eu
        JOIN eu.user u
        JOIN eu.event e
        WHERE eu.deletedAt IS NULL
          AND u.deletedAt IS NULL
          AND e.deletedAt IS NULL
          AND EXISTS (
                SELECT 1
                FROM UserGroup ug
                JOIN EventGroup eg
                  ON eg.groupSalle.uuid = ug.group.uuid
                WHERE ug.user.uuid = u.uuid
                  AND ug.year = :year
                  AND ug.deletedAt IS NULL
                  AND eg.event.uuid = e.uuid
                  AND eg.deletedAt IS NULL
          )
    """)
    AttendanceTotalsProjection findGlobalAttendanceTotals(@Param("year") Integer year);

    @Query("""
        SELECT COUNT(DISTINCT e.uuid)
        FROM EventUser eu
        JOIN eu.event e
        JOIN eu.user u
        WHERE eu.deletedAt IS NULL
          AND u.deletedAt IS NULL
          AND e.deletedAt IS NULL
          AND EXISTS (
                SELECT 1
                FROM UserGroup ug
                JOIN EventGroup eg
                  ON eg.groupSalle.uuid = ug.group.uuid
                WHERE ug.user.uuid = u.uuid
                  AND ug.year = :year
                  AND ug.deletedAt IS NULL
                  AND eg.event.uuid = e.uuid
                  AND eg.deletedAt IS NULL
          )
    """)
    Long countDistinctEventsByYear(@Param("year") Integer year);

    @Query("select eu from EventUser eu where eu.user.uuid = :userUuid")
    List<EventUser> findAllByUserIncludingDeleted(@Param("userUuid") UUID userUuid);

    @Query("""
        select eu from EventUser eu
         where eu.event.uuid = :eventUuid
           and eu.user.uuid = :userUuid
    """)
    Optional<EventUser> findByEventAndUserIncludingDeleted(@Param("eventUuid") UUID eventUuid,
                                                           @Param("userUuid") UUID userUuid);

    @Modifying
    @Query("""
        update EventUser eu
           set eu.deletedAt = null
         where eu.user.uuid = :userUuid
           and eu.deletedAt is not null
    """)
    int reactivateByUser(@Param("userUuid") UUID userUuid);

    @Query("""
        select eu from EventUser eu
         where eu.event.uuid = :eventUuid
           and eu.user.uuid = :userUuid
           and eu.deletedAt is null
    """)
    Optional<EventUser> findActiveByEventAndUser(@Param("eventUuid") UUID eventUuid,
                                                 @Param("userUuid") UUID userUuid);
}
