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
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GroupLeaderImporterService {
/*
    private final UserRepository userRepository;
    private final CenterRepository centerRepository;
    private final GroupRepository groupRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void importGroupLeaders(String filePath) throws Exception {
        try (CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            List<String[]> rows = reader.readAll();
            rows.remove(0); // remove header

            for (String[] row : rows) {
                if (row.length < 7) continue;

                String colegioRaw = row[0];
                String nombre = row[1];
                String apellidos = row[2];
                String fechaNacimientoStr = row[3];
                String dni = row[4];
                String telefono = row[5];
                String email = row[6];

                if (email == null || email.trim().isEmpty()) {
                    System.out.println("⚠️ Email vacío, se omite: " + nombre + " " + apellidos);
                    continue;
                }

                // Normalizar nombre del colegio
                String[] parts = colegioRaw.split("-");
                String centerNameRaw = parts[0].trim();
                String extractedCity = (parts.length > 1) ? parts[1].trim() : null;

                String centerClean = Normalizer.normalize(centerNameRaw, Normalizer.Form.NFC)
                        .replaceAll("[\\p{Cf}]", "")
                        .trim();

                if (centerClean.equalsIgnoreCase("Córdoba")) {
                    centerClean = "Córdoba";
                    extractedCity = "Córdoba";
                }

                // 👉 Ponemos la coletilla "Salle Joven"
                String centerFullName = "Salle Joven " + centerClean;

                final String finalCenterCity = extractedCity;

                // Buscar o crear centro teniendo en cuenta nombre y ciudad
                Center center = centerRepository.findByNameAndCity(centerFullName, finalCenterCity)
                        .orElseGet(() -> {
                            Center newCenter = Center.builder()
                                    .name(centerFullName)
                                    .city(finalCenterCity)
                                    .build();
                            centerRepository.save(newCenter);
                            System.out.println("✅ Centro creado: " + centerFullName + " (" + finalCenterCity + ")");
                            return newCenter;
                        });

                // Obtener todos los grupos del centro
                Set<GroupSalle> groupsOfCenter = new HashSet<>(groupRepository.findByCenter(center));
                if (groupsOfCenter.isEmpty()) {
                    System.out.println("⚠️ Centro sin grupos: " + centerFullName);
                }

                Optional<UserSalle> existing = userRepository.findByEmail(email.trim());
                if (existing.isPresent()) {
                    UserSalle user = existing.get();

                    // Añadir grupos del centro que no tenga
                    if (user.getGroups() == null) user.setGroups(new HashSet<>());
                    boolean newGroupsAdded = user.getGroups().addAll(groupsOfCenter);

                    // Añadir rol si no lo tiene
                    String roles = user.getRoles();
                    if (roles == null || !roles.contains("ROLE_GROUP_LEADER")) {
                        roles = (roles == null ? "" : roles + ",") + "ROLE_GROUP_LEADER";
                        user.setRoles(roles);
                    }

                    userRepository.save(user);
                    System.out.println("🔁 Usuario actualizado: " + nombre + " " + apellidos + (newGroupsAdded ? " (grupos añadidos)" : ""));

                } else {
                    Date fechaNacimiento = parseFlexibleDate(fechaNacimientoStr);

                    UserSalle user = UserSalle.builder()
                            .name(nombre)
                            .lastName(apellidos)
                            .dni(dni)
                            .phone(telefono)
                            .email(email.trim())
                            .birthDate(fechaNacimiento)
                            .password(passwordEncoder.encode("password"))
                            .roles("ROLE_GROUP_LEADER")
                            .groups(groupsOfCenter)
                            .build();

                    userRepository.save(user);
                    System.out.println("✅ Coordinador creado: " + nombre + " " + apellidos);
                }
            }
        }
    }

    private Date parseFlexibleDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        String[] formats = {"dd/MM/yyyy", "d/M/yyyy", "dd-MM-yyyy", "yyyy-MM-dd"};
        for (String format : formats) {
            try {
                return new SimpleDateFormat(format).parse(dateStr);
            } catch (Exception ignored) {}
        }
        System.out.println("⚠️ Fecha inválida: " + dateStr);
        return null;
    }*/
}