package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.WeeklySessionUser;
import com.sallejoven.backend.repository.projection.AttendanceTotalsProjection;
import com.sallejoven.backend.repository.projection.CenterGroupSessionRateProjection;
import com.sallejoven.backend.repository.projection.GroupMemberAttendanceProjection;
import com.sallejoven.backend.repository.projection.GroupSessionSummaryProjection;
import com.sallejoven.backend.repository.projection.UserRecentSessionProjection;
import com.sallejoven.backend.repository.projection.UserSessionAttendanceStatsProjection;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WeeklySessionUserRepository extends JpaRepository<WeeklySessionUser, UUID> {

    interface AttendanceCountProjection {
        Long getAttendanceCount();
        Long getTotalCount();
    }

    @Query("""
        SELECT DISTINCT wsu
        FROM WeeklySessionUser wsu
        JOIN UserGroup ug
          ON ug.user.uuid = wsu.user.uuid
         AND ug.group.uuid = :groupUuid
         AND ug.year = :year
         AND ug.deletedAt IS NULL
        WHERE wsu.weeklySession.uuid = :sessionUuid
          AND wsu.deletedAt IS NULL
          AND wsu.user.deletedAt IS NULL
    """)
    List<WeeklySessionUser> findBySessionUuidAndGroupUuid(@Param("sessionUuid") UUID sessionUuid,
                                                          @Param("groupUuid") UUID groupUuid,
                                                          @Param("year") Integer year);

    @Query("""
        SELECT DISTINCT wsu
        FROM WeeklySessionUser wsu
        JOIN UserGroup ug
          ON ug.user.uuid = wsu.user.uuid
         AND ug.group.uuid = wsu.weeklySession.group.uuid
         AND ug.year = :year
         AND ug.deletedAt IS NULL
        JOIN ug.group g
        WHERE wsu.weeklySession.uuid = :sessionUuid
          AND wsu.deletedAt IS NULL
          AND wsu.user.deletedAt IS NULL
        ORDER BY g.center.name ASC, g.stage ASC
    """)
    List<WeeklySessionUser> findBySessionUuidOrdered(@Param("sessionUuid") UUID sessionUuid,
                                                     @Param("year") Integer year);

    @Query("""
        SELECT wsu
        FROM WeeklySessionUser wsu
        WHERE wsu.uuid = :uuid
          AND wsu.deletedAt IS NULL
    """)
    Optional<WeeklySessionUser> findById(@Param("uuid") UUID uuid);

    @Query("""
        SELECT wsu
        FROM WeeklySessionUser wsu
        JOIN UserGroup ug
          ON ug.user.uuid = wsu.user.uuid
         AND ug.group.uuid = :groupUuid
         AND ug.year = :year
         AND ug.deletedAt IS NULL
        WHERE wsu.weeklySession.uuid = :sessionUuid
          AND wsu.user.uuid = :userUuid
          AND wsu.deletedAt IS NULL
          AND wsu.user.deletedAt IS NULL
    """)
    Optional<WeeklySessionUser> findBySessionUserAndGroup(@Param("sessionUuid") UUID sessionUuid,
                                                          @Param("userUuid") UUID userUuid,
                                                          @Param("groupUuid") UUID groupUuid,
                                                          @Param("year") Integer year);

    @Query("""
        SELECT COALESCE(SUM(CASE WHEN wsu.status = 1 THEN 1 ELSE 0 END), 0) AS attendanceCount,
               COUNT(wsu.uuid) AS totalCount
        FROM WeeklySessionUser wsu
        JOIN UserGroup ug
          ON ug.user.uuid = wsu.user.uuid
         AND ug.group.uuid = wsu.weeklySession.group.uuid
         AND ug.year = :year
         AND ug.deletedAt IS NULL
        WHERE wsu.weeklySession.uuid = :sessionUuid
          AND ug.userType = 0
          AND wsu.deletedAt IS NULL
    """)
    AttendanceCountProjection countAttendanceBySessionUuid(@Param("sessionUuid") UUID sessionUuid,
                                                           @Param("year") Integer year);

    @Modifying
    @Query("""
        UPDATE WeeklySessionUser wsu
           SET wsu.deletedAt = CURRENT_TIMESTAMP
         WHERE wsu.weeklySession.uuid = :sessionUuid
           AND wsu.deletedAt IS NULL
    """)
    void softDeleteBySessionUuid(@Param("sessionUuid") UUID sessionUuid);

    @Query("""
        SELECT wsu.user.uuid
        FROM WeeklySessionUser wsu
        WHERE wsu.weeklySession.uuid = :sessionUuid
    """)
    List<UUID> findUserUuidsBySession(@Param("sessionUuid") UUID sessionUuid);

    @Modifying
    @Query("""
        UPDATE WeeklySessionUser wsu
           SET wsu.status = :status
         WHERE wsu.weeklySession.uuid = :sessionUuid
           AND wsu.user.uuid = :userUuid
           AND wsu.user.uuid IN (
                SELECT ug.user.uuid
                FROM UserGroup ug
                WHERE ug.user.uuid = :userUuid
                  AND ug.group.uuid = :groupUuid
                  AND ug.year = :year
                  AND ug.deletedAt IS NULL
           )
           AND wsu.deletedAt IS NULL
    """)
    int updateStatusBySessionUserAndGroup(@Param("sessionUuid") UUID sessionUuid,
                                          @Param("userUuid") UUID userUuid,
                                          @Param("groupUuid") UUID groupUuid,
                                          @Param("year") Integer year,
                                          @Param("status") int status);

    @Modifying
    @Query("""
        UPDATE WeeklySessionUser wsu
           SET wsu.deletedAt = CURRENT_TIMESTAMP
         WHERE wsu.weeklySession.uuid = :sessionUuid
           AND wsu.user.uuid IN :userUuids
           AND wsu.deletedAt IS NULL
    """)
    int softDeleteBySessionUuidAndUserUuids(@Param("sessionUuid") UUID sessionUuid,
                                            @Param("userUuids") Collection<UUID> userUuids);

    @Modifying
    @Query("UPDATE WeeklySessionUser wsu SET wsu.deletedAt = :when WHERE wsu.user.uuid IN :userUuids AND wsu.deletedAt IS NULL")
    int softDeleteByUserUuidIn(@Param("userUuids") List<UUID> userUuids, @Param("when") LocalDateTime when);

    @Query("""
        SELECT COUNT(wsu.uuid) AS total,
               COALESCE(SUM(CASE WHEN wsu.status = 1 THEN 1 ELSE 0 END), 0) AS attended,
               COALESCE(SUM(CASE WHEN wsu.justified = true THEN 1 ELSE 0 END), 0) AS justified
        FROM WeeklySessionUser wsu
        JOIN wsu.user u
        JOIN wsu.weeklySession ws
        WHERE u.uuid = :userUuid
          AND wsu.deletedAt IS NULL
          AND u.deletedAt IS NULL
          AND ws.deletedAt IS NULL
          AND EXISTS (
                SELECT 1
                FROM UserGroup ug
                WHERE ug.user.uuid = u.uuid
                  AND ug.group.uuid = ws.group.uuid
                  AND ug.year = :year
                  AND ug.deletedAt IS NULL
          )
    """)
    UserSessionAttendanceStatsProjection findUserAttendanceStats(@Param("userUuid") UUID userUuid,
                                                                 @Param("year") Integer year);

    @Query("""
        SELECT ws.sessionDateTime AS date,
               ws.title AS title,
               vs.title AS vitalSituationTitle,
               vss.title AS vitalSituationSessionTitle,
               CASE WHEN wsu.status = 1 THEN true ELSE false END AS attended,
               wsu.justified AS justified
        FROM WeeklySessionUser wsu
        JOIN wsu.weeklySession ws
        JOIN ws.vitalSituationSession vss
        JOIN vss.vitalSituation vs
        JOIN wsu.user u
        WHERE u.uuid = :userUuid
          AND wsu.deletedAt IS NULL
          AND u.deletedAt IS NULL
          AND ws.deletedAt IS NULL
          AND EXISTS (
                SELECT 1
                FROM UserGroup ug
                WHERE ug.user.uuid = u.uuid
                  AND ug.group.uuid = ws.group.uuid
                  AND ug.year = :year
                  AND ug.deletedAt IS NULL
          )
        ORDER BY ws.sessionDateTime DESC, wsu.uuid DESC
    """)
    List<UserRecentSessionProjection> findRecentSessionsByUser(@Param("userUuid") UUID userUuid,
                                                               @Param("year") Integer year,
                                                               Pageable pageable);

    @Query("""
        SELECT ws.uuid AS uuid,
               ws.sessionDateTime AS date,
               ws.title AS title,
               vs.title AS vitalSituationTitle,
               ws.content AS content,
               COALESCE(SUM(CASE WHEN wsu.status = 1 THEN 1 ELSE 0 END), 0) AS attendanceCount,
               COUNT(wsu.uuid) AS totalCount
        FROM WeeklySessionUser wsu
        JOIN wsu.weeklySession ws
        JOIN ws.vitalSituationSession vss
        JOIN vss.vitalSituation vs
        JOIN UserGroup ug
          ON ug.user.uuid = wsu.user.uuid
         AND ug.group.uuid = ws.group.uuid
         AND ug.year = :year
         AND ug.deletedAt IS NULL
        JOIN wsu.user u
        WHERE ws.group.uuid = :groupUuid
          AND ug.userType = 0
          AND wsu.deletedAt IS NULL
          AND u.deletedAt IS NULL
          AND ws.deletedAt IS NULL
        GROUP BY ws.uuid, ws.sessionDateTime, ws.title, vs.title, ws.content
        ORDER BY ws.sessionDateTime DESC
    """)
    List<GroupSessionSummaryProjection> findGroupSessionSummaries(@Param("groupUuid") UUID groupUuid,
                                                                  @Param("year") Integer year);

    @Query("""
        SELECT u.uuid AS userUuid,
               u.name AS name,
               u.lastName AS lastName,
               COALESCE(SUM(CASE WHEN wsu.status = 1 THEN 1 ELSE 0 END), 0) AS sessionsAttended,
               COUNT(wsu.uuid) AS sessionsTotal
        FROM UserGroup ug
        JOIN ug.user u
        LEFT JOIN WeeklySessionUser wsu
               ON wsu.user.uuid = u.uuid
              AND wsu.deletedAt IS NULL
              AND wsu.weeklySession.deletedAt IS NULL
              AND wsu.weeklySession.group.uuid = ug.group.uuid
        WHERE ug.group.uuid = :groupUuid
          AND ug.year = :year
          AND ug.userType = 0
          AND ug.deletedAt IS NULL
          AND u.deletedAt IS NULL
        GROUP BY u.uuid, u.name, u.lastName
        ORDER BY u.lastName ASC, u.name ASC
    """)
    List<GroupMemberAttendanceProjection> findGroupMemberAttendance(@Param("groupUuid") UUID groupUuid,
                                                                    @Param("year") Integer year);

    @Query("""
        SELECT g.uuid AS groupUuid,
               g.stage AS stage,
               ws.uuid AS sessionUuid,
               COALESCE(SUM(CASE WHEN wsu.status = 1 THEN 1 ELSE 0 END), 0) AS attendedCount,
               COUNT(wsu.uuid) AS totalCount
        FROM WeeklySessionUser wsu
        JOIN wsu.weeklySession ws
        JOIN UserGroup ug
          ON ug.user.uuid = wsu.user.uuid
         AND ug.group.uuid = ws.group.uuid
         AND ug.year = :year
         AND ug.deletedAt IS NULL
        JOIN ug.group g
        JOIN ug.user u
        WHERE g.center.uuid = :centerUuid
          AND ug.userType = 0
          AND wsu.deletedAt IS NULL
          AND u.deletedAt IS NULL
          AND ws.deletedAt IS NULL
        GROUP BY g.uuid, g.stage, ws.uuid
        ORDER BY g.stage ASC, ws.uuid ASC
    """)
    List<CenterGroupSessionRateProjection> findCenterGroupSessionRates(@Param("centerUuid") UUID centerUuid,
                                                                       @Param("year") Integer year);

    @Query("""
        SELECT COUNT(wsu.uuid) AS total,
               COALESCE(SUM(CASE WHEN wsu.status = 1 THEN 1 ELSE 0 END), 0) AS attended
        FROM WeeklySessionUser wsu
        JOIN UserGroup ug
          ON ug.user.uuid = wsu.user.uuid
         AND ug.group.uuid = wsu.weeklySession.group.uuid
         AND ug.year = :year
         AND ug.deletedAt IS NULL
        JOIN ug.group g
        JOIN ug.user u
        JOIN wsu.weeklySession ws
        WHERE g.center.uuid = :centerUuid
          AND ug.userType = 0
          AND wsu.deletedAt IS NULL
          AND u.deletedAt IS NULL
          AND ws.deletedAt IS NULL
    """)
    AttendanceTotalsProjection findCenterAttendanceTotals(@Param("centerUuid") UUID centerUuid,
                                                          @Param("year") Integer year);

    @Query("""
        SELECT COUNT(wsu.uuid) AS total,
               COALESCE(SUM(CASE WHEN wsu.status = 1 THEN 1 ELSE 0 END), 0) AS attended
        FROM WeeklySessionUser wsu
        JOIN UserGroup ug
          ON ug.user.uuid = wsu.user.uuid
         AND ug.group.uuid = wsu.weeklySession.group.uuid
         AND ug.year = :year
         AND ug.deletedAt IS NULL
        JOIN ug.user u
        JOIN wsu.weeklySession ws
        WHERE ug.userType = 0
          AND wsu.deletedAt IS NULL
          AND u.deletedAt IS NULL
          AND ws.deletedAt IS NULL
    """)
    AttendanceTotalsProjection findGlobalAttendanceTotals(@Param("year") Integer year);

    @Query("""
        SELECT COUNT(DISTINCT ws.uuid)
        FROM WeeklySessionUser wsu
        JOIN wsu.weeklySession ws
        JOIN UserGroup ug
          ON ug.user.uuid = wsu.user.uuid
         AND ug.group.uuid = ws.group.uuid
         AND ug.year = :year
         AND ug.deletedAt IS NULL
        WHERE wsu.deletedAt IS NULL
          AND ws.deletedAt IS NULL
    """)
    Long countDistinctSessionsByYear(@Param("year") Integer year);

    @Query("select wsu from WeeklySessionUser wsu where wsu.user.uuid = :userUuid")
    List<WeeklySessionUser> findAllByUserIncludingDeleted(@Param("userUuid") UUID userUuid);

    @Modifying
    @Query("""
        update WeeklySessionUser wsu
           set wsu.deletedAt = null
         where wsu.user.uuid = :userUuid
           and wsu.deletedAt is not null
    """)
    int reactivateByUser(@Param("userUuid") UUID userUuid);

    @Query("""
        select wsu from WeeklySessionUser wsu
         where wsu.weeklySession.uuid = :sessionUuid
           and wsu.user.uuid = :userUuid
           and wsu.deletedAt is null
    """)
    Optional<WeeklySessionUser> findActiveBySessionAndUser(@Param("sessionUuid") UUID sessionUuid,
                                                            @Param("userUuid") UUID userUuid);
}
