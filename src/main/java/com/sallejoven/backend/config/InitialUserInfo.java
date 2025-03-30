package com.sallejoven.backend.config;

import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Component
@Slf4j
public class InitialUserInfo implements CommandLineRunner {
    private final UserRepository userInfoRepo;
    private final PasswordEncoder passwordEncoder;

    
    @Override
    public void run(String... args) throws Exception {

        /*
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println("Admin: " + encoder.encode("password"));
        System.out.println("Pastoral Delegate: " + encoder.encode("password"));
        System.out.println("Group Leader: " + encoder.encode("password"));
        System.out.println("Animator: " + encoder.encode("password"));
        System.out.println("Participant: " + encoder.encode("password"));


        UserSalle admin = UserSalle.builder()
                .name("Admin")
                .lastName("Admin")
                .email("admin@admin.com")
                .password(passwordEncoder.encode("password"))
                .roles("ROLE_ADMIN")
                .imageAuthorization(true) // Proporcionar valor para image_authorization
                .birthDate(java.sql.Date.valueOf(LocalDate.of(1990, 1, 1))) // Proporcionar valor para birth_date
                .build();

        UserSalle pastoralDelegate = UserSalle.builder()
                .name("Pastoral Delegate")
                .lastName("Delegate")
                .email("pastoral@delegate.com")
                .password(passwordEncoder.encode("password"))
                .roles("ROLE_PASTORAL_DELEGATE")
                .imageAuthorization(true) // Proporcionar valor para image_authorization
                .birthDate(java.sql.Date.valueOf(LocalDate.of(1985, 5, 15))) // Proporcionar valor para birth_date
                .build();

        UserSalle groupLeader = UserSalle.builder()
                .name("Group Leader")
                .lastName("Leader")
                .email("group@leader.com")
                .password(passwordEncoder.encode("password"))
                .roles("ROLE_GROUP_LEADER")
                .imageAuthorization(true) // Proporcionar valor para image_authorization
                .birthDate(java.sql.Date.valueOf(LocalDate.of(1992, 7, 20))) // Proporcionar valor para birth_date
                .build();

        UserSalle animator = UserSalle.builder()
                .name("Animator")
                .lastName("Animator")
                .email("animator@animator.com")
                .password(passwordEncoder.encode("password"))
                .roles("ROLE_ANIMATOR")
                .imageAuthorization(true) // Proporcionar valor para image_authorization
                .birthDate(java.sql.Date.valueOf(LocalDate.of(1988, 3, 30))) // Proporcionar valor para birth_date
                .build();

        UserSalle participant = UserSalle.builder()
                .name("Participant")
                .lastName("Participant")
                .email("participant@participant.com")
                .password(passwordEncoder.encode("password"))
                .roles("ROLE_PARTICIPANT")
                .imageAuthorization(true) // Proporcionar valor para image_authorization
                .birthDate(java.sql.Date.valueOf(LocalDate.of(1995, 11, 25))) // Proporcionar valor para birth_date
                .build();

        userInfoRepo.saveAll(List.of(admin, pastoralDelegate, groupLeader, animator, participant));*/
    }
}