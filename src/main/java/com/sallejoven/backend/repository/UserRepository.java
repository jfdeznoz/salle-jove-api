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

    @Query(value = """
    SELECT u.*
    FROM user_salle u
    WHERE u.deleted_at IS NULL
      AND (
        -- nombre completo: name + ' ' + last_name
        lower(
          translate(
            trim(coalesce(u.name,'') || ' ' || coalesce(u.last_name,'')),
            'ÁÉÍÓÚÜÀÈÌÒÙÑáéíóúüàèìòùñ',
            'AEIOUUAEIOUNaeiouuaeioun'
          )
        ) LIKE CONCAT('%', :normalized, '%')
        OR
        lower(
          translate(
            coalesce(u.email,''),
            'ÁÉÍÓÚÜÀÈÌÒÙÑáéíóúüàèìòùñ',
            'AEIOUUAEIOUNaeiouuaeioun'
          )
        ) LIKE CONCAT('%', :normalized, '%')
      )
    ORDER BY u.name NULLS LAST, u.last_name NULLS LAST
    """, nativeQuery = true)
    List<UserSalle> searchUsersNormalized(@Param("normalized") String normalized);

    @Query("""
        select distinct u
        from UserSalle u
        join UserGroup ug on ug.user.id = u.id
        join ug.group g
        where u.deletedAt is null
          and ug.deletedAt is null
          and ug.year = :year
          and ug.userType = 1
          and g.center.id = :centerId
    """)
    List<UserSalle> findAnimatorsByCenterAndYear(@Param("centerId") Long centerId,
                                                 @Param("year") int year);
}