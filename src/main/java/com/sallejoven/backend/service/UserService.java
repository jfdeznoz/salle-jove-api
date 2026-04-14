package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.Event;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserCenter;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.model.entity.UserPending;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.enums.ErrorCodes;
import com.sallejoven.backend.model.enums.Role;
import com.sallejoven.backend.model.requestDto.UserSalleRequest;
import com.sallejoven.backend.model.requestDto.UserSalleRequestOptional;
import com.sallejoven.backend.repository.EventUserRepository;
import com.sallejoven.backend.repository.RefreshTokenRepository;
import com.sallejoven.backend.repository.UserCenterRepository;
import com.sallejoven.backend.repository.UserGroupRepository;
import com.sallejoven.backend.repository.UserRepository;
import com.sallejoven.backend.repository.WeeklySessionUserRepository;
import com.sallejoven.backend.utils.ReferenceParser;
import com.sallejoven.backend.utils.TextNormalizeUtils;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EventUserService eventUserService;
    private final GroupService groupService;
    private final AcademicStateService academicStateService;
    private final EventService eventService;
    private final CenterService centerService;
    private final UserCenterService userCenterService;
    private final UserRoleHelper roleHelper;
    private final UserGroupRepository userGroupRepository;
    private final UserCenterRepository userCenterRepository;
    private final EventUserRepository eventUserRepository;
    private final WeeklySessionUserRepository weeklySessionUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public UserSalle findByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new SalleException(ErrorCodes.USER_NOT_FOUND));
    }

    public UserSalle findByUserId(UUID uuid) {
        return userRepository.findById(uuid).orElseThrow(() -> new SalleException(ErrorCodes.USER_NOT_FOUND));
    }

    public UserSalle findByReference(String reference) {
        return ReferenceParser.asUuid(reference)
                .flatMap(userRepository::findByUuid)
                .orElseThrow(() -> new SalleException(ErrorCodes.USER_NOT_FOUND));
    }

    public List<UserSalle> searchUsersSmart(String rawSearch, UserSalle me) {
        String normalized = TextNormalizeUtils.toLowerNoAccents(rawSearch).trim();
        if (normalized.length() < 5) {
            return List.of();
        }

        if (Boolean.TRUE.equals(me.getIsAdmin())) {
            return userRepository.searchUsersNormalized(normalized);
        }

        List<UserCenter> userCenters = userCenterService.findByUserForCurrentYear(me.getUuid());
        if (userCenters == null || userCenters.isEmpty()) {
            return List.of();
        }

        Set<UUID> centerUuids = userCenters.stream()
                .map(UserCenter::getCenter)
                .filter(Objects::nonNull)
                .map(Center::getUuid)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (centerUuids.isEmpty()) {
            return List.of();
        }

        return userRepository.searchUsersNormalizedByCenterUuids(normalized, centerUuids);
    }

    public List<UserSalle> searchUsers(String rawSearch) {
        String normalized = TextNormalizeUtils.toLowerNoAccents(rawSearch).trim();
        if (normalized.length() < 5) {
            return List.of();
        }
        return userRepository.searchUsersNormalized(normalized);
    }

    @Transactional
    public UserSalle updateUserFromDto(UUID uuid, UserSalleRequestOptional dto) {
        UserSalle user = findByUserId(uuid);

        dto.getName().ifPresent(value -> user.setName(trimReq(value)));
        dto.getLastName().ifPresent(value -> user.setLastName(trimReq(value)));
        dto.getEmail().ifPresent(value -> user.setEmail(trimReq(value)));
        dto.getDni().ifPresent(value -> user.setDni(trimOrNull(value)));
        dto.getPhone().ifPresent(value -> user.setPhone(trimOrNull(value)));
        dto.getTshirtSize().ifPresent(user::setTshirtSize);
        dto.getHealthCardNumber().ifPresent(value -> user.setHealthCardNumber(trimOrNull(value)));
        dto.getIntolerances().ifPresent(value -> user.setIntolerances(trimOrNull(value)));
        dto.getChronicDiseases().ifPresent(value -> user.setChronicDiseases(trimOrNull(value)));
        dto.getAddress().ifPresent(value -> user.setAddress(trimOrNull(value)));
        dto.getCity().ifPresent(value -> user.setCity(trimOrNull(value)));
        dto.getImageAuthorization().ifPresent(user::setImageAuthorization);
        dto.getBirthDate().ifPresent(user::setBirthDate);
        dto.getGender().ifPresent(user::setGender);
        dto.getMotherFullName().ifPresent(value -> user.setMotherFullName(trimOrNull(value)));
        dto.getFatherFullName().ifPresent(value -> user.setFatherFullName(trimOrNull(value)));
        dto.getMotherEmail().ifPresent(value -> user.setMotherEmail(trimOrNull(value)));
        dto.getFatherEmail().ifPresent(value -> user.setFatherEmail(trimOrNull(value)));
        dto.getMotherPhone().ifPresent(value -> user.setMotherPhone(trimOrNull(value)));
        dto.getFatherPhone().ifPresent(value -> user.setFatherPhone(trimOrNull(value)));

        return userRepository.save(user);
    }

    public UserSalle saveUser(UserSalle user) {
        normalizeUserStrings(user);
        return userRepository.save(user);
    }

    @Transactional
    public UserSalle saveUser(UserSalleRequest req) {
        if (academicStateService.isLocked()) {
            throw new SalleException(ErrorCodes.SYSTEM_LOCKED);
        }

        final String role = roleHelper.normalizeRole(req.getRol());
        final boolean isAdmin = "ROLE_ADMIN".equals(role);
        int userType = roleHelper.mapUserTypeFromRequest(role);

        final UUID centerUuid = resolveCenterUuid(req);
        final UUID eventUuid = resolveEventUuid(req);
        final UUID groupUuid = resolveGroupUuid(req);

        // Fallar rápido si el contexto es incoherente con el rol: evita persistir
        // usuarios huérfanos que luego se mostrarían como PARTICIPANT por defecto.
        roleHelper.assertScopeForRole(role, centerUuid, groupUuid);
        if (eventUuid != null && groupUuid == null) {
            throw new SalleException(ErrorCodes.GROUP_NOT_FOUND,
                    "Se requiere groupUuid para inscribir en un evento.");
        }

        UserSalle user = UserSalle.builder()
                .name(trimReq(req.getName()))
                .lastName(trimReq(req.getLastName()))
                .dni(trimOrNull(req.getDni()))
                .phone(trimOrNull(req.getPhone()))
                .email(trimReq(req.getEmail()))
                .tshirtSize(req.getTshirtSize())
                .healthCardNumber(trimOrNull(req.getHealthCardNumber()))
                .intolerances(trimOrNull(req.getIntolerances()))
                .chronicDiseases(trimOrNull(req.getChronicDiseases()))
                .imageAuthorization(req.getImageAuthorization())
                .birthDate(req.getBirthDate())
                .password(passwordEncoder.encode("password"))
                .isAdmin(isAdmin)
                .gender(req.getGender())
                .address(trimOrNull(req.getAddress()))
                .city(trimOrNull(req.getCity()))
                .motherFullName(trimOrNull(req.getMotherFullName()))
                .fatherFullName(trimOrNull(req.getFatherFullName()))
                .motherEmail(trimOrNull(req.getMotherEmail()))
                .fatherEmail(trimOrNull(req.getFatherEmail()))
                .motherPhone(trimOrNull(req.getMotherPhone()))
                .fatherPhone(trimOrNull(req.getFatherPhone()))
                .build();

        if (user.getGroups() == null) {
            user.setGroups(new java.util.HashSet<>());
        }

        UserSalle saved = userRepository.save(user);

        if (isAdmin) {
            return saved;
        }

        if (centerUuid != null && eventUuid == null && roleHelper.usesCenterOnly(role)) {
            userCenterService.addCenterRole(saved.getUuid(), centerUuid, userType);
            userRepository.save(saved);
            return saved;
        }

        if (eventUuid != null && groupUuid != null) {
            GroupSalle group = groupService.findById(groupUuid);
            if (roleHelper.isAnimator(role)) {
                userType = 5;
            }
            UserGroup userGroup = roleHelper.ensureMembership(saved, group, userType);
            saved = userRepository.save(saved);
            Event event = eventService.findById(eventUuid)
                    .orElseThrow(() -> new SalleException(ErrorCodes.EVENT_NOT_FOUND));
            eventUserService.assignEventToUserGroups(event, List.of(userGroup));
            return saved;
        }

        if (groupUuid != null) {
            GroupSalle group = groupService.findById(groupUuid);
            roleHelper.ensureMembership(saved, group, userType);
            saved = userRepository.save(saved);
            eventUserService.assignFutureGroupEventsToUser(saved, group);
            return saved;
        }

        return saved;
    }

    @Transactional
    public void addUserToGroup(UUID userUuid, UUID groupUuid, Integer userType) {
        int year = academicStateService.getVisibleYear();
        GroupSalle group = groupService.findById(groupUuid);
        UserSalle user = findByUserId(userUuid);

        UserGroup membership = UserGroup.builder()
                .user(user)
                .group(group)
                .userType(userType)
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

    public Optional<UserSalle> findById(UUID uuid) {
        return userRepository.findById(uuid);
    }

    public Optional<UserSalle> findByReferenceOptional(String reference) {
        return ReferenceParser.asUuid(reference).flatMap(userRepository::findByUuid);
    }

    @Transactional
    public void deleteUser(UUID uuid) {
        UserSalle user = findByUserId(uuid);
        LocalDateTime when = LocalDateTime.now();

        if (user.getGroups() != null && !user.getGroups().isEmpty()) {
            List<UUID> userGroupUuids = user.getGroups().stream()
                    .map(UserGroup::getUuid)
                    .filter(Objects::nonNull)
                    .toList();

            user.getGroups().forEach(userGroup -> userGroup.setDeletedAt(when));
            if (!userGroupUuids.isEmpty()) {
                eventUserService.softDeleteByUserGroupIds(userGroupUuids, when);
            }
        }
        user.setDeletedAt(when);
        userRepository.save(user);
    }

    public List<UserSalle> getUsersByStages(List<Integer> stages) {
        return userRepository.findUsersByStages(stages);
    }

    public List<UserSalle> getUsersByGroupId(UUID groupUuid) {
        return userRepository.findUsersByGroupUuid(groupUuid);
    }

    public List<UserSalle> getUsersByCenterId(UUID centerUuid) {
        return userRepository.findUsersByCenterUuid(centerUuid);
    }

    public List<UserSalle> getCatechistsByCenter(UUID centerUuid) {
        return userRepository.findAnimatorsByCenterAndYear(centerUuid, academicStateService.getVisibleYear());
    }

    public List<Role> getUserRoles(UserPending userPending) {
        return Arrays.stream(userPending.getRoles().split(","))
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
        }
    }

    private UUID resolveCenterUuid(UserSalleRequest req) {
        if (req.getCenterUuid() != null && !req.getCenterUuid().isBlank()) {
            return centerService.findByReference(req.getCenterUuid()).getUuid();
        }
        return null;
    }

    private UUID resolveGroupUuid(UserSalleRequest req) {
        if (req.getGroupUuid() != null && !req.getGroupUuid().isBlank()) {
            return groupService.findByReference(req.getGroupUuid()).getUuid();
        }
        return null;
    }

    private UUID resolveEventUuid(UserSalleRequest req) {
        if (req.getEventUuid() != null && !req.getEventUuid().isBlank()) {
            return eventService.findByReference(req.getEventUuid())
                    .orElseThrow(() -> new SalleException(ErrorCodes.EVENT_NOT_FOUND))
                    .getUuid();
        }
        return null;
    }

    @Transactional(readOnly = true)
    public List<UserSalle> findDeletedUsers(String rawSearch) {
        if (rawSearch == null || rawSearch.isBlank()) {
            return userRepository.findAllByDeletedAtIsNotNullOrderByDeletedAtDesc(PageRequest.of(0, 100));
        }
        String normalized = TextNormalizeUtils.toLowerNoAccents(rawSearch).trim();
        if (normalized.length() < 3) {
            return List.of();
        }
        return userRepository.findDeletedByNormalized(normalized);
    }

    @Transactional
    public UserSalle reactivate(UUID baseUuid, UUID mergeFromUuid) {
        if (mergeFromUuid != null && mergeFromUuid.equals(baseUuid)) {
            throw new SalleException(ErrorCodes.INVALID_MERGE_TARGET);
        }

        UserSalle base;
        UserSalle mergeFrom = null;
        if (mergeFromUuid != null) {
            // Lock in deterministic UUID order to avoid deadlocks when two
            // concurrent reactivations touch the same pair of users.
            if (baseUuid.compareTo(mergeFromUuid) <= 0) {
                base = userRepository.findByIdForUpdate(baseUuid)
                        .orElseThrow(() -> new SalleException(ErrorCodes.USER_NOT_FOUND));
                mergeFrom = userRepository.findByIdForUpdate(mergeFromUuid)
                        .orElseThrow(() -> new SalleException(ErrorCodes.USER_NOT_FOUND));
            } else {
                mergeFrom = userRepository.findByIdForUpdate(mergeFromUuid)
                        .orElseThrow(() -> new SalleException(ErrorCodes.USER_NOT_FOUND));
                base = userRepository.findByIdForUpdate(baseUuid)
                        .orElseThrow(() -> new SalleException(ErrorCodes.USER_NOT_FOUND));
            }
        } else {
            base = userRepository.findByIdForUpdate(baseUuid)
                    .orElseThrow(() -> new SalleException(ErrorCodes.USER_NOT_FOUND));
        }

        if (base.getDeletedAt() == null) {
            throw new SalleException(ErrorCodes.USER_NOT_DELETED);
        }

        assertEmailNotInUseByOthers(base, mergeFrom);

        if (mergeFrom != null) {
            mergeScalarFields(base, mergeFrom);
            migrateMemberships(base, mergeFrom);
            refreshTokenRepository.deleteByUserUuid(mergeFrom.getUuid());
            if (mergeFrom.getDeletedAt() == null) {
                mergeFrom.setDeletedAt(LocalDateTime.now());
                userRepository.save(mergeFrom);
            }
        }

        reactivateOwnRelations(base.getUuid());
        base.setDeletedAt(null);
        refreshTokenRepository.deleteByUserUuid(base.getUuid());
        return userRepository.save(base);
    }

    private void assertEmailNotInUseByOthers(UserSalle base, UserSalle mergeFrom) {
        if (base.getEmail() == null) {
            return;
        }
        userRepository.findActiveByEmail(base.getEmail()).ifPresent(existing -> {
            if (existing.getUuid().equals(base.getUuid())) {
                return;
            }
            if (mergeFrom != null && existing.getUuid().equals(mergeFrom.getUuid())) {
                return;
            }
            throw new SalleException(ErrorCodes.EMAIL_IN_USE);
        });
    }

    private void mergeScalarFields(UserSalle base, UserSalle from) {
        if (isBlank(base.getName())) base.setName(from.getName());
        if (isBlank(base.getLastName())) base.setLastName(from.getLastName());
        if (isBlank(base.getDni())) base.setDni(from.getDni());
        if (isBlank(base.getPhone())) base.setPhone(from.getPhone());
        if (isBlank(base.getEmail())) base.setEmail(from.getEmail());
        if (base.getTshirtSize() == null) base.setTshirtSize(from.getTshirtSize());
        if (isBlank(base.getHealthCardNumber())) base.setHealthCardNumber(from.getHealthCardNumber());
        if (isBlank(base.getIntolerances())) base.setIntolerances(from.getIntolerances());
        if (isBlank(base.getChronicDiseases())) base.setChronicDiseases(from.getChronicDiseases());
        if (isBlank(base.getAddress())) base.setAddress(from.getAddress());
        if (isBlank(base.getCity())) base.setCity(from.getCity());
        if (base.getBirthDate() == null) base.setBirthDate(from.getBirthDate());
        if (base.getImageAuthorization() == null) base.setImageAuthorization(from.getImageAuthorization());
        if (base.getGender() == null) base.setGender(from.getGender());
        if (isBlank(base.getMotherFullName())) base.setMotherFullName(from.getMotherFullName());
        if (isBlank(base.getFatherFullName())) base.setFatherFullName(from.getFatherFullName());
        if (isBlank(base.getMotherEmail())) base.setMotherEmail(from.getMotherEmail());
        if (isBlank(base.getFatherEmail())) base.setFatherEmail(from.getFatherEmail());
        if (isBlank(base.getMotherPhone())) base.setMotherPhone(from.getMotherPhone());
        if (isBlank(base.getFatherPhone())) base.setFatherPhone(from.getFatherPhone());
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private void migrateMemberships(UserSalle base, UserSalle from) {
        migrateUserGroupsFrom(base, from);
        migrateUserCentersFrom(base, from);
        migrateEventUsersFrom(base, from);
        migrateWeeklySessionUsersFrom(base, from);
    }

    private void migrateUserGroupsFrom(UserSalle base, UserSalle source) {
        UUID baseUuid = base.getUuid();
        userGroupRepository.findAllByUserIncludingDeleted(source.getUuid()).forEach(ug -> {
            UUID groupUuid = ug.getGroup().getUuid();
            Integer year = ug.getYear();
            boolean conflict = userGroupRepository
                    .findActiveByUserGroupYear(baseUuid, groupUuid, year)
                    .isPresent();
            if (!conflict) {
                ug.setUser(base);
                ug.setDeletedAt(null);
                userGroupRepository.save(ug);
            }
        });
    }

    private void migrateUserCentersFrom(UserSalle base, UserSalle source) {
        UUID baseUuid = base.getUuid();
        userCenterRepository.findAllByUserIncludingDeleted(source.getUuid()).forEach(uc -> {
            UUID centerUuid = uc.getCenter().getUuid();
            Integer year = uc.getYear();
            boolean conflict = userCenterRepository
                    .findActiveByUserCenterYear(baseUuid, centerUuid, year)
                    .isPresent();
            if (!conflict) {
                uc.setUser(base);
                uc.setDeletedAt(null);
                userCenterRepository.save(uc);
            }
        });
    }

    private void migrateEventUsersFrom(UserSalle base, UserSalle source) {
        UUID baseUuid = base.getUuid();
        eventUserRepository.findAllByUserIncludingDeleted(source.getUuid()).forEach(eu -> {
            UUID eventUuid = eu.getEvent().getUuid();
            boolean conflict = eventUserRepository
                    .findActiveByEventAndUser(eventUuid, baseUuid)
                    .isPresent();
            if (!conflict) {
                eu.setUser(base);
                eu.setDeletedAt(null);
                eventUserRepository.save(eu);
            }
        });
    }

    private void migrateWeeklySessionUsersFrom(UserSalle base, UserSalle source) {
        UUID baseUuid = base.getUuid();
        weeklySessionUserRepository.findAllByUserIncludingDeleted(source.getUuid()).forEach(wsu -> {
            UUID sessionUuid = wsu.getWeeklySession().getUuid();
            boolean conflict = weeklySessionUserRepository
                    .findActiveBySessionAndUser(sessionUuid, baseUuid)
                    .isPresent();
            if (!conflict) {
                wsu.setUser(base);
                wsu.setDeletedAt(null);
                weeklySessionUserRepository.save(wsu);
            }
        });
    }

    private void reactivateOwnRelations(UUID userUuid) {
        userGroupRepository.reactivateByUser(userUuid);
        userCenterRepository.reactivateByUser(userUuid);
        eventUserRepository.reactivateByUser(userUuid);
        weeklySessionUserRepository.reactivateByUser(userUuid);
    }

    private static void normalizeUserStrings(UserSalle user) {
        if (user == null) {
            return;
        }

        user.setName(trimReq(user.getName()));
        user.setLastName(trimReq(user.getLastName()));
        user.setEmail(trimReq(user.getEmail()));
        user.setDni(trimOrNull(user.getDni()));
        user.setPhone(trimOrNull(user.getPhone()));
        user.setHealthCardNumber(trimOrNull(user.getHealthCardNumber()));
        user.setIntolerances(trimOrNull(user.getIntolerances()));
        user.setChronicDiseases(trimOrNull(user.getChronicDiseases()));
        user.setAddress(trimOrNull(user.getAddress()));
        user.setCity(trimOrNull(user.getCity()));
        user.setMotherFullName(trimOrNull(user.getMotherFullName()));
        user.setFatherFullName(trimOrNull(user.getFatherFullName()));
        user.setMotherEmail(trimOrNull(user.getMotherEmail()));
        user.setFatherEmail(trimOrNull(user.getFatherEmail()));
        user.setMotherPhone(trimOrNull(user.getMotherPhone()));
        user.setFatherPhone(trimOrNull(user.getFatherPhone()));
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
