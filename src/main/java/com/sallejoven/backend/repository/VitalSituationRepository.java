package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.VitalSituation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VitalSituationRepository extends JpaRepository<VitalSituation, UUID> {

    default Optional<VitalSituation> findByUuid(UUID uuid) {
        return findById(uuid);
    }

    @Query(value = "SELECT * FROM vital_situation vs WHERE vs.deleted_at IS NULL AND :stage = ANY(vs.stages) ORDER BY vs.title ASC", nativeQuery = true)
    List<VitalSituation> findByStage(@Param("stage") Integer stage);

    @Query("SELECT vs FROM VitalSituation vs WHERE vs.deletedAt IS NULL ORDER BY vs.title ASC")
    List<VitalSituation> findAllActive();

    @Query("SELECT vs FROM VitalSituation vs WHERE vs.uuid = :uuid AND vs.deletedAt IS NULL")
    Optional<VitalSituation> findById(@Param("uuid") UUID uuid);

    @Modifying
    @Query("UPDATE VitalSituation vs SET vs.deletedAt = CURRENT_TIMESTAMP WHERE vs.uuid = :uuid")
    void softDelete(@Param("uuid") UUID uuid);
}
