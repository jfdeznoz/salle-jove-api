package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.WeeklySessionBehaviorWarning;
import com.sallejoven.backend.repository.projection.GroupWarningTotalsProjection;
import com.sallejoven.backend.repository.projection.WarningTotalsProjection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WeeklySessionBehaviorWarningRepository extends JpaRepository<WeeklySessionBehaviorWarning, UUID> {

    @Query("""
        select warning
        from WeeklySessionBehaviorWarning warning
        where warning.weeklySessionUser.uuid = :sessionUserUuid
    """)
    Optional<WeeklySessionBehaviorWarning> findByWeeklySessionUserUuidIncludingDeleted(@Param("sessionUserUuid") UUID sessionUserUuid);

    @Query("""
        select warning
        from WeeklySessionBehaviorWarning warning
        where warning.weeklySessionUser.uuid = :sessionUserUuid
          and warning.deletedAt is null
    """)
    Optional<WeeklySessionBehaviorWarning> findActiveByWeeklySessionUserUuid(@Param("sessionUserUuid") UUID sessionUserUuid);

    @Query("""
        select coalesce(sum(case when warning.warningType = com.sallejoven.backend.model.enums.WeeklySessionWarningType.YELLOW then 1 else 0 end), 0) as yellowCount,
               coalesce(sum(case when warning.warningType = com.sallejoven.backend.model.enums.WeeklySessionWarningType.RED then 1 else 0 end), 0) as redCount
        from WeeklySessionBehaviorWarning warning
        join warning.weeklySessionUser wsu
        join wsu.weeklySession ws
        where ws.uuid = :sessionUuid
          and warning.deletedAt is null
          and wsu.deletedAt is null
          and ws.deletedAt is null
    """)
    WarningTotalsProjection findSessionWarningTotals(@Param("sessionUuid") UUID sessionUuid);

    @Query("""
        select coalesce(sum(case when warning.warningType = com.sallejoven.backend.model.enums.WeeklySessionWarningType.YELLOW then 1 else 0 end), 0) as yellowCount,
               coalesce(sum(case when warning.warningType = com.sallejoven.backend.model.enums.WeeklySessionWarningType.RED then 1 else 0 end), 0) as redCount
        from WeeklySessionBehaviorWarning warning
        join warning.weeklySessionUser wsu
        join wsu.weeklySession ws
        join wsu.user u
        where u.uuid = :userUuid
          and warning.deletedAt is null
          and wsu.deletedAt is null
          and ws.deletedAt is null
          and ws.sessionDateTime >= :academicYearStart
          and ws.sessionDateTime < :nextAcademicYearStart
    """)
    WarningTotalsProjection findUserWarningTotals(@Param("userUuid") UUID userUuid,
                                                  @Param("academicYearStart") java.time.LocalDateTime academicYearStart,
                                                  @Param("nextAcademicYearStart") java.time.LocalDateTime nextAcademicYearStart);

    @Query("""
        select ws.uuid as referenceUuid,
               coalesce(sum(case when warning.warningType = com.sallejoven.backend.model.enums.WeeklySessionWarningType.YELLOW then 1 else 0 end), 0) as yellowCount,
               coalesce(sum(case when warning.warningType = com.sallejoven.backend.model.enums.WeeklySessionWarningType.RED then 1 else 0 end), 0) as redCount
        from WeeklySessionBehaviorWarning warning
        join warning.weeklySessionUser wsu
        join wsu.weeklySession ws
        where ws.group.uuid = :groupUuid
          and warning.deletedAt is null
          and wsu.deletedAt is null
          and ws.deletedAt is null
          and exists (
                select 1
                from UserGroup ug
                where ug.user.uuid = wsu.user.uuid
                  and ug.group.uuid = ws.group.uuid
                  and ug.year = :year
                  and ug.deletedAt is null
          )
        group by ws.uuid
    """)
    List<WarningTotalsProjection> findGroupSessionWarningTotals(@Param("groupUuid") UUID groupUuid,
                                                                @Param("year") Integer year);

    @Query("""
        select u.uuid as referenceUuid,
               coalesce(sum(case when warning.warningType = com.sallejoven.backend.model.enums.WeeklySessionWarningType.YELLOW then 1 else 0 end), 0) as yellowCount,
               coalesce(sum(case when warning.warningType = com.sallejoven.backend.model.enums.WeeklySessionWarningType.RED then 1 else 0 end), 0) as redCount
        from WeeklySessionBehaviorWarning warning
        join warning.weeklySessionUser wsu
        join wsu.weeklySession ws
        join wsu.user u
        where ws.group.uuid = :groupUuid
          and warning.deletedAt is null
          and wsu.deletedAt is null
          and ws.deletedAt is null
          and exists (
                select 1
                from UserGroup ug
                where ug.user.uuid = u.uuid
                  and ug.group.uuid = ws.group.uuid
                  and ug.year = :year
                  and ug.deletedAt is null
          )
        group by u.uuid
    """)
    List<WarningTotalsProjection> findGroupMemberWarningTotals(@Param("groupUuid") UUID groupUuid,
                                                               @Param("year") Integer year);

    @Query("""
        select ws.group.uuid as groupUuid,
               ws.group.stage as stage,
               coalesce(sum(case when warning.warningType = com.sallejoven.backend.model.enums.WeeklySessionWarningType.YELLOW then 1 else 0 end), 0) as yellowCount,
               coalesce(sum(case when warning.warningType = com.sallejoven.backend.model.enums.WeeklySessionWarningType.RED then 1 else 0 end), 0) as redCount
        from WeeklySessionBehaviorWarning warning
        join warning.weeklySessionUser wsu
        join wsu.weeklySession ws
        where ws.group.center.uuid = :centerUuid
          and warning.deletedAt is null
          and wsu.deletedAt is null
          and ws.deletedAt is null
          and exists (
                select 1
                from UserGroup ug
                where ug.user.uuid = wsu.user.uuid
                  and ug.group.uuid = ws.group.uuid
                  and ug.year = :year
                  and ug.deletedAt is null
          )
        group by ws.group.uuid, ws.group.stage
    """)
    List<GroupWarningTotalsProjection> findCenterGroupWarningTotals(@Param("centerUuid") UUID centerUuid,
                                                                    @Param("year") Integer year);

    @Query("""
        select coalesce(sum(case when warning.warningType = com.sallejoven.backend.model.enums.WeeklySessionWarningType.YELLOW then 1 else 0 end), 0) as yellowCount,
               coalesce(sum(case when warning.warningType = com.sallejoven.backend.model.enums.WeeklySessionWarningType.RED then 1 else 0 end), 0) as redCount
        from WeeklySessionBehaviorWarning warning
        join warning.weeklySessionUser wsu
        join wsu.weeklySession ws
        where ws.group.center.uuid = :centerUuid
          and warning.deletedAt is null
          and wsu.deletedAt is null
          and ws.deletedAt is null
          and exists (
                select 1
                from UserGroup ug
                where ug.user.uuid = wsu.user.uuid
                  and ug.group.uuid = ws.group.uuid
                  and ug.year = :year
                  and ug.deletedAt is null
          )
    """)
    WarningTotalsProjection findCenterWarningTotals(@Param("centerUuid") UUID centerUuid,
                                                    @Param("year") Integer year);

    @Query("""
        select coalesce(sum(case when warning.warningType = com.sallejoven.backend.model.enums.WeeklySessionWarningType.YELLOW then 1 else 0 end), 0) as yellowCount,
               coalesce(sum(case when warning.warningType = com.sallejoven.backend.model.enums.WeeklySessionWarningType.RED then 1 else 0 end), 0) as redCount
        from WeeklySessionBehaviorWarning warning
        join warning.weeklySessionUser wsu
        join wsu.weeklySession ws
        where warning.deletedAt is null
          and wsu.deletedAt is null
          and ws.deletedAt is null
          and exists (
                select 1
                from UserGroup ug
                where ug.user.uuid = wsu.user.uuid
                  and ug.group.uuid = ws.group.uuid
                  and ug.year = :year
                  and ug.deletedAt is null
          )
    """)
    WarningTotalsProjection findGlobalWarningTotals(@Param("year") Integer year);
}
