package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.VitalSituationSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VitalSituationSessionRepository extends JpaRepository<VitalSituationSession, Long> {

    @Query("SELECT vss FROM VitalSituationSession vss WHERE vss.deletedAt IS NULL AND vss.vitalSituation.id = :vitalSituationId ORDER BY vss.title ASC")
    List<VitalSituationSession> findByVitalSituationId(@Param("vitalSituationId") Long vitalSituationId);

    @Query("SELECT vss FROM VitalSituationSession vss WHERE vss.id = :id AND vss.deletedAt IS NULL")
    Optional<VitalSituationSession> findById(@Param("id") Long id);

    @Modifying
    @Query("UPDATE VitalSituationSession vss SET vss.deletedAt = CURRENT_TIMESTAMP WHERE vss.id = :id")
    void softDelete(@Param("id") Long id);
}

