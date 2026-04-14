package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.VitalSituationSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VitalSituationSessionRepository extends JpaRepository<VitalSituationSession, UUID> {

    default Optional<VitalSituationSession> findByUuid(UUID uuid) {
        return findById(uuid);
    }

    @Query("SELECT vss FROM VitalSituationSession vss WHERE vss.deletedAt IS NULL AND vss.vitalSituation.uuid = :vitalSituationUuid ORDER BY vss.title ASC")
    List<VitalSituationSession> findByVitalSituationUuid(@Param("vitalSituationUuid") UUID vitalSituationUuid);

    @Query("SELECT vss FROM VitalSituationSession vss WHERE vss.uuid = :uuid AND vss.deletedAt IS NULL")
    Optional<VitalSituationSession> findById(@Param("uuid") UUID uuid);

    @Modifying
    @Query("UPDATE VitalSituationSession vss SET vss.deletedAt = CURRENT_TIMESTAMP WHERE vss.uuid = :uuid")
    void softDelete(@Param("uuid") UUID uuid);
}
