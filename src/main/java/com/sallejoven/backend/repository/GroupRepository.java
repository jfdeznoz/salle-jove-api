package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.GroupSalle;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupRepository extends JpaRepository<GroupSalle, UUID> {

    default Optional<GroupSalle> findByUuid(UUID uuid) {
        return findById(uuid);
    }

    Optional<GroupSalle> findByCenterAndStage(Center center, Integer stage);

    List<GroupSalle> findByStageIn(List<Integer> stages);

    List<GroupSalle> findByCenter(Center center);

    @Query("SELECT g FROM GroupSalle g WHERE g.stage IN :stages AND g.center.uuid = :centerUuid")
    List<GroupSalle> findAllByStagesAndCenterUuid(@Param("stages") List<Integer> stages,
                                                  @Param("centerUuid") UUID centerUuid);

    List<GroupSalle> findByCenterUuid(UUID centerUuid);

    Optional<GroupSalle> findByCenterUuidAndStage(UUID centerUuid, Integer stage);

    List<GroupSalle> findByCenterUuidIn(Collection<UUID> centerUuids);
}
