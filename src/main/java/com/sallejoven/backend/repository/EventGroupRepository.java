package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.EventGroup;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface EventGroupRepository extends JpaRepository<EventGroup, Long> {

    // JPQL con nombres de propiedad (no de columna)
    @Query("""
        SELECT eg
        FROM EventGroup eg
        WHERE eg.event.id = :eventId
          AND eg.deletedAt IS NULL
    """)
    List<EventGroup> findByEventId(@Param("eventId") Long eventId);

    List<EventGroup> findByEvent_IdAndGroupSalle_IdIn(Long eventId, List<Long> groupIds);

    void deleteByEvent_IdAndGroupSalle_IdIn(Long eventId, List<Long> groupSalleIds);

    List<EventGroup> findByGroupSalle_Id(Long groupSalleId);

    @Modifying
    @Transactional
    @Query("UPDATE EventGroup eg SET eg.deletedAt = CURRENT_TIMESTAMP WHERE eg.event.id = :eventId")
    void softDeleteByEventId(@Param("eventId") Long eventId);

    @Query("""
        SELECT eg
        FROM EventGroup eg
        JOIN eg.groupSalle g
        WHERE eg.event.id = :eventId
          AND g.center.id = :centerId
          AND eg.deletedAt IS NULL
    """)
    List<EventGroup> findByEventIdAndCenterId(@Param("eventId") Long eventId,
                                              @Param("centerId") Long centerId);

    @Query("""
        SELECT eg
        FROM EventGroup eg
        JOIN eg.groupSalle g
        WHERE eg.event.id = :eventId
          AND g.center.id IN :centerIds
          AND eg.deletedAt IS NULL
    """)
    List<EventGroup> findByEventIdAndCenterIds(@Param("eventId") Long eventId,
                                               @Param("centerIds") List<Long> centerIds);
}