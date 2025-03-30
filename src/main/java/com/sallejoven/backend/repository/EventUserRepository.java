package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.EventUser;
import com.sallejoven.backend.model.ids.EventUserId;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventUserRepository extends JpaRepository<EventUser, EventUserId> {

    @Query("SELECT eu FROM EventUser eu " +
           "JOIN eu.user u " +
           "JOIN u.groups g " +
           "WHERE eu.event.id = :eventId AND g.id = :groupId")
    List<EventUser> findByEventIdAndGroupId(@Param("eventId") Integer eventId, @Param("groupId") Integer groupId);
}
