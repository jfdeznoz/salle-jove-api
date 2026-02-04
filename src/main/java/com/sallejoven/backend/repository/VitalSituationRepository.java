package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.VitalSituation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VitalSituationRepository extends JpaRepository<VitalSituation, Long> {

    @Query(value = "SELECT * FROM vital_situation vs WHERE vs.deleted_at IS NULL AND :stage = ANY(vs.stages) ORDER BY vs.title ASC", nativeQuery = true)
    List<VitalSituation> findByStage(@Param("stage") Integer stage);

    @Query("SELECT vs FROM VitalSituation vs WHERE vs.deletedAt IS NULL ORDER BY vs.title ASC")
    List<VitalSituation> findAllActive();

    @Query("SELECT vs FROM VitalSituation vs WHERE vs.id = :id AND vs.deletedAt IS NULL")
    Optional<VitalSituation> findById(@Param("id") Long id);

    @Modifying
    @Query("UPDATE VitalSituation vs SET vs.deletedAt = CURRENT_TIMESTAMP WHERE vs.id = :id")
    void softDelete(@Param("id") Long id);
}

