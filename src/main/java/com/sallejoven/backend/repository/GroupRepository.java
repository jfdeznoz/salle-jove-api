package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.GroupSalle;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupRepository extends JpaRepository<GroupSalle, Long> {

    List<GroupSalle> findByStageIn(List<Integer> stages);
}