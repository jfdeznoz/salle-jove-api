package com.sallejoven.backend.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
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
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserImporterService {

    private final UserRepository userSalleRepository;
    private final GroupRepository groupSalleRepository;
    private final CenterRepository centerRepository;
    private final PasswordEncoder passwordEncoder;
    

    @Transactional
    public void importUsersFromCsv(String filePath) throws IOException, CsvException {
        try (CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            List<String[]> rows = reader.readAll();
            rows.remove(0); // remove header

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

            for (String[] row : rows) {
                String colegio = row[0];     // Ejemplo: "Mirandilla - Cádiz"
                String etapa = row[1];       // Stage: 0, 1, etc.
                String catecumeno = row[2];
                String catequista = row[3];
                String name = row[4];
                String lastName = row[5];
                String birthDateStr = row[6];
                String dni = row[7];
                String address = row[8];
                String city = row[9];
                String phone = row[10];
                String email = row[11];
                String tshirtSizeStr = row[12];
                String healthCardNumber = row[13];
                String intolerances = row[14];
                String chronicDiseases = row[15];
                // Skip index 16 ("¿En qué colegio estudia? ¿Qué curso?")
                String motherFullName = row[17];
                String motherPhone = row[18];
                String motherEmail = row[19];
                String fatherFullName = row[20];
                String fatherPhone = row[21];
                String fatherEmail = row[22];
                String imageAuthorizationStr = row[23];

                String fullNameCsvNorm = Normalizer.normalize((name + lastName), Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}\\s-]", "")
                .toLowerCase();

                if (email == null || email.trim().isEmpty() || email.trim().equals(".") || email.contains("-")) {
                    // Normalizamos nombre+apellidos a ascii y sin espacios
                    String localPart = Normalizer.normalize(name + lastName, Normalizer.Form.NFD)
                        .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                        .replaceAll("[^A-Za-z0-9]", "")
                        .toLowerCase();
                    email = localPart + "@salle.com";
                    System.out.println("🔄 Email generado para " + name + " " + lastName + ": " + email);
                }

                // Determine role
                String role;
                if ("TRUE".equalsIgnoreCase(catecumeno)) {
                    role = "ROLE_PARTICIPANT";
                } else if ("TRUE".equalsIgnoreCase(catequista)) {
                    role = "ROLE_ANIMATOR";
                } else {
                    continue; // Skip rows with no role
                }

                // Parse birth date
                Date birthDate = null;
                try {
                    birthDate = parseFlexibleDate(birthDateStr);
                } catch (Exception e) {
                    System.out.println("Invalid date for: " + name + " " + lastName);
                }

                // Parse tshirt size (default to 0 if empty)
                // Parse tshirt size (según las tallas S, M, L, etc.)
                int tshirtSize = mapTshirtSize(tshirtSizeStr);

                // Parse image authorization
                boolean imageAuthorization = false;
                if (imageAuthorizationStr != null && !imageAuthorizationStr.trim().isEmpty()) {
                    imageAuthorization = "TRUE".equalsIgnoreCase(imageAuthorizationStr.trim());
                } else {
                    System.out.println("⚠️ Image authorization vacío para: " + name + " " + lastName);
                }

                // Gender is not present -> default to 3 (not declared)
                int gender = 3;

                // Get Center by name (before '-')
                String[] colegioParts = colegio.split("-");
                String centerNameRaw = colegioParts[0].trim();
                String centerCityTemp = colegioParts.length > 1 ? colegioParts[1].trim() : null;

                // ✅ Normalizamos para quitar acentos y caracteres invisibles
                String centerNameClean = Normalizer.normalize(centerNameRaw, Normalizer.Form.NFC)
                        .replaceAll("[\\p{Cf}]", "")
                        .trim();

                // 📢 Caso especial: si el colegio es solo "Córdoba"
                if (centerNameClean.equalsIgnoreCase("Córdoba")) {
                    centerNameClean = "Córdoba";
                    centerCityTemp = "Córdoba";
                }

                // 👉 Ponemos la coletilla "Salle Joven"
                String centerFullName = "Salle Joven " + centerNameClean;

                // ✅ Hacemos una copia final de la ciudad
                final String finalCenterCity = centerCityTemp;

                // 🔥 BUSCAR o CREAR el centro teniendo en cuenta nombre Y ciudad
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

                // Parse stage (etapa)
                Integer parsedStage = mapStageFromString(etapa);
                if (parsedStage == null) {
                    System.out.println("⚠️ Etapa no reconocida para: " + colegio + " -> " + etapa);
                    continue;
                }

                // 🔥 BUSCAR o CREAR el grupo
                GroupSalle group = groupSalleRepository.findByCenterAndStage(center, parsedStage)
                        .orElseGet(() -> {
                            GroupSalle newGroup = GroupSalle.builder()
                                    .center(center)
                                    .stage(parsedStage)
                                    .build();
                            groupSalleRepository.save(newGroup);
                            System.out.println("✅ Grupo creado: " + centerFullName + " - Stage: " + parsedStage);
                            return newGroup;
                        });

                Optional<UserSalle> existingUserOpt = userSalleRepository.findByEmail(email);
                UserSalle user = null;
        
                if (existingUserOpt.isPresent()) {
                    UserSalle existing = existingUserOpt.get();
        
                    // Normalizamos nombre del DB para comparar
                    String dbNameNorm = Normalizer.normalize((existing.getName() + existing.getLastName()), Normalizer.Form.NFD)
                        .replaceAll("[\\p{InCombiningDiacriticalMarks}\\s-]", "")
                        .toLowerCase();
        
                    if (dbNameNorm.equals(fullNameCsvNorm)) {
                        // 3a) Mismo usuario: lo reutilizamos
                        user = existing;
                        System.out.println("ℹ️ Usuario existente emparejado por email+nombre: " + name + " " + lastName);
                    } else {
                        // 3b) Email duplicado pero distinto nombre -> generamos variante
                        String localPart = Normalizer.normalize(name + lastName, Normalizer.Form.NFD)
                            .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                            .replaceAll("[^A-Za-z0-9]", "")
                            .toLowerCase();
                        String randomSuffix = UUID.randomUUID().toString().substring(0, 2);
                        email = localPart + "_" + randomSuffix + "@salle.com";
                        System.out.println("⚠️ Email colisión para " + name + " " + lastName
                            + "; usando variante: " + email);
                        existingUserOpt = Optional.empty();  // forzamos creación
                    }
                }
        
                if (existingUserOpt.isEmpty()) {
                    // 4) Creamos nuevo
                    user = UserSalle.builder()
                            .name(name)
                            .lastName(lastName)
                            .dni(dni)
                            .phone(phone)
                            .email(email)
                            .tshirtSize(tshirtSize)
                            .healthCardNumber(healthCardNumber)
                            .intolerances(intolerances)
                            .chronicDiseases(chronicDiseases)
                            .city(city)
                            .imageAuthorization(imageAuthorization)
                            .birthDate(birthDate)
                            .address(address)
                            .roles(role)
                            .password(passwordEncoder.encode("password"))
                            .gender(gender)
                            .motherFullName(motherFullName)
                            .fatherFullName(fatherFullName)
                            .motherEmail(motherEmail)
                            .fatherEmail(fatherEmail)
                            .motherPhone(motherPhone)
                            .fatherPhone(fatherPhone)
                            .build();
                    userSalleRepository.save(user);
                    System.out.println("✅ Usuario creado: " + name + " " + lastName);
                }
        
                // 5) Añadimos grupo si faltaba
                Set<GroupSalle> groups = Optional.ofNullable(user.getGroups()).orElse(new HashSet<>());
                if (groups.add(group)) {
                    user.setGroups(groups);
                    userSalleRepository.save(user);
                    System.out.println("✅ Grupo añadido a: " + name + " " + lastName);
                } else {
                    System.out.println("ℹ️ Ya tenía este grupo: " + name + " " + lastName);
                }
            }
        }
    }

    private Integer mapStageFromString(String groupName) {
        if (groupName == null) return null;
        // ✅ Normalizamos y quitamos tildes/acentos invisibles
        String normalized = Normalizer.normalize(groupName.trim().toUpperCase(), Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
    
        switch (normalized) {
            case "NAZARET 1":
                return 0;
            case "NAZARET 2":
                return 1;
            case "GENESARET 1":
                return 2;
            case "GENESARET 2":
                return 3;
            case "CAFARNAUM 1":  // ✅ Se aceptará "Cafarnaúm" o "Cafarnaum"
                return 4;
            case "CAFARNAUM 2":
                return 5;
            case "BETANIA 1":
                return 6;
            case "BETANIA 2":
                return 7;
            case "JERUSALEN":  // ✅ Se aceptará "Jerusalén" o "Jerusalen"
                return 8;
            default:
                return null;
        }
    }    

    private int mapTshirtSize(String size) {
        if (size == null) return -1;
        String normalized = size.trim().toUpperCase();
        switch (normalized) {
            case "XS":
                return 0;
            case "S":
                return 1;
            case "M":
                return 2;
            case "L":
                return 3;
            case "XL":
                return 4;
            case "XXL":
                return 5;
            case "XXXL":
                return 6;
            default:
                return -1;  // o 0 si prefieres default a "XS"
        }
    }    

    private Date parseFlexibleDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        String[] formats = {"dd/MM/yyyy", "dd-MM-yyyy"};
        for (String format : formats) {
            try {
                return new SimpleDateFormat(format).parse(dateStr);
            } catch (Exception ignored) {}
        }
        System.out.println("⚠️ Fecha inválida: " + dateStr);
        return null;
    }

    public void importGroupLeaders(String filePath) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'importGroupLeaders'");
    }
    
    
}