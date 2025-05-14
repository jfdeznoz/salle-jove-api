package com.sallejoven.backend.service;

import com.opencsv.CSVReader;
import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.repository.CenterRepository;
import com.sallejoven.backend.repository.GroupRepository;
import com.sallejoven.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PastoralDelegateImporterService {

    private final UserRepository userRepository;
    private final CenterRepository centerRepository;
    private final GroupRepository groupRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void importPastoralDelegatesByCenterId(String filePath) throws Exception {
        try (CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            List<String[]> rows = reader.readAll();
            rows.remove(0); // Remove header

            for (String[] row : rows) {
                if (row.length < 5) {
                    System.out.println("⚠️ Fila incompleta: " + Arrays.toString(row));
                    continue;
                }

                String fullName = row[0].trim();
                String telefono = row[1].trim();
                String dni = row[2].trim();
                String email = row[3].trim();
                String centerIdRaw = row[4].trim();

                if (email.isEmpty()) {
                    System.out.println("⚠️ Email vacío, se omite: " + fullName);
                    continue;
                }

                String[] nameParts = fullName.split(" ", 2);
                String nombre = nameParts[0];
                String apellidos = nameParts.length > 1 ? nameParts[1] : "";

                Optional<UserSalle> existing = userRepository.findByEmail(email);

                if (centerIdRaw.isEmpty()) {
                    // Usuario sin centro, solo admin
                    if (existing.isPresent()) {
                        System.out.println("ℹ️ Admin ya existe: " + email);
                        continue;
                    }
                    UserSalle admin = UserSalle.builder()
                            .name(nombre)
                            .lastName(apellidos)
                            .dni(dni)
                            .phone(telefono)
                            .email(email)
                            .roles("ROLE_ADMIN")
                            .password(passwordEncoder.encode("password"))
                            .imageAuthorization(true)
                            .gender(3)
                            .build();
                    userRepository.save(admin);
                    System.out.println("✅ Admin creado: " + fullName);
                    continue;
                }

                Long centerId;
                try {
                    centerId = Long.parseLong(centerIdRaw);
                } catch (NumberFormatException e) {
                    System.out.println("⚠️ Centro ID inválido: " + centerIdRaw + " para " + fullName);
                    continue;
                }

                Optional<Center> centerOpt = centerRepository.findById(centerId);
                if (centerOpt.isEmpty()) {
                    System.out.println("⚠️ Centro no encontrado ID=" + centerId + " para " + fullName);
                    continue;
                }

                Center center = centerOpt.get();
                Set<GroupSalle> groupSet = new HashSet<>(groupRepository.findByCenter(center));

                if (groupSet.isEmpty()) {
                    System.out.println("⚠️ Centro sin grupos: " + center.getName());
                }

                if (existing.isPresent()) {
                    UserSalle existingUser = existing.get();
                    existingUser.getGroups().addAll(groupSet);
                    if (!existingUser.getRoles().contains("ROLE_PASTORAL_DELEGATE")) {
                        existingUser.setRoles(existingUser.getRoles() + ",ROLE_PASTORAL_DELEGATE");
                    }
                    userRepository.save(existingUser);
                    System.out.println("🔁 Usuario actualizado: " + email);
                } else {
                    UserSalle newUser = UserSalle.builder()
                            .name(nombre)
                            .lastName(apellidos)
                            .dni(dni)
                            .phone(telefono)
                            .email(email)
                            .roles("ROLE_PASTORAL_DELEGATE")
                            .password(passwordEncoder.encode("password"))
                            .groups(groupSet)
                            .imageAuthorization(true)
                            .gender(3)
                            .build();
                    userRepository.save(newUser);
                    System.out.println("✅ Usuario creado: " + fullName);
                }
            }
        }
    }
}