package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.dto.UserPendingDto;
import com.sallejoven.backend.model.dto.UserSelfDto;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.model.entity.UserPending;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.requestDto.UserSalleRequest;
import com.sallejoven.backend.model.types.ErrorCodes;
import com.sallejoven.backend.repository.UserPendingRepository;
import com.sallejoven.backend.repository.UserRepository;
import com.sallejoven.backend.utils.SalleConverters;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserRepository userRepository;
    private final UserPendingRepository userPendingRepository;

    private final PasswordEncoder passwordEncoder;

    private final GroupService groupService;
    private final EventService eventService;
    private final EventUserService eventUserService;
    private final AcademicStateService academicStateService;
    private final UserCenterService userCenterService;

    private final SalleConverters converters;

    @Transactional
    public UserPending registerPublic(UserSalleRequest req) throws SalleException {
        final String role = normalizeRole(req.getRol());

        if (usesCenterOnly(role)) {
            if (req.getCenterId() == null) {
                throw new SalleException(ErrorCodes.CENTER_NOT_FOUND, "Se requiere centerId para " + role);
            }
        } else {
            if (req.getGroupId() == null) {
                throw new SalleException(ErrorCodes.GROUP_NOT_FOUND, "Se requiere groupId para " + role);
            }
        }

        if (userRepository.existsByEmail(req.getEmail()) || userPendingRepository.existsByEmail(req.getEmail())) {
            throw new SalleException(ErrorCodes.EMAIL_ALREADY_EXISTS);
        }
        if (req.getDni() != null && !req.getDni().isBlank()) {
            if (userRepository.existsByDni(req.getDni()) || userPendingRepository.existsByDni(req.getDni())) {
                throw new SalleException(ErrorCodes.DNI_ALREADY_EXISTS);
            }
        }

        final UserPending pending = UserPending.builder()
                .name(req.getName())
                .lastName(req.getLastName())
                .dni(req.getDni())
                .phone(req.getPhone())
                .email(req.getEmail())
                .tshirtSize(req.getTshirtSize())
                .healthCardNumber(req.getHealthCardNumber())
                .intolerances(req.getIntolerances())
                .chronicDiseases(req.getChronicDiseases())
                .city(req.getCity())
                .address(req.getAddress())
                .imageAuthorization(req.getImageAuthorization())
                .birthDate(req.getBirthDate())
                .roles(role)
                .password(passwordEncoder.encode(req.getPassword()))
                .gender(req.getGender())
                .motherFullName(req.getMotherFullName())
                .fatherFullName(req.getFatherFullName())
                .motherEmail(req.getMotherEmail())
                .fatherEmail(req.getFatherEmail())
                .motherPhone(req.getMotherPhone())
                .fatherPhone(req.getFatherPhone())
                .centerId(req.getCenterId() != null ? req.getCenterId().longValue() : null)
                .groupId(req.getGroupId()  != null ? req.getGroupId().longValue()  : null)
                .createdAt(LocalDateTime.now())
                .build();

        return userPendingRepository.save(pending);
    }

    @Transactional
    public UserSalle approvePending(Long pendingId) throws SalleException {
        UserPending p = userPendingRepository.findById(pendingId)
                .orElseThrow(() -> new SalleException(ErrorCodes.USER_NOT_FOUND, "Solicitud no encontrada"));

        if (userRepository.existsByEmail(p.getEmail())) {
            throw new SalleException(ErrorCodes.EMAIL_ALREADY_EXISTS);
        }

        if (p.getDni() != null && !p.getDni().isBlank() && userRepository.existsByDni(p.getDni())) {
            throw new SalleException(ErrorCodes.DNI_ALREADY_EXISTS);
        }

        UserSalle user = UserSalle.builder()
                .name(p.getName())
                .lastName(p.getLastName())
                .dni(p.getDni())
                .phone(p.getPhone())
                .email(p.getEmail())
                .tshirtSize(p.getTshirtSize())
                .healthCardNumber(p.getHealthCardNumber())
                .intolerances(p.getIntolerances())
                .chronicDiseases(p.getChronicDiseases())
                .city(p.getCity())
                .address(p.getAddress())
                .imageAuthorization(p.getImageAuthorization())
                .birthDate(p.getBirthDate())
                .roles(p.getRoles())
                .password(p.getPassword())
                .gender(p.getGender())
                .motherFullName(p.getMotherFullName())
                .fatherFullName(p.getFatherFullName())
                .motherEmail(p.getMotherEmail())
                .fatherEmail(p.getFatherEmail())
                .motherPhone(p.getMotherPhone())
                .fatherPhone(p.getFatherPhone())
                .build();

        if (user.getGroups() == null) user.setGroups(new HashSet<>());
        user = userRepository.save(user);

        final String role = normalizeRole(p.getRoles());
        final int userType = mapUserTypeFromRequest(role);

        if (usesCenterOnly(role)) {
            if (p.getCenterId() == null) throw new SalleException(ErrorCodes.CENTER_NOT_FOUND);
            userCenterService.addCenterRole(user.getId(), p.getCenterId(), userType);
            userRepository.save(user);
        } else {
            if (p.getGroupId() == null) throw new SalleException(ErrorCodes.GROUP_NOT_FOUND);
            GroupSalle group = groupService.findById(p.getGroupId());

            UserGroup ug = ensureMembership(user, group, userType);
            userRepository.save(user);

            eventUserService.assignFutureGroupEventsToUser(user, group);
        }

        userPendingRepository.deleteById(pendingId);
        return user;
    }

    @Transactional
    public void rejectPending(Long id) throws SalleException {
        if (!userPendingRepository.existsById(id)) {
            throw new SalleException(ErrorCodes.USER_NOT_FOUND, "Solicitud pendiente no encontrada");
        }
        userPendingRepository.deleteById(id);
    }

    public List<UserPendingDto> listPending() {
        return userPendingRepository.findAll()
                .stream()
                .map(p -> {
                    try {
                        return converters.userPendingToDto(p);
                    } catch (SalleException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();
    }

    private String normalizeRole(String rolText) {
        String r = (rolText == null || rolText.isBlank()) ? "PARTICIPANT" : rolText.trim().toUpperCase();
        return r.startsWith("ROLE_") ? r : "ROLE_" + r;
    }

    private boolean usesCenterOnly(String role) {
        return role.endsWith("GROUP_LEADER") || role.endsWith("PASTORAL_DELEGATE");
    }

    private int mapUserTypeFromRequest(String role) {
        String r = role;
        if (r.startsWith("ROLE_")) r = r.substring(5);
        return switch (r) {
            case "GROUP_LEADER"      -> 2;
            case "ANIMATOR"          -> 1;
            case "PASTORAL_DELEGATE" -> 3;
            default                  -> 0; // PARTICIPANT
        };
    }

    private UserGroup ensureMembership(UserSalle user, GroupSalle group, int userType) throws SalleException {
        final int year = academicStateService.getVisibleYear();
        UserGroup existing = user.getGroups().stream()
                .filter(ug ->
                        ug.getGroup() != null
                                && ug.getGroup().getId().equals(group.getId())
                                && ug.getDeletedAt() == null
                                && ug.getYear() != null
                                && ug.getYear() == year)
                .findFirst()
                .orElse(null);

        if (existing != null) {
            existing.setUserType(userType);
            return existing;
        }

        UserGroup membership = UserGroup.builder()
                .user(user)
                .group(group)
                .userType(userType)
                .year(year)
                .build();

        user.getGroups().add(membership);
        return membership;
    }
}
