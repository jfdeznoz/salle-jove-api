// src/main/java/.../repository/AcademicStateRepository.java
package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.AcademicState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademicStateRepository extends JpaRepository<AcademicState, Short> {}
