package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.UserGroup;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {

    @Query("""
        select ug
        from UserGroup ug
        where ug.group.id in :groupIds
    """)
    List<UserGroup> findByGroupIds(@Param("groupIds") Collection<Long> groupIds);


    /** Todos los user_groups cuyos grupos están en esos stages (cualquier centro). */
    @Query("""
        select ug
        from UserGroup ug
        join ug.group g
        where g.stage in :stages
    """)
    List<UserGroup> findByGroupStages(@Param("stages") Collection<Integer> stages);

    /** User_groups filtrando por centro + stages. */
    @Query("""
        select ug
        from UserGroup ug
        join ug.group g
        where g.center.id = :centerId
          and g.stage in :stages
    """)
    List<UserGroup> findByCenterAndGroupStages(@Param("centerId") Long centerId,
                                               @Param("stages") Collection<Integer> stages);
}