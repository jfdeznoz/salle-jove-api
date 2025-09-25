package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserGroup;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromotionService {

    private final UserGroupService userGroupService;
    private final GroupService groupService;
    private final AcademicStateService academicStateService;

    // Primaria -> Secundaria (Andalucía)
    private static final Map<Long, Long> PRIMARY_TO_SECONDARY = Map.of(
            1L, 2L,   // Mirandilla (Cádiz) -> Viña (Cádiz)
            15L,16L,  // S. Francisco Javier (Antequera) -> Virlecha (Antequera)
            13L,12L,  // La Purísima (Sevilla) -> Felipe Benito (Sevilla)
            9L, 7L    // San José (Jerez)     -> Buen Pastor (Jerez)
    );

    @Transactional
    public void promote() throws SalleException {
        final LocalDate today = LocalDate.now();
        final int newYear = academicYear(today);
        final int sourceYear = newYear - 1;

        List<UserGroup> sourceRows = userGroupService.findActiveByYear(sourceYear);
        if (sourceRows.isEmpty()) {
            log.info("Promoción: no hay filas vigentes en year={} (nada que promocionar).", sourceYear);
            academicStateService.setVisibleYear(newYear);
            return;
        }

        List<UserGroup> toInsert = new ArrayList<>(sourceRows.size());
        int created = 0, skipped = 0;

        for (UserGroup ug : sourceRows) {
            GroupSalle g = ug.getGroup();
            Long centerId = g.getCenter().getId();
            int stage = g.getStage();

            GroupSalle target;
            if (stage == 8) {
                // Jerusalén -> mismo grupo
                target = g;
            } else if (stage == 1 && PRIMARY_TO_SECONDARY.containsKey(centerId)) {
                // Nazaret 2 (1) → Genesaret 1 (2) en el centro de Secundaria
                Long destCenter = PRIMARY_TO_SECONDARY.get(centerId);
                target = groupService.getByCenterAndStageOrThrow(destCenter, 2);
            } else {
                // Progresión normal: stage → stage+1 en el mismo centro
                target = groupService.getByCenterAndStageOrThrow(centerId, stage + 1);
            }

            Long userId = ug.getUser().getId();
            if (userGroupService.existsActiveForUserInYear(userId, newYear)) {
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

    private static int academicYear(LocalDate d) {
        return (d.getMonthValue() >= 9) ? d.getYear() : d.getYear() - 1;
    }
}