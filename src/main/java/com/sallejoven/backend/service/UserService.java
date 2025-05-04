package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.requestDto.UserSalleRequest;
import com.sallejoven.backend.model.types.ErrorCodes;
import com.sallejoven.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserSalle findByEmail(String email) throws SalleException {
        return userRepository.findByEmail(email).orElseThrow(() -> new SalleException(ErrorCodes.USER_NOT_FOUND));
    }

    public UserSalle findByUserId(Long id) throws SalleException {
        return userRepository.findById(id).orElseThrow(() -> new SalleException(ErrorCodes.USER_NOT_FOUND));
    }

    public UserSalle saveUser(UserSalle user) {
        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }

    public UserSalle saveUser(UserSalleRequest userRequest, Set<GroupSalle> userGroups) {
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
                .password("fdsrew")
                .roles("ROLE_PARTICIPANT")
                .gender(userRequest.getGender())
                .address(userRequest.getAddress())
                .city(userRequest.getCity())
                .build();

        user.setGroups(userGroups);        
        return userRepository.save(user);
    }

    public UserSalle updateUserFields(Long id, UserSalleRequest userDetails) throws SalleException {
        UserSalle existingUser = findByUserId(id);
    
        if (userDetails.getName() != null) {
            existingUser.setName(userDetails.getName());
        }
        if (userDetails.getLastName() != null) {
            existingUser.setLastName(userDetails.getLastName());
        }
        if (userDetails.getEmail() != null) {
            existingUser.setEmail(userDetails.getEmail());
        }
        if (userDetails.getDni() != null) {
            existingUser.setDni(userDetails.getDni());
        }
        if (userDetails.getPhone() != null) {
            existingUser.setPhone(userDetails.getPhone());
        }
        if (userDetails.getBirthDate() != null) {
            existingUser.setBirthDate(userDetails.getBirthDate());
        }
        if (userDetails.getAddress() != null) {
            existingUser.setAddress(userDetails.getAddress());
        }
        if (userDetails.getCity() != null) {
            existingUser.setCity(userDetails.getCity());
        }
        if (userDetails.getMotherFullName() != null) {
            existingUser.setMotherFullName(userDetails.getMotherFullName());
        }
        if (userDetails.getFatherFullName() != null) {
            existingUser.setFatherFullName(userDetails.getFatherFullName());
        }
        if (userDetails.getMotherEmail() != null) {
            existingUser.setMotherEmail(userDetails.getMotherEmail());
        }
        if (userDetails.getFatherEmail() != null) {
            existingUser.setFatherEmail(userDetails.getFatherEmail());
        }
        if (userDetails.getMotherPhone() != null) {
            existingUser.setMotherPhone(userDetails.getMotherPhone());
        }
        if (userDetails.getFatherPhone() != null) {
            existingUser.setFatherPhone(userDetails.getFatherPhone());
        }
        if (userDetails.getTshirtSize() != null) {
            existingUser.setTshirtSize(userDetails.getTshirtSize());
        }
        if (userDetails.getImageAuthorization() != null) {
            existingUser.setImageAuthorization(userDetails.getImageAuthorization());
        }
        if (userDetails.getChronicDiseases() != null) {
            existingUser.setChronicDiseases(userDetails.getChronicDiseases());
        }
        if (userDetails.getIntolerances() != null) {
            existingUser.setIntolerances(userDetails.getIntolerances());
        }
    
        return userRepository.save(existingUser);
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