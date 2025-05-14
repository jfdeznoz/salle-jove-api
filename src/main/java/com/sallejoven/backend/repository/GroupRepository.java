package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.GroupSalle;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupRepository extends JpaRepository<GroupSalle, Long> {

    Optional<GroupSalle> findByCenterAndStage(Center center, Integer stage);
    List<GroupSalle> findByStageIn(List<Integer> stages);
    List<GroupSalle> findByCenter(Center center);
}