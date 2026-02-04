package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.WeeklySessionUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeeklySessionUserRepository extends JpaRepository<WeeklySessionUser, Long> {

    @Query("""
        SELECT wsu
        FROM WeeklySessionUser wsu
        JOIN wsu.userGroup ug
        JOIN ug.group g
        JOIN ug.user u
        WHERE wsu.weeklySession.id = :sessionId
          AND g.id = :groupId
          AND wsu.deletedAt IS NULL
          AND u.deletedAt IS NULL
    """)
    List<WeeklySessionUser> findBySessionIdAndGroupId(@Param("sessionId") Long sessionId,
                                                      @Param("groupId") Long groupId);

    @Query("""
        SELECT wsu
        FROM WeeklySessionUser wsu
        JOIN wsu.userGroup ug
        JOIN ug.user u
        JOIN ug.group g
        WHERE wsu.weeklySession.id = :sessionId
          AND wsu.deletedAt IS NULL
          AND u.deletedAt IS NULL
        ORDER BY g.center.name ASC, g.stage ASC
    """)
    List<WeeklySessionUser> findBySessionIdOrdered(@Param("sessionId") Long sessionId);

    @Query("""
        SELECT wsu
        FROM WeeklySessionUser wsu
        WHERE wsu.id = :id
          AND wsu.deletedAt IS NULL
    """)
    Optional<WeeklySessionUser> findById(@Param("id") Long id);

    @Modifying
    @Query("""
        UPDATE WeeklySessionUser wsu
           SET wsu.deletedAt = CURRENT_TIMESTAMP
         WHERE wsu.weeklySession.id = :sessionId
           AND wsu.deletedAt IS NULL
    """)
    void softDeleteBySessionId(@Param("sessionId") Long sessionId);

    @Query("""
        SELECT wsu.userGroup.id
        FROM WeeklySessionUser wsu
        WHERE wsu.weeklySession.id = :sessionId
    """)
    List<Long> findUserGroupIdsBySession(@Param("sessionId") Long sessionId);

    @Modifying
    @Query("""
        UPDATE WeeklySessionUser wsu
           SET wsu.status = :status
         WHERE wsu.weeklySession.id = :sessionId
           AND wsu.userGroup.id IN (
                SELECT ug.id
                FROM UserGroup ug
                WHERE ug.user.id  = :userId
                  AND ug.group.id = :groupId
           )
           AND wsu.deletedAt IS NULL
    """)
    int updateStatusBySessionUserAndGroup(@Param("sessionId") Long sessionId,
                                          @Param("userId") Long userId,
                                          @Param("groupId") Long groupId,
                                          @Param("status") int status);

    @Modifying
    @Query("""
        UPDATE WeeklySessionUser wsu
           SET wsu.deletedAt = CURRENT_TIMESTAMP
         WHERE wsu.weeklySession.id = :sessionId
           AND wsu.userGroup.id IN :userGroupIds
           AND wsu.deletedAt IS NULL
    """)
    int softDeleteBySessionIdAndUserGroupIds(@Param("sessionId") Long sessionId,
                                             @Param("userGroupIds") Collection<Long> userGroupIds);

    @Modifying
    @Query("UPDATE WeeklySessionUser wsu SET wsu.deletedAt = :when WHERE wsu.userGroup.id IN :ugIds AND wsu.deletedAt IS NULL")
    int softDeleteByUserGroupIdIn(@Param("ugIds") List<Long> ugIds, @Param("when") LocalDateTime when);
}

