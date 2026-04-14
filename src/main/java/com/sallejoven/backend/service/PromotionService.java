package com.sallejoven.backend.service;

import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.repository.CenterRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromotionService {

    private final UserGroupService userGroupService;
    private final GroupService groupService;
    private final AcademicStateService academicStateService;
    private final CenterRepository centerRepository;

    /**
     * Primaria -> Secundaria (Andalucía). Resuelto por (nombre, ciudad) para no depender
     * de IDs numéricos concretos — los IDs son UUIDs generados aleatoriamente.
     */
    private static final List<CenterPromotionPair> PRIMARY_TO_SECONDARY_PAIRS = List.of(
            new CenterPromotionPair("Mirandilla", "Cádiz", "Viña", "Cádiz"),
            new CenterPromotionPair("S. Francisco Javier", "Antequera", "Virlecha", "Antequera"),
            new CenterPromotionPair("La Purísima", "Sevilla", "Felipe Benito", "Sevilla"),
            new CenterPromotionPair("San José", "Jerez", "Buen Pastor", "Jerez")
    );

    @Transactional
    public void promote() {
        final LocalDate today = LocalDate.now();
        final int newYear = academicYear(today);
        final int sourceYear = newYear - 1;

        List<UserGroup> sourceRows = userGroupService.findActiveCatechumensByYear(sourceYear);
        if (sourceRows.isEmpty()) {
            log.info("Promoción: no hay filas vigentes en year={} (nada que promocionar).", sourceYear);
            academicStateService.setVisibleYear(newYear);
            return;
        }

        Map<UUID, UUID> primaryToSecondary = resolvePrimaryToSecondaryMap();

        List<UserGroup> toInsert = new ArrayList<>(sourceRows.size());
        int created = 0, skipped = 0;

        for (UserGroup ug : sourceRows) {
            GroupSalle g = ug.getGroup();
            UUID centerUuid = g.getCenter().getUuid();
            int stage = g.getStage();

            GroupSalle target;
            if (stage == 8) {
                target = g;
            } else if (stage == 1 && primaryToSecondary.containsKey(centerUuid)) {
                UUID destCenter = primaryToSecondary.get(centerUuid);
                target = groupService.getByCenterAndStageOrThrow(destCenter, 2);
            } else {
                target = groupService.getByCenterAndStageOrThrow(centerUuid, stage + 1);
            }

            UUID userUuid = ug.getUser().getUuid();
            if (userGroupService.existsActiveForUserInYear(userUuid, newYear)) {
                skipped++;
                continue;
            }

            UserGroup nextRow = UserGroup.builder()
                    .user(ug.getUser())
                    .group(target)
                    .userType(ug.getUserType())
                    .year(newYear)
                    .deletedAt(null)
                    .build();

            toInsert.add(nextRow);
            created++;
        }

        if (!toInsert.isEmpty()) {
            userGroupService.saveAll(toInsert);
        }

        academicStateService.setVisibleYear(newYear);
        log.info("Promoción completada: sourceYear={}, newYear={}, creados={}, omitidos={}",
                sourceYear, newYear, created, skipped);
    }

    private Map<UUID, UUID> resolvePrimaryToSecondaryMap() {
        Map<UUID, UUID> out = new HashMap<>();
        for (CenterPromotionPair pair : PRIMARY_TO_SECONDARY_PAIRS) {
            Center primary = centerRepository.findByNameAndCity(pair.primaryName(), pair.primaryCity()).orElse(null);
            Center secondary = centerRepository.findByNameAndCity(pair.secondaryName(), pair.secondaryCity()).orElse(null);
            if (primary != null && secondary != null) {
                out.put(primary.getUuid(), secondary.getUuid());
            }
        }
        return out;
    }

    private static int academicYear(LocalDate d) {
        return (d.getMonthValue() >= 9) ? d.getYear() : d.getYear() - 1;
    }

    private record CenterPromotionPair(String primaryName, String primaryCity, String secondaryName, String secondaryCity) {}
}
