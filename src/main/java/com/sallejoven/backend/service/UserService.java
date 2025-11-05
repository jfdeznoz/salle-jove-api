package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.Event;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserCenter;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.model.entity.UserPending;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.enums.Role;
import com.sallejoven.backend.model.requestDto.UserSalleRequest;
import com.sallejoven.backend.model.requestDto.UserSalleRequestOptional;
import com.sallejoven.backend.model.enums.ErrorCodes;
import com.sallejoven.backend.repository.UserRepository;
import com.sallejoven.backend.utils.TextNormalizeUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EventUserService eventUserService;
    private final GroupService groupService;
    private final AcademicStateService academicStateService;
    private final EventService eventService;
    private final UserCenterService userCenterService;

    public UserSalle findByEmail(String email) throws SalleException {
        return userRepository.findByEmail(email).orElseThrow(() -> new SalleException(ErrorCodes.USER_NOT_FOUND));
    }

    public UserSalle findByUserId(Long id) throws SalleException {
        return userRepository.findById(id).orElseThrow(() -> new SalleException(ErrorCodes.USER_NOT_FOUND));
    }

    public List<UserSalle> searchUsersSmart(String rawSearch, UserSalle me) throws SalleException {
        String normalized = TextNormalizeUtils.toLowerNoAccents(rawSearch).trim();
        if (normalized.length() < 5) return List.of();

        if (Boolean.TRUE.equals(me.getIsAdmin())) {
            return userRepository.searchUsersNormalized(normalized); // global
        }

        List<UserCenter> userCenters = userCenterService.findByUserForCurrentYear(me.getId());
        if (userCenters == null || userCenters.isEmpty()) return List.of();

        Set<Long> centerIds = userCenters.stream()
                .map(UserCenter::getCenter)
                .filter(java.util.Objects::nonNull)
                .map(Center::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (centerIds.isEmpty()) return List.of();

        return userRepository.searchUsersNormalizedByCenters(normalized, centerIds);
    }

    public List<UserSalle> searchUsers(String rawSearch) {
        String normalized = TextNormalizeUtils.toLowerNoAccents(rawSearch).trim();
        if (normalized.length() < 5) {
            return List.of();
        }
        return userRepository.searchUsersNormalized(normalized);
    }

    public UserSalle updateUserFromDto(Long id, UserSalleRequestOptional dto) throws SalleException {
        UserSalle user = findByUserId(id);
    
        dto.getName().ifPresent(user::setName);
        dto.getLastName().ifPresent(user::setLastName);
        dto.getEmail().ifPresent(user::setEmail);
        dto.getDni().ifPresent(user::setDni);
        dto.getPhone().ifPresent(user::setPhone);
        dto.getTshirtSize().ifPresent(user::setTshirtSize);
        dto.getHealthCardNumber().ifPresent(user::setHealthCardNumber);
        dto.getIntolerances().ifPresent(user::setIntolerances);
        dto.getChronicDiseases().ifPresent(user::setChronicDiseases);
        dto.getAddress().ifPresent(user::setAddress);
        dto.getCity().ifPresent(user::setCity);
        dto.getImageAuthorization().ifPresent(user::setImageAuthorization);
        dto.getBirthDate().ifPresent(user::setBirthDate);
        dto.getGender().ifPresent(user::setGender);
        dto.getMotherFullName().ifPresent(user::setMotherFullName);
        dto.getFatherFullName().ifPresent(user::setFatherFullName);
        dto.getMotherEmail().ifPresent(user::setMotherEmail);
        dto.getFatherEmail().ifPresent(user::setFatherEmail);
        dto.getMotherPhone().ifPresent(user::setMotherPhone);
        dto.getFatherPhone().ifPresent(user::setFatherPhone);
    
        return userRepository.save(user);
    }    

    public UserSalle saveUser(UserSalle user) {
        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }

    @Transactional
    public UserSalle saveUser(UserSalleRequest req) throws SalleException {
        if (academicStateService.isLocked()) {
            throw new SalleException(ErrorCodes.SYSTEM_LOCKED);
        }

        // 0) Normalización y derivación
        final String role = normalizeRole(req.getRol());   // "ROLE_*" (si viene null → ROLE_PARTICIPANT)
        final boolean isAdmin = "ROLE_ADMIN".equals(role);
        int userType = mapUserTypeFromRequest(role);       // 0..3 normalmente

        // 1) Construir y persistir usuario base
        UserSalle user = UserSalle.builder()
                .name(req.getName())
                .lastName(req.getLastName())
                .dni(req.getDni())
                .phone(req.getPhone())
                .email(req.getEmail())
                .tshirtSize(req.getTshirtSize())
                .healthCardNumber(req.getHealthCardNumber())
                .intolerances(req.getIntolerances())
                .chronicDiseases(req.getChronicDiseases())
                .imageAuthorization(req.getImageAuthorization())
                .birthDate(req.getBirthDate())
                .password(passwordEncoder.encode("password"))
                .isAdmin(isAdmin)                     // ← **nuevo**
                .gender(req.getGender())
                .address(req.getAddress())
                .city(req.getCity())
                .build();

        if (user.getGroups() == null) {
            user.setGroups(new java.util.HashSet<>());
        }

        UserSalle saved = userRepository.save(user);

        // 2) Orquestación
        final Integer centerId = req.getCenterId();
        final Integer eventId  = req.getEventId();
        final Integer groupId  = req.getGroupId();

        // Si es admin: no asignamos roles de centro/grupo
        if (isAdmin) {
            return saved;
        }

        // Validación: eventId sin groupId no tiene sentido
        if (eventId != null && groupId == null) {
            throw new SalleException(ErrorCodes.GROUP_NOT_FOUND, "Se requiere groupId para inscribir en un evento.");
        }

        // --- Caso A) Alta por centro (PD / GL) y NO es un alta puntual de evento ---
        if (centerId != null && eventId == null && usesCenterOnly(role)) {
            // Asigna el rol de centro explícito (2=GL, 3=PD)
            userCenterService.addCenterRole(saved.getId(), centerId.longValue(), userType);
            userRepository.save(saved);
            return saved;
        }

        // --- Caso B) Alta puntual a un evento (requiere grupo) ---
        if (eventId != null && groupId != null) {
            GroupSalle group = groupService.findById(groupId.longValue());

            // Tu especial: si se crea "como animador" para evento puntual, usas 5
            if (isAnimator(role)) {
                userType = 5;
            }

            UserGroup ug = ensureMembership(saved, group, userType);
            saved = userRepository.save(saved); // asegurar ug.id

            Event event = eventService.findById(eventId.longValue())
                    .orElseThrow(() -> new SalleException(ErrorCodes.EVENT_NOT_FOUND));

            eventUserService.assignEventToUserGroups(event, java.util.List.of(ug));
            return saved;
        }

        // --- Caso C) Alta normal a un grupo ---
        if (groupId != null) {
            GroupSalle group = groupService.findById(groupId.longValue());
            ensureMembership(saved, group, userType);
            saved = userRepository.save(saved);
            eventUserService.assignFutureGroupEventsToUser(saved, group);
            return saved;
        }

        // Sin centro/grupo/evento → solo usuario base
        return saved;
    }

    /* ===== helpers iguales a los de RegistrationService ===== */

    private String normalizeRole(String rolText) {
        String r = (rolText == null || rolText.isBlank()) ? "PARTICIPANT" : rolText.trim().toUpperCase();
        return r.startsWith("ROLE_") ? r : "ROLE_" + r;
    }

    private boolean usesCenterOnly(String role) {
        return role.endsWith("GROUP_LEADER") || role.endsWith("PASTORAL_DELEGATE");
    }
    private boolean isAnimator(String role) {
        return role.endsWith("ANIMATOR");
    }

    private int mapUserTypeFromRequest(String role) {
        String r = role.startsWith("ROLE_") ? role.substring(5) : role;
        return switch (r) {
            case "GROUP_LEADER"      -> 2;
            case "PASTORAL_DELEGATE" -> 3;
            case "ANIMATOR"          -> 1;
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

    public void addUserToGroup(Long userId, Long groupId, Long userType) throws SalleException {
        final int year = academicStateService.getVisibleYear();

        GroupSalle group = groupService.findById(groupId);

        UserSalle user = findByUserId(userId);


        UserGroup membership = UserGroup.builder()
                .user(user)
                .group(group)
                .userType(userType.intValue())
                .year(year)
                .build();

        user.getGroups().add(membership);
        saveUser(user);
        eventUserService.assignFutureGroupEventsToUser(user, group);
    }       

    public List<UserSalle> findAllUsers() {
        return userRepository.findAll();
    }

    public boolean existsByDni(String dni) {
        return userRepository.existsByDni(dni);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public Optional<UserSalle> findById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional
    public void deleteUser(Long id) throws SalleException {
        UserSalle user = findByUserId(id);
        LocalDateTime now = LocalDateTime.now();

        if (user.getGroups() != null && !user.getGroups().isEmpty()) {
            List<Long> ugIds = user.getGroups().stream()
                    .map(UserGroup::getId)
                    .filter(java.util.Objects::nonNull)
                    .toList();

            user.getGroups().forEach(ug -> ug.setDeletedAt(now));

            if (!ugIds.isEmpty()) {
                eventUserService.softDeleteByUserGroupIds(ugIds, now);
            }
        }
        user.setDeletedAt(now);
        userRepository.save(user);
    }

    public List<UserSalle> getUsersByStages(List<Integer> stages) {
        return userRepository.findUsersByStages(stages);
    }

    public List<UserSalle> getUsersByGroupId(Long groupId) {
        return userRepository.findUsersByGroupId(groupId);
    }

    public List<UserSalle> getUsersByCenterId(Long centerId) {
            return userRepository.findUsersByCenterId(centerId);
    }

    public List<UserSalle> getCatechistsByCenter(Long centerId) throws SalleException {
        int year = academicStateService.getVisibleYear();
        return userRepository.findAnimatorsByCenterAndYear(centerId, year);
    }

    public List<Role> getUserRoles(UserPending userSalle) throws SalleException {
        return Arrays.stream(userSalle.getRoles().split(","))
                .map(String::trim)
                .map(role -> role.replace("ROLE_", ""))
                .map(String::toUpperCase)
                .map(Role::valueOf)
                .toList();
    }

    @Transactional
    public void removeUserFromGroup(UserSalle user, GroupSalle group) {
        if (user.getGroups().remove(group)) {
            userRepository.save(user);
            System.out.println("✅ Grupo eliminado de usuario: " 
                + user.getName() + " " + user.getLastName());
        } else {
            System.out.println("ℹ️ El usuario no pertenecía a ese grupo: " 
                + user.getName() + " " + user.getLastName());
        }
    }
}