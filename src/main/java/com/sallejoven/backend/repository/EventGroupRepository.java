package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.EventGroup;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface EventGroupRepository extends JpaRepository<EventGroup, UUID> {

    @Query("""
        SELECT eg
        FROM EventGroup eg
        WHERE eg.event.uuid = :eventUuid
          AND eg.deletedAt IS NULL
    """)
    List<EventGroup> findByEventUuid(@Param("eventUuid") UUID eventUuid);

    List<EventGroup> findByEvent_UuidAndGroupSalle_UuidIn(UUID eventUuid, List<UUID> groupUuids);

    void deleteByEvent_UuidAndGroupSalle_UuidIn(UUID eventUuid, List<UUID> groupUuids);

    List<EventGroup> findByGroupSalle_Uuid(UUID groupUuid);

    @Modifying
    @Transactional
    @Query("UPDATE EventGroup eg SET eg.deletedAt = CURRENT_TIMESTAMP WHERE eg.event.uuid = :eventUuid")
    void softDeleteByEventUuid(@Param("eventUuid") UUID eventUuid);

    Optional<EventGroup> findFirstByEvent_UuidAndDeletedAtIsNullOrderByUuidAsc(UUID eventUuid);

    @Query("""
        SELECT eg
        FROM EventGroup eg
        JOIN eg.groupSalle g
        WHERE eg.event.uuid = :eventUuid
          AND g.center.uuid = :centerUuid
          AND eg.deletedAt IS NULL
    """)
    List<EventGroup> findByEventUuidAndCenterUuid(@Param("eventUuid") UUID eventUuid,
                                                  @Param("centerUuid") UUID centerUuid);

    @Query("""
        SELECT eg
        FROM EventGroup eg
        JOIN eg.groupSalle g
        WHERE eg.event.uuid = :eventUuid
          AND g.center.uuid IN :centerUuids
          AND eg.deletedAt IS NULL
    """)
    List<EventGroup> findByEventUuidAndCenterUuids(@Param("eventUuid") UUID eventUuid,
                                                   @Param("centerUuids") List<UUID> centerUuids);

    @Query("""
        SELECT eg
        FROM EventGroup eg
        JOIN FETCH eg.groupSalle g
        JOIN FETCH g.center
        WHERE eg.event.uuid IN :eventUuids
          AND eg.deletedAt IS NULL
    """)
    List<EventGroup> findActiveByEventUuids(@Param("eventUuids") Collection<UUID> eventUuids);
}
