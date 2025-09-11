package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.entity.Event;
import com.sallejoven.backend.model.entity.EventGroup;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.enums.Role;
import com.sallejoven.backend.model.requestDto.UserSalleRequest;
import com.sallejoven.backend.model.requestDto.UserSalleRequestOptional;
import com.sallejoven.backend.model.types.ErrorCodes;
import com.sallejoven.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EventUserService eventUserService;
    private final EventGroupService eventGroupService;
    private final GroupService groupService;

    public UserSalle findByEmail(String email) throws SalleException {
        return userRepository.findByEmail(email).orElseThrow(() -> new SalleException(ErrorCodes.USER_NOT_FOUND));
    }

    public UserSalle findByUserId(Long id) throws SalleException {
        return userRepository.findById(id).orElseThrow(() -> new SalleException(ErrorCodes.USER_NOT_FOUND));
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
                .roles(req.getRol() != null ? "ROLE_" + req.getRol() : null)
                .gender(req.getGender())
                .address(req.getAddress())
                .city(req.getCity())
                .build();

        if (user.getGroups() == null) {
            user.setGroups(new HashSet<>());
        }

        UserSalle saved = userRepository.save(user);

        // 2) Orquestación
        final Integer centerId = req.getCenterId();
        final Integer eventId  = req.getEventId();
        final Integer groupId  = req.getGroupId();
        int userType = mapUserTypeFromRequest(req.getRol());

        // Validación: eventId sin groupId
        if (eventId != null && groupId == null) {
            throw new SalleException(ErrorCodes.GROUP_NOT_FOUND, "Se requiere groupId para inscribir en un evento.");
        }

        if (centerId != null && eventId == null) {
            List<GroupSalle> centerGroups = groupService.findGroupsByCenterId(centerId.longValue());
            if (centerGroups != null) {
                for (GroupSalle g : centerGroups) {
                    ensureMembership(saved, g, userType);
                    saved = userRepository.save(saved);
                    assignFutureGroupEventsToUser(saved, g);
                }
            }
            return saved;
        }

        if (eventId != null && groupId != null) {
            GroupSalle group = groupService.findById(groupId.longValue())
                    .orElseThrow(() -> new SalleException(ErrorCodes.GROUP_NOT_FOUND));

            if (isAnimator(req.getRol())) {
                userType = 5;
            }

            UserGroup ug = ensureMembership(saved, group, userType);
            saved = userRepository.save(saved); // asegurar ug.id

            Long ugId = ug.getId();
            if (ugId == null) {
                saved = userRepository.save(saved);
                ugId = ug.getId();
            }
            eventUserService.assignEventToUserGroups(eventId.longValue(), List.of(ugId));
            return saved;
        }

        // (C) groupId SIN evento => membership + eventos futuros
        if (groupId != null) {
            GroupSalle group = groupService.findById(groupId.longValue())
                    .orElseThrow(() -> new SalleException(ErrorCodes.GROUP_NOT_FOUND));
            ensureMembership(saved, group, userType);
            saved = userRepository.save(saved);
            assignFutureGroupEventsToUser(saved, group);
            return saved;
        }

        // (D) sin centro/grupo/evento => solo usuario
        return saved;
    }

    private boolean isAnimator(String rolText) {
        if (rolText == null) return false;
        String r = rolText.trim().toUpperCase();
        if (r.startsWith("ROLE_")) r = r.substring(5);
        return "ANIMATOR".equals(r);
    }

    private UserGroup ensureMembership(UserSalle user, GroupSalle group, int userType) {
        UserGroup existing = user.getGroups().stream()
                .filter(ug -> ug.getGroup() != null
                        && ug.getGroup().getId().equals(group.getId())
                        && ug.getDeletedAt() == null)
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
                .build();

        user.getGroups().add(membership);
        return membership;
    }

    /** Mapea el rol textual del request al código user_type (0..3). */
    private int mapUserTypeFromRequest(String rolText) {
        if (rolText == null) return 0; // PARTICIPANT por defecto
        String r = rolText.trim().toUpperCase();
        if (r.startsWith("ROLE_")) r = r.substring(5);

        return switch (r) {
            case "GROUP_LEADER"      -> 2;
            case "ANIMATOR"          -> 1;
            case "PASTORAL_DELEGATE" -> 3; // por si lo usas en grupos
            default                  -> 0; // PARTICIPANT
        };
    }

    public void addUserToGroup(Long userId, Long groupId, Long userType) throws SalleException {
        GroupSalle group = groupService.findById(groupId)
                .orElseThrow(() -> new SalleException(ErrorCodes.GROUP_NOT_FOUND));

        UserSalle user = findByUserId(userId);


        UserGroup membership = UserGroup.builder()
                .user(user)
                .group(group)
                .userType(userType.intValue())
                .build();

        user.getGroups().add(membership);
        saveUser(user);
        assignFutureGroupEventsToUser(user, group);
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

    public List<UserSalle> getUsersByCenterId(Long centerId, String role) {
        if (role == null || role.isBlank()) {
            return userRepository.findUsersByCenterId(centerId);
        } else {
            return userRepository.findUsersByCenterIdAndRole(centerId, role);
        }
    }
     

    public List<Role> getUserRoles(UserSalle userSalle) throws SalleException {
        return Arrays.stream(userSalle.getRoles().split(","))
                .map(String::trim)
                .map(role -> role.replace("ROLE_", ""))
                .map(String::toUpperCase)
                .map(Role::valueOf)
                .toList();
    }  

    public List<UserSalle> findAllByRoles() {
        return userRepository.findAllByRoles("ROLE_PARTICIPANT", "ROLE_ANIMATOR");
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

    @Transactional
    public void moveUserBetweenGroups(Long userId, Long fromGroupId, Long toGroupId) throws SalleException {

        if (fromGroupId.equals(toGroupId)) return;

        UserSalle user = findByUserId(userId);

        GroupSalle from = groupService.findById(fromGroupId)
                .orElseThrow(() -> new SalleException(ErrorCodes.GROUP_NOT_FOUND));
        GroupSalle to = groupService.findById(toGroupId)
                .orElseThrow(() -> new SalleException(ErrorCodes.GROUP_NOT_FOUND));

        // 1) localizar membership origen (user + fromGroup)
        UserGroup source = user.getGroups().stream()
                .filter(m -> m.getGroup().getId().equals(fromGroupId))
                .findFirst()
                .orElseThrow(() -> new SalleException(ErrorCodes.USER_GROUP_NOT_ASSIGNED));

        // 2) conservar tipo
        Integer type = source.getUserType();

        // 3) quitar membership origen (orphanRemoval=true la borrará en BD si tu mapeo lo tiene)
        user.getGroups().remove(source);

        // 4) crear/actualizar membership destino con mismo tipo
        UserGroup dest = user.getGroups().stream()
                .filter(m -> m.getGroup().getId().equals(toGroupId))
                .findFirst()
                .orElseGet(() -> {
                    UserGroup ug = UserGroup.builder()
                            .user(user)
                            .group(to)
                            .userType(type)
                            .build();
                    user.getGroups().add(ug);
                    return ug;
                });

        dest.setUserType(type);

        // 5) persistir cambios en memberships
        UserSalle updated = userRepository.save(user);

        // 6) inscribir al usuario (vía su UserGroup destino) en los eventos futuros del nuevo grupo
        assignFutureGroupEventsToUser(updated, to);
    }

    private void assignFutureGroupEventsToUser(UserSalle user, GroupSalle group) {
        // 0) buscar la membership (UserGroup) de este usuario en este grupo
        UserGroup membership = user.getGroups().stream()
                .filter(m -> m.getGroup().getId().equals(group.getId()))
                .findFirst()
                .orElse(null);

        if (membership == null) {
            // el usuario ya no pertenece a este grupo; no inscribir
            return;
        }

        // 1) eventos del grupo
        List<EventGroup> egList = eventGroupService.getEventGroupsByGroupId(group.getId());

        // 2) filtrar eventos futuros (si endDate es null, usamos eventDate)
        LocalDate today = LocalDate.now();
        List<Event> upcoming = egList.stream()
                .map(EventGroup::getEvent)
                .filter(evt -> {
                    LocalDate end = evt.getEndDate() != null ? evt.getEndDate() : evt.getEventDate();
                    return end != null && !end.isBefore(today);
                })
                .toList();

        // 3) inscribir el UserGroup en esos eventos (evita duplicados por UNIQUE(event,user_group_id))
        for (Event e : upcoming) {
            eventUserService.assignEventToUserGroups(e, java.util.List.of(membership));
        }
    }

    /*@Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserSalle user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // Convertir los roles del usuario en GrantedAuthority para Spring Security
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                authorities
        );
    }*/

}