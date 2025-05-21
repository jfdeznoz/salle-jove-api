package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.entity.Event;
import com.sallejoven.backend.model.entity.EventUser;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.enums.Role;
import com.sallejoven.backend.model.requestDto.UserSalleRequest;
import com.sallejoven.backend.model.requestDto.UserSalleRequestOptional;
import com.sallejoven.backend.model.types.ErrorCodes;
import com.sallejoven.backend.repository.EventUserRepository;
import com.sallejoven.backend.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EventUserService eventUserService;

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

    public UserSalle saveUser(UserSalleRequest userRequest) {
        UserSalle user = UserSalle.builder()
                .name(userRequest.getName())
                .lastName(userRequest.getLastName())
                .dni(userRequest.getDni())
                .phone(userRequest.getPhone())
                .email(userRequest.getEmail())
                .tshirtSize(userRequest.getTshirtSize())
                .healthCardNumber(userRequest.getHealthCardNumber())
                .intolerances(userRequest.getIntolerances())
                .chronicDiseases(userRequest.getChronicDiseases())
                .imageAuthorization(userRequest.getImageAuthorization())
                .birthDate(userRequest.getBirthDate())
                .password(passwordEncoder.encode("password"))
                .roles("ROLE_" + userRequest.getRol())
                .gender(userRequest.getGender())
                .address(userRequest.getAddress())
                .city(userRequest.getCity())
                .build();

        return userRepository.save(user);
    }

    @Transactional
    public UserSalle saveUser(UserSalleRequest userRequest, Set<GroupSalle> userGroups) {
        // 1) Guardamos el usuario
        UserSalle user = UserSalle.builder()
            .name(userRequest.getName())
            .lastName(userRequest.getLastName())
            .dni(userRequest.getDni())
            .phone(userRequest.getPhone())
            .email(userRequest.getEmail())
            .tshirtSize(userRequest.getTshirtSize())
            .healthCardNumber(userRequest.getHealthCardNumber())
            .intolerances(userRequest.getIntolerances())
            .chronicDiseases(userRequest.getChronicDiseases())
            .imageAuthorization(userRequest.getImageAuthorization())
            .birthDate(userRequest.getBirthDate())
            .password(passwordEncoder.encode("password"))
            .roles("ROLE_" + userRequest.getRol())
            .gender(userRequest.getGender())
            .address(userRequest.getAddress())
            .city(userRequest.getCity())
            .build();

            user.setGroups(userGroups);
            UserSalle savedUser = userRepository.save(user);

        Integer eventId = userRequest.getEventId();

        if (eventId != null) {
            eventUserService.assignEventToUsers(eventId, List.of(savedUser));
        }

        return savedUser;
    }

    public void addUserToGroup(UserSalle user, GroupSalle group) {
        user.getGroups().add(group);
        saveUser(user);
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

    public void deleteUser(Long id) throws SalleException {
        UserSalle userSalle = findByUserId(id);

        userSalle.setDeletedAt(LocalDateTime.now());
        userRepository.save(userSalle);
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