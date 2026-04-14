package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.repository.projection.SeguroRow;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserGroupRepository extends JpaRepository<UserGroup, UUID> {

    @Query("""
        select ug
        from UserGroup ug
        where ug.uuid = :uuid
          and ug.deletedAt is null
    """)
    Optional<UserGroup> findByIdAndDeletedAtIsNull(@Param("uuid") UUID uuid);

    @Query("""
        select ug
        from UserGroup ug
        where ug.group.uuid in :groupUuids
          and ug.year = :year
          and ug.deletedAt is null
    """)
    List<UserGroup> findByGroupUuidsAndYear(@Param("groupUuids") Collection<UUID> groupUuids,
                                            @Param("year") Integer year);

    @Query("""
        select ug
        from UserGroup ug
        where ug.group.uuid = :groupUuid
          and ug.year = :year
          and ug.deletedAt is null
    """)
    List<UserGroup> findByGroupUuidAndYear(@Param("groupUuid") UUID groupUuid,
                                           @Param("year") Integer year);

    @Query("""
        select ug
        from UserGroup ug
        join ug.group g
        where g.stage in :stages
          and ug.year = :year
          and ug.deletedAt is null
    """)
    List<UserGroup> findByGroupStagesAndYear(@Param("stages") Collection<Integer> stages,
                                             @Param("year") Integer year);

    @Query("""
        select ug
        from UserGroup ug
        join ug.group g
        where g.center.uuid = :centerUuid
          and g.stage in :stages
          and ug.year = :year
          and ug.deletedAt is null
    """)
    List<UserGroup> findByCenterAndGroupStagesAndYear(@Param("centerUuid") UUID centerUuid,
                                                      @Param("stages") Collection<Integer> stages,
                                                      @Param("year") Integer year);

    @Query(value = """
        select distinct on (ug.user_uuid) ug.*
        from user_group ug
        join group_salle g on g.uuid = ug.group_uuid
        where g.center_uuid = :centerUuid
          and ug.user_type = any(:types)
          and ug.year = :year
          and ug.deleted_at is null
        order by ug.user_uuid, ug.uuid
      """, nativeQuery = true)
    List<UserGroup> findUserGroupsByCenterAndUserTypesAndYear(@Param("centerUuid") UUID centerUuid,
                                                              @Param("year") Integer year,
                                                              @Param("types") Integer[] types);

    List<UserGroup> findByYearAndDeletedAtIsNull(Integer year);

    List<UserGroup> findByYearAndUserTypeAndDeletedAtIsNull(int year, int userType);

    List<UserGroup> findByUser_UuidAndYearAndDeletedAtIsNull(UUID userUuid, Integer year);

    @Query("""
        select distinct ug.group.uuid
        from UserGroup ug
        where ug.user.uuid = :userUuid
          and ug.year = :year
          and ug.deletedAt is null
    """)
    List<UUID> findDistinctGroupUuidsByUserUuidAndYear(@Param("userUuid") UUID userUuid,
                                                       @Param("year") Integer year);

    @Query("""
        select distinct ug.group.center.uuid
        from UserGroup ug
        where ug.user.uuid = :userUuid
          and ug.year = :year
          and ug.deletedAt is null
    """)
    List<UUID> findDistinctCenterUuidsByUserUuidAndYear(@Param("userUuid") UUID userUuid,
                                                        @Param("year") Integer year);

    @Query("SELECT DISTINCT ug.group.uuid FROM UserGroup ug WHERE ug.group.center.uuid = :centerUuid AND ug.year = :year AND ug.deletedAt IS NULL")
    List<UUID> findDistinctGroupUuidsByCenterUuidAndYear(@Param("centerUuid") UUID centerUuid,
                                                         @Param("year") Integer year);

    @Query("SELECT DISTINCT ug.year FROM UserGroup ug WHERE ug.user.uuid = :userUuid AND ug.deletedAt IS NULL ORDER BY ug.year DESC")
    List<Integer> findDistinctYearsByUserUuid(@Param("userUuid") UUID userUuid);

    @Query("""
        SELECT COUNT(DISTINCT ug.user.uuid)
        FROM UserGroup ug
        JOIN ug.user u
        WHERE ug.group.center.uuid = :centerUuid
          AND ug.year = :year
          AND ug.deletedAt IS NULL
          AND u.deletedAt IS NULL
    """)
    Long countDistinctUsersByCenterUuidAndYear(@Param("centerUuid") UUID centerUuid,
                                               @Param("year") Integer year);

    @Query("""
        SELECT COUNT(DISTINCT ug.user.uuid)
        FROM UserGroup ug
        JOIN ug.user u
        WHERE ug.year = :year
          AND ug.deletedAt IS NULL
          AND u.deletedAt IS NULL
    """)
    Long countDistinctUsersByYear(@Param("year") Integer year);

    boolean existsByUser_UuidAndYearAndDeletedAtIsNull(UUID userUuid, Integer year);

    @Query("SELECT DISTINCT ug.user.uuid FROM UserGroup ug WHERE ug.uuid IN :userGroupUuids")
    List<UUID> findDistinctUserUuidsByUuidIn(@Param("userGroupUuids") Collection<UUID> userGroupUuids);

    Optional<UserGroup> findByUser_UuidAndGroup_UuidAndYearAndDeletedAtIsNull(UUID userUuid,
                                                                              UUID groupUuid,
                                                                              Integer year);

    Optional<UserGroup> findByUser_UuidAndGroup_UuidAndYear(UUID userUuid,
                                                            UUID groupUuid,
                                                            Integer year);

    Optional<UserGroup> findTopByUser_UuidAndGroup_UuidAndDeletedAtIsNullOrderByYearDesc(UUID userUuid,
                                                                                          UUID groupUuid);

    List<UserGroup> findByUser_UuidAndYearAndDeletedAtIsNullAndUserType(UUID userUuid,
                                                                        Integer year,
                                                                        Integer userType);

    List<UserGroup> findByGroup_UuidAndYearAndDeletedAtIsNullAndUser_UuidIn(UUID groupUuid,
                                                                             Integer year,
                                                                             Collection<UUID> userUuids);

    boolean existsByUser_UuidAndYearAndDeletedAtIsNullAndUserType(UUID userUuid, Integer year, Integer userType);

    boolean existsByUser_UuidAndGroup_UuidAndYearAndDeletedAtIsNullAndUserType(UUID userUuid,
                                                                                UUID groupUuid,
                                                                                Integer year,
                                                                                Integer userType);

    @Query(value = """
      SELECT 
        u.uuid       AS user_uuid,
        u.name       AS name,
        u.last_name  AS last_name,
        u.birth_date AS birth_date,
        u.dni        AS dni,
        MAX(ug.user_type) AS user_type,
        STRING_AGG(
          DISTINCT (
            c.name || ' (' || c.city || ') - ' ||
            CASE g.stage
              WHEN 0 THEN 'NAZARET 1'
              WHEN 1 THEN 'NAZARET 2'
              WHEN 2 THEN 'GENESARET 1'
              WHEN 3 THEN 'GENESARET 2'
              WHEN 4 THEN 'CAFARNAUM 1'
              WHEN 5 THEN 'CAFARNAUM 2'
              WHEN 6 THEN 'BETANIA 1'
              WHEN 7 THEN 'BETANIA 2'
              WHEN 8 THEN 'JERUSALEM'
            END
          ),
          ', '
          ORDER BY
          (c.name || ' (' || c.city || ') - ' ||
           CASE g.stage
             WHEN 0 THEN 'NAZARET 1'
             WHEN 1 THEN 'NAZARET 2'
             WHEN 2 THEN 'GENESARET 1'
             WHEN 3 THEN 'GENESARET 2'
             WHEN 4 THEN 'CAFARNAUM 1'
             WHEN 5 THEN 'CAFARNAUM 2'
             WHEN 6 THEN 'BETANIA 1'
             WHEN 7 THEN 'BETANIA 2'
             WHEN 8 THEN 'JERUSALEM'
           END)
        ) AS centers_groups
      FROM user_group ug
      JOIN user_salle  u ON u.uuid = ug.user_uuid
      JOIN group_salle g ON g.uuid = ug.group_uuid
      JOIN center      c ON c.uuid = g.center_uuid
      WHERE ug.year = :year
        AND ug.deleted_at IS NULL
        AND ug.user_type IN (0, 1)
      GROUP BY u.uuid, u.name, u.last_name, u.birth_date, u.dni
      ORDER BY MIN(c.name), MIN(c.city), MIN(g.stage), u.last_name, u.name
      """, nativeQuery = true)
    List<SeguroRow> findSeguroRows(@Param("year") int year);

    @Query("select ug from UserGroup ug where ug.user.uuid = :userUuid")
    List<UserGroup> findAllByUserIncludingDeleted(@Param("userUuid") UUID userUuid);

    @Query("select ug from UserGroup ug where ug.user.uuid = :userUuid and ug.deletedAt is not null")
    List<UserGroup> findDeletedByUser(@Param("userUuid") UUID userUuid);

    @Modifying
    @Query("""
        update UserGroup ug
           set ug.deletedAt = null
         where ug.user.uuid = :userUuid
           and ug.deletedAt is not null
    """)
    int reactivateByUser(@Param("userUuid") UUID userUuid);

    @Query("""
        select ug from UserGroup ug
         where ug.user.uuid = :userUuid
           and ug.group.uuid = :groupUuid
           and ug.year = :year
           and ug.deletedAt is null
    """)
    Optional<UserGroup> findActiveByUserGroupYear(@Param("userUuid") UUID userUuid,
                                                  @Param("groupUuid") UUID groupUuid,
                                                  @Param("year") Integer year);
}
