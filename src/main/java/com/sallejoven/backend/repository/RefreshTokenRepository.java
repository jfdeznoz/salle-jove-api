package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.RefreshToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(String refreshToken);

    @Modifying
    @Query("delete from RefreshToken rt where rt.user.uuid = :userUuid")
    int deleteByUserUuid(@Param("userUuid") UUID userUuid);
}
