package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.UserSalle;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserSalle, UUID> {

    Optional<UserSalle> findByEmail(String email);

    default Optional<UserSalle> findByUuid(UUID uuid) {
        return findById(uuid);
    }

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
        WHERE gr.uuid = :groupUuid
          AND u.deletedAt IS NULL
        """)
    List<UserSalle> findUsersByGroupUuid(@Param("groupUuid") UUID groupUuid);

    @Query("""
      SELECT DISTINCT u
      FROM UserSalle u
      JOIN u.groups ug
      JOIN ug.group g
      WHERE g.center.uuid = :centerUuid
        AND u.deletedAt IS NULL
    """)
    List<UserSalle> findUsersByCenterUuid(@Param("centerUuid") UUID centerUuid);

    @Query(value = """
    SELECT u.*
    FROM user_salle u
    WHERE u.deleted_at IS NULL
      AND (
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

    @Query(value = """
    SELECT DISTINCT u.*
    FROM user_salle u
    LEFT JOIN user_group ug ON ug.user_uuid = u.uuid
    LEFT JOIN group_salle g ON g.uuid = ug.group_uuid
    WHERE u.deleted_at IS NULL
      AND g.center_uuid IN (:centerUuids)
      AND (
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
    List<UserSalle> searchUsersNormalizedByCenterUuids(@Param("normalized") String normalized,
                                                       @Param("centerUuids") Set<UUID> centerUuids);

    @Query("""
        select distinct u
        from UserSalle u
        join UserGroup ug on ug.user.uuid = u.uuid
        join ug.group g
        where u.deletedAt is null
          and ug.deletedAt is null
          and ug.year = :year
          and ug.userType = 1
          and g.center.uuid = :centerUuid
    """)
    List<UserSalle> findAnimatorsByCenterAndYear(@Param("centerUuid") UUID centerUuid,
                                                 @Param("year") int year);

    @Query("""
        select u
        from UserSalle u
        where u.deletedAt is not null
        order by u.deletedAt desc
    """)
    List<UserSalle> findAllByDeletedAtIsNotNullOrderByDeletedAtDesc(org.springframework.data.domain.Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UserSalle u WHERE u.uuid = :uuid")
    Optional<UserSalle> findByIdForUpdate(@Param("uuid") UUID uuid);

    @Query(value = """
    SELECT u.*
    FROM user_salle u
    WHERE u.deleted_at IS NOT NULL
      AND (
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
        OR
        lower(coalesce(u.dni,'')) LIKE CONCAT('%', :normalized, '%')
      )
    ORDER BY u.deleted_at DESC
    LIMIT 100
    """, nativeQuery = true)
    List<UserSalle> findDeletedByNormalized(@Param("normalized") String normalized);

    @Query("select u from UserSalle u where u.email = :email and u.deletedAt is null")
    Optional<UserSalle> findActiveByEmail(@Param("email") String email);
}
