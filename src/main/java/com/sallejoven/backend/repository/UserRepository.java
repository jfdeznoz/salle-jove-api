package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.UserSalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserSalle, Long> {

    Optional<UserSalle> findByEmail(String email);

    boolean existsByDni(String dni);

    boolean existsByEmail(String email);

    @Query("SELECT DISTINCT u FROM UserSalle u " +
       "JOIN u.groups g " +
       "WHERE g.stage IN :stages AND u.deletedAt IS NULL")
    List<UserSalle> findUsersByStages(@Param("stages") List<Integer> stages);

    @Query("SELECT u FROM UserSalle u JOIN u.groups g WHERE g.id = :groupId AND u.deletedAt IS NULL")
    List<UserSalle> findUsersByGroupId(@Param("groupId") Long groupId);

}