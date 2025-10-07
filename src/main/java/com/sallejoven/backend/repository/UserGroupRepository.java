package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.UserGroup;
import java.util.Collection;
import java.util.List;
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

    boolean existsByUser_IdAndYearAndDeletedAtIsNull(Long userId, Integer year);

    Optional<UserGroup> findByUser_IdAndGroup_IdAndYearAndDeletedAtIsNull(
            Long userId,
            Long groupId,
            Integer year
    );
}