package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.UserGroup;
import java.util.Collection;
import java.util.List;

import com.sallejoven.backend.repository.projection.SeguroRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {

    @Query("""
        select ug
        from UserGroup ug
        where ug.id = :id
          and ug.deletedAt is null
    """)
    Optional<UserGroup> findByIdAndDeletedAtIsNull(@Param("id") Long id);

    @Query("""
        select ug
        from UserGroup ug
        where ug.group.id in :groupIds
          and ug.year = :year
          and ug.deletedAt is null
    """)
    List<UserGroup> findByGroupIdsAndYear(
            @Param("groupIds") Collection<Long> groupIds,
            @Param("year") Integer year
    );

    @Query("""
        select ug
        from UserGroup ug
        where ug.group.id = :groupId
          and ug.year = :year
          and ug.deletedAt is null
    """)
    List<UserGroup> findByGroupIdAndYear(
            @Param("groupId") Long groupId,
            @Param("year") Integer year);

    @Query("""
        select ug
        from UserGroup ug
        join ug.group g
        where g.stage in :stages
          and ug.year = :year
          and ug.deletedAt is null
    """)
    List<UserGroup> findByGroupStagesAndYear(
            @Param("stages") Collection<Integer> stages,
            @Param("year") Integer year);


    @Query("""
        select ug
        from UserGroup ug
        join ug.group g
        where g.center.id = :centerId
          and g.stage in :stages
          and ug.year = :year
          and ug.deletedAt is null
    """)
    List<UserGroup> findByCenterAndGroupStagesAndYear(
            @Param("centerId") Long centerId,
            @Param("stages") Collection<Integer> stages,
            @Param("year") Integer year);


    @Query(value = """
        select distinct on (ug.user_salle) ug.*
        from user_group ug
        join group_salle g on g.id = ug.group_salle
        where g.center = :centerId
          and ug.user_type = any(:types)
          and ug.year = :year
          and ug.deleted_at is null
          and g.deleted_at is null
        order by ug.user_salle, ug.id
      """, nativeQuery = true)
    List<UserGroup> findUserGroupsByCenterAndUserTypesAndYear(
            @Param("centerId") Long centerId,
            @Param("year") Integer year,
            @Param("types") Integer[] types
    );

    List<UserGroup> findByYearAndDeletedAtIsNull(Integer year);

    List<UserGroup> findByYearAndUserTypeAndDeletedAtIsNull(int year, int userType);

    List<UserGroup> findByUser_IdAndYearAndDeletedAtIsNull(Long userId, Integer year);

    boolean existsByUser_IdAndYearAndDeletedAtIsNull(Long userId, Integer year);


    Optional<UserGroup> findByUser_IdAndGroup_IdAndYearAndDeletedAtIsNull(
            Long userId,
            Long groupId,
            Integer year
    );

    Optional<UserGroup> findByUser_IdAndGroup_IdAndYear(
            Long userId,
            Long groupId,
            Integer year
    );

    List<UserGroup> findByUser_IdAndYearAndDeletedAtIsNullAndUserType(
            Long userId,
            Integer year,
            Integer userType
    );

    boolean existsByUser_IdAndYearAndDeletedAtIsNullAndUserType(Long userId, Integer year, Integer userType);

    boolean existsByUser_IdAndGroup_IdAndYearAndDeletedAtIsNullAndUserType(
            Long userId,
            Long groupId,
            Integer year,
            Integer userType
    );

    @Query(value = """
  SELECT 
    u.id         AS user_id,
    u.name       AS name,
    u.last_name  AS last_name,
    u.birth_date AS birth_date,
    u.dni        AS dni,
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
  JOIN user_salle  u ON u.id = ug.user_salle
  JOIN group_salle g ON g.id = ug.group_salle
  JOIN center      c ON c.id = g.center
  WHERE ug.year = :year
    AND ug.deleted_at IS NULL
    AND ug.user_type IN (0, 1)
  GROUP BY u.id, u.name, u.last_name, u.birth_date, u.dni
  ORDER BY MIN(c.name), MIN(g.stage), u.last_name, u.name
  """, nativeQuery = true)
    List<SeguroRow> findSeguroRows(@Param("year") int year);

}