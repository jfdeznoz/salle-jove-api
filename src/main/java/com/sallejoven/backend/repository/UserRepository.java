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

    @Query("""
      SELECT DISTINCT u
      FROM UserSalle u
      JOIN u.groups ug
      JOIN ug.group g
      WHERE g.stage IN :stages
        AND u.deletedAt IS NULL
    """)
    List<UserSalle> findUsersByStages(@Param("stages") List<Integer> stages);

    @Query("""
        SELECT DISTINCT u
        FROM UserSalle u
        JOIN u.groups ug
        JOIN ug.group gr
        WHERE gr.id = :groupId
          AND u.deletedAt IS NULL
        """)
    List<UserSalle> findUsersByGroupId(@Param("groupId") Long groupId);

    @Query("""
      SELECT DISTINCT u
      FROM UserSalle u
      JOIN u.groups ug
      JOIN ug.group g
      WHERE g.center.id = :centerId
        AND u.deletedAt IS NULL
    """)
    List<UserSalle> findUsersByCenterId(@Param("centerId") Long centerId);

    @Query("""
      SELECT DISTINCT u
      FROM UserSalle u
      JOIN u.groups ug
      WHERE ug.group.center.id = :centerId
        AND u.roles LIKE :role
        AND u.deletedAt IS NULL
    """)
    List<UserSalle> findUsersByCenterIdAndRole(@Param("centerId") Long centerId, @Param("role") String role);

    @Query("SELECT u FROM UserSalle u WHERE u.roles LIKE %:role1% OR u.roles LIKE %:role2% AND u.deletedAt IS NULL")
    List<UserSalle> findAllByRoles(@Param("role1") String role1, @Param("role2") String role2);
}