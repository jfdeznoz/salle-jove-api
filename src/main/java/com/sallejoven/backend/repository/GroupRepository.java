package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.GroupSalle;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupRepository extends JpaRepository<GroupSalle, Long> {

    Optional<GroupSalle> findByCenterAndStage(Center center, Integer stage);
    List<GroupSalle> findByStageIn(List<Integer> stages);
    List<GroupSalle> findByCenter(Center center);

    @Query("SELECT g FROM GroupSalle g WHERE g.stage IN :stages AND g.center.id = :centerId")
    List<GroupSalle> findAllByStagesAndCenterId(@Param("stages") List<Integer> stages, @Param("centerId") Long centerId);

    List<GroupSalle> findByCenterId(Long centerId);

    Optional<GroupSalle> findByCenterIdAndStage(Long centerId, Integer stage);

    List<GroupSalle> findByCenterIdIn(Collection<Long> centerIds);

}