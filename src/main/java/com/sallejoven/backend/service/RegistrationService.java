package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserPending;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.enums.ErrorCodes;
import com.sallejoven.backend.model.requestDto.UserSalleRequest;
import com.sallejoven.backend.repository.UserPendingRepository;
import com.sallejoven.backend.repository.UserRepository;
import com.sallejoven.backend.utils.ReferenceParser;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserRepository userRepository;
    private final UserPendingRepository userPendingRepository;
    private final PasswordEncoder passwordEncoder;
    private final GroupService groupService;
    private final CenterService centerService;
    private final EventUserService eventUserService;
    private final AcademicStateService academicStateService;
    private final UserCenterService userCenterService;
    private final AuthorityService authorityService;
    private final UserRoleHelper roleHelper;

    @Transactional
    public UserPending registerPublic(UserSalleRequest req) {
        String role = roleHelper.normalizeRole(req.getRol());
        String dni = trimOrNull(req.getDni());
        Center center = resolveCenter(req);
        GroupSalle group = resolveGroup(req);

        if (roleHelper.usesCenterOnly(role) && center == null) {
            throw new SalleException(ErrorCodes.CENTER_NOT_FOUND, "Se requiere centerUuid para " + role);
        }
        if (!roleHelper.usesCenterOnly(role) && group == null) {
            throw new SalleException(ErrorCodes.GROUP_NOT_FOUND, "Se requiere groupUuid para " + role);
        }
        if (userRepository.existsByEmail(req.getEmail()) || userPendingRepository.existsByEmail(req.getEmail())) {
            throw new SalleException(ErrorCodes.EMAIL_ALREADY_EXISTS);
        }
        if (dni != null
                && (userRepository.existsByDni(dni) || userPendingRepository.existsByDni(dni))) {
            throw new SalleException(ErrorCodes.DNI_ALREADY_EXISTS);
        }

        UserPending pending = UserPending.builder()
                .name(trimReq(req.getName()))
                .lastName(trimReq(req.getLastName()))
                .dni(dni)
                .phone(trimOrNull(req.getPhone()))
                .email(trimReq(req.getEmail()))
                .tshirtSize(req.getTshirtSize())
                .healthCardNumber(trimOrNull(req.getHealthCardNumber()))
                .intolerances(trimOrNull(req.getIntolerances()))
                .chronicDiseases(trimOrNull(req.getChronicDiseases()))
                .city(trimOrNull(req.getCity()))
                .address(trimOrNull(req.getAddress()))
                .imageAuthorization(req.getImageAuthorization())
                .birthDate(req.getBirthDate())
                .roles(role)
                .password(passwordEncoder.encode(req.getPassword()))
                .gender(req.getGender())
                .motherFullName(trimOrNull(req.getMotherFullName()))
                .fatherFullName(trimOrNull(req.getFatherFullName()))
                .motherEmail(trimOrNull(req.getMotherEmail()))
                .fatherEmail(trimOrNull(req.getFatherEmail()))
                .motherPhone(trimOrNull(req.getMotherPhone()))
                .fatherPhone(trimOrNull(req.getFatherPhone()))
                .centerUuid(center != null ? center.getUuid() : null)
                .groupUuid(group != null ? group.getUuid() : null)
                .createdAt(LocalDateTime.now())
                .build();

        return userPendingRepository.save(pending);
    }

    @Transactional
    public UserSalle approvePending(UUID pendingUuid) {
        if (academicStateService.isLocked()) {
            throw new SalleException(ErrorCodes.SYSTEM_LOCKED);
        }
        UserPending pending = userPendingRepository.findById(pendingUuid)
                .orElseThrow(() -> new SalleException(ErrorCodes.USER_NOT_FOUND, "Solicitud no encontrada"));

        if (userRepository.existsByEmail(pending.getEmail())) {
            throw new SalleException(ErrorCodes.EMAIL_ALREADY_EXISTS);
        }
        if (pending.getDni() != null && !pending.getDni().isBlank() && userRepository.existsByDni(pending.getDni())) {
            throw new SalleException(ErrorCodes.DNI_ALREADY_EXISTS);
        }

        String role = roleHelper.normalizeRole(pending.getRoles());
        int userType = roleHelper.mapUserTypeFromRequest(role);
        boolean isAdmin = role.equals("ROLE_ADMIN");

        UserSalle user = UserSalle.builder()
                .name(pending.getName())
                .lastName(pending.getLastName())
                .dni(pending.getDni())
                .phone(pending.getPhone())
                .email(pending.getEmail())
                .tshirtSize(pending.getTshirtSize())
                .healthCardNumber(pending.getHealthCardNumber())
                .intolerances(pending.getIntolerances())
                .chronicDiseases(pending.getChronicDiseases())
                .city(pending.getCity())
                .address(pending.getAddress())
                .imageAuthorization(pending.getImageAuthorization())
                .birthDate(pending.getBirthDate())
                .isAdmin(isAdmin)
                .password(pending.getPassword())
                .gender(pending.getGender())
                .motherFullName(pending.getMotherFullName())
                .fatherFullName(pending.getFatherFullName())
                .motherEmail(pending.getMotherEmail())
                .fatherEmail(pending.getFatherEmail())
                .motherPhone(pending.getMotherPhone())
                .fatherPhone(pending.getFatherPhone())
                .build();

        if (user.getGroups() == null) {
            user.setGroups(new java.util.HashSet<>());
        }
        user = userRepository.save(user);

        if (!isAdmin) {
            if (roleHelper.usesCenterOnly(role)) {
                userCenterService.addCenterRole(user.getUuid(), resolvePendingCenter(pending).getUuid(), userType);
            } else {
                GroupSalle group = resolvePendingGroup(pending);
                roleHelper.ensureMembership(user, group, userType);
                userRepository.save(user);
                eventUserService.assignFutureGroupEventsToUser(user, group);
            }
        }

        userPendingRepository.deleteById(pendingUuid);
        return user;
    }

    @Transactional
    public void rejectPending(UUID pendingUuid) {
        if (!userPendingRepository.existsById(pendingUuid)) {
            throw new SalleException(ErrorCodes.USER_NOT_FOUND, "Solicitud pendiente no encontrada");
        }
        userPendingRepository.deleteById(pendingUuid);
    }

    public List<UserPending> listPending() {
        var auths = authorityService.getCurrentAuth();
        if (auths.contains("ROLE_ADMIN")) {
            return userPendingRepository.findAll();
        }

        Integer year = academicStateService.getVisibleYearOrNull();
        if (year == null) {
            return List.of();
        }

        Set<UUID> myCenterUuids = authorityService.extractCenterIdsForYear(auths, year);
        if (myCenterUuids.isEmpty()) {
            return List.of();
        }

        Set<UserPending> result = new LinkedHashSet<>();
        result.addAll(userPendingRepository.findByCenterUuidIn(myCenterUuids));
        result.addAll(userPendingRepository.findByGroupCenterUuids(myCenterUuids));
        return result.stream().toList();
    }

    public UserPending findPendingByReference(String reference) {
        UUID uuid = ReferenceParser.asUuid(reference)
                .orElseThrow(() -> new SalleException(ErrorCodes.USER_NOT_FOUND, "Solicitud no encontrada"));
        return userPendingRepository.findByUuid(uuid)
                .orElseThrow(() -> new SalleException(ErrorCodes.USER_NOT_FOUND, "Solicitud no encontrada"));
    }

    private Center resolveCenter(UserSalleRequest req) {
        if (req.getCenterUuid() == null || req.getCenterUuid().isBlank()) {
            return null;
        }
        return centerService.findByReference(req.getCenterUuid());
    }

    private GroupSalle resolveGroup(UserSalleRequest req) {
        if (req.getGroupUuid() == null || req.getGroupUuid().isBlank()) {
            return null;
        }
        return groupService.findByReference(req.getGroupUuid());
    }

    private Center resolvePendingCenter(UserPending pending) {
        if (pending.getCenterUuid() == null) {
            throw new SalleException(ErrorCodes.CENTER_NOT_FOUND);
        }
        return centerService.findById(pending.getCenterUuid());
    }

    private GroupSalle resolvePendingGroup(UserPending pending) {
        if (pending.getGroupUuid() == null) {
            throw new SalleException(ErrorCodes.GROUP_NOT_FOUND);
        }
        return groupService.findById(pending.getGroupUuid());
    }

    private static String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String trimReq(String value) {
        return value == null ? null : value.trim();
    }
}
