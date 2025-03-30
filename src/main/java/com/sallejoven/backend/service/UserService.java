package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.types.ErrorCodes;
import com.sallejoven.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final GroupService groupService;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, GroupService groupService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.groupService = groupService;
    }

    public UserSalle findByEmail(String email) throws SalleException {
        return userRepository.findByEmail(email).orElseThrow(() -> new SalleException(ErrorCodes.USER_NOT_FOUND));
    }

    public UserSalle saveUser(UserSalle user) {
        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }

    public UserSalle saveUser(UserSalle user, Long groupId) {
        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        Optional<GroupSalle> group = groupService.findById(groupId);
        if (group.isPresent()) {
            Set<GroupSalle> userGroups = user.getGroups();
            if (userGroups == null) {
                userGroups = new HashSet<>();
            }
            userGroups.add(group.get());
            user.setGroups(userGroups);
        }

        return userRepository.save(user);
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

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
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