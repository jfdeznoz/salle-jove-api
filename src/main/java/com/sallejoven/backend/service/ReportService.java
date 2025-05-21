package com.sallejoven.backend.service;

import com.sallejoven.backend.model.entity.Event;
import com.sallejoven.backend.model.entity.EventUser;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.enums.Role;
import com.sallejoven.backend.model.enums.Stage;
import com.sallejoven.backend.model.enums.TshirtSizeEnum;
import com.sallejoven.backend.model.types.ReportType;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final EventService eventService;
    private final UserService userService;
    private final S3Service s3Service;
    private final EmailService emailService;
    private final AuthService authService;

    public String generateSeguroReportZip() throws IOException {
        List<UserSalle> participants = userService.findAllByRoles();
        ByteArrayOutputStream seguroReport = generateSeguroReport(participants);
        String folderPath = "reports/general";
        String fileName   = "informe_seguro_salle_joven.xlsx";
        return s3Service.uploadFileReport(seguroReport, folderPath, fileName);
    }

    public List<String> generateEventReports(Long eventId, List<ReportType> types, boolean overwrite) throws Exception {
        Event event = eventService.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado ID: " + eventId));
        List<EventUser> participants = eventService.getUsersByEventOrdered(eventId);
        String folder = "reports/event_" + eventId;

        Map<String, byte[]> attachments = new LinkedHashMap<>();
        List<String>      urls        = new ArrayList<>();
        String            email       = authService.getCurrentUserEmail();

        for (ReportType type : types) {
            String filename = String.format("informe_%s_%d.xlsx",
                    type.name().toLowerCase(), eventId);
            String fullPath = folder + "/" + filename;

            // Si existe y no pedimos overwrite, lo descargamos y seguimos
            if (!overwrite && s3Service.exists(fullPath)) {
                String  url       = s3Service.getFileUrl(fullPath);
                byte[]  fileBytes = s3Service.downloadFile(fullPath);
                attachments.put(filename, fileBytes);
                urls.add(url);
                continue;
            }

            // Si no existe (o overwrite=true), lo generamos
            ByteArrayOutputStream baos;
            switch (type) {
                case CAMISETAS:
                    baos = generateCamisetasReport(event, participants);
                    break;
                case INTOLERANCIAS:
                    baos = generateIntoleranciasReport(participants);
                    break;
                case ENFERMEDADES:
                    baos = generateEnfermedadesReport(participants);
                    break;
                case ASISTENCIA:
                    baos = generateAsistenciaReport(participants);
                    break;
                default:
                    throw new IllegalArgumentException("Tipo no soportado: " + type);
            }

            String url = s3Service.uploadFileReport(baos, folder, filename);
            attachments.put(filename, baos.toByteArray());
            urls.add(url);
        }

        // Envío por email
        String subject = "Informes para evento \"" + event.getName() + "\"";
        String body    = "<p>Adjunto encontrarás los informes solicitados para el evento <strong>"
                         + event.getName() + "</strong>.</p>";
        emailService.sendEmailWithAttachments(email, subject, body, attachments);

        return urls;
    }

    private ByteArrayOutputStream generateSeguroReport(List<UserSalle> participants) throws IOException {
        Workbook wb    = new XSSFWorkbook();
        Sheet    sheet = wb.createSheet("Informe Seguro");

        String[] cols = {"Nombre y apellidos", "Fecha de nacimiento", "DNI", "Colegio", "Grupo"};
        Row header = sheet.createRow(0);
        for (int i = 0; i < cols.length; i++) {
            header.createCell(i).setCellValue(cols[i]);
        }

        int rowNum = 1;
        for (UserSalle u : participants) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(u.getName() + " " + u.getLastName());
            row.createCell(1).setCellValue(
                    u.getBirthDate() != null
                            ? u.getBirthDate().toString()
                            : "");
            row.createCell(2).setCellValue(u.getDni());
            row.createCell(3).setCellValue(getSchool(u));
            row.createCell(4).setCellValue(getGroup(u));
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();
        return out;
    }

    private ByteArrayOutputStream generateCamisetasReport(Event event,
                                                         List<EventUser> participants) throws IOException {
        Workbook wb    = new XSSFWorkbook();
        Sheet    sheet = wb.createSheet("Camisetas");
        String[] sizes = {"XS","S","M","L","XL","XXL","XXXL"};

        // --- Filas de título y encabezados ---
        // Fila 0: nombre del encuentro
        Row row0 = sheet.createRow(0);
        Cell c0  = row0.createCell(0);
        c0.setCellValue(event.getName());
        CellStyle center = wb.createCellStyle();
        center.setAlignment(HorizontalAlignment.CENTER);
        c0.setCellStyle(center);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 16));

        // Fila 1: Colegio:
        Row row1 = sheet.createRow(1);
        Cell c1  = row1.createCell(0);
        c1.setCellValue("Colegio:");
        c1.setCellStyle(center);

        // Fila 2: CATECÚMENOS / separador / CATEQUISTAS
        Row row2 = sheet.createRow(2);
        Cell cat = row2.createCell(2);
        cat.setCellValue("CATECÚMENOS");
        cat.setCellStyle(center);
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 2, 8));

        Cell sep = row2.createCell(9);
        sep.getCellStyle().setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
        sep.getCellStyle().setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Cell catq = row2.createCell(10);
        catq.setCellValue("CATEQUISTAS");
        catq.setCellStyle(center);
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 10, 16));

        // Fila 3: tallas
        Row row3 = sheet.createRow(3);
        for (int i = 0; i < sizes.length; i++) {
            Cell cc = row3.createCell(2 + i);
            cc.setCellValue(sizes[i]);
            cc.setCellStyle(center);
            Cell cq = row3.createCell(10 + i);
            cq.setCellValue(sizes[i]);
            cq.setCellStyle(center);
        }

        // --- Conteo por colegio, talla y rol ---
        Map<String, int[][]> bySchool = new LinkedHashMap<>();
        for (EventUser eu : participants) {
            if (eu.getStatus() != 1) continue;
            UserSalle u = eu.getUser();
            String   school = getSchool(u);
            bySchool.putIfAbsent(school, new int[sizes.length][2]);

            TshirtSizeEnum ts = TshirtSizeEnum.fromIndex(u.getTshirtSize());
            if (ts == null) continue;
            int sizeIdx = ts.ordinal();

            List<Role> roles = Arrays.stream(u.getRoles().split(","))
                    .map(String::trim)
                    .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                    .map(Role::valueOf)
                    .collect(Collectors.toList());
            Role top = roles.stream()
                    .min(Comparator.comparingInt(Role::ordinal))
                    .orElse(Role.PARTICIPANT);

            int roleIdx = (top == Role.PARTICIPANT) ? 0 : 1;
            bySchool.get(school)[sizeIdx][roleIdx]++;
        }

        // --- Volcado final ---
        int rowNum = 4;
        for (Map.Entry<String, int[][]> e : bySchool.entrySet()) {
            Row r = sheet.createRow(rowNum++);
            r.createCell(0).setCellValue(e.getKey());
            int[][] cnt = e.getValue();
            for (int i = 0; i < sizes.length; i++) {
                r.createCell(2 + i).setCellValue(cnt[i][0]);
                r.createCell(10 + i).setCellValue(cnt[i][1]);
            }
        }

        for (int c = 0; c <= 16; c++) sheet.autoSizeColumn(c);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();
        return out;
    }

    private ByteArrayOutputStream generateIntoleranciasReport(List<EventUser> participants) throws IOException {
        Workbook wb    = new XSSFWorkbook();
        Sheet    sheet = wb.createSheet("Intolerancias");

        // Header: Nombre, Colegio, Etapa, Intolerancias
        Row h = sheet.createRow(0);
        h.createCell(0).setCellValue("Nombre");
        h.createCell(1).setCellValue("Colegio");
        h.createCell(2).setCellValue("Etapa");
        h.createCell(3).setCellValue("Intolerancias");

        int r = 1;
        for (EventUser eu : participants) {
            if (eu.getStatus() != 1) continue;
            UserSalle u = eu.getUser();
            if (u.getIntolerances() == null || u.getIntolerances().isBlank()) continue;

            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(u.getName() + " " + u.getLastName());
            row.createCell(1).setCellValue(getSchool(u));
            row.createCell(2).setCellValue(getGroup(u));
            row.createCell(3).setCellValue(u.getIntolerances());
        }

        for (int c = 0; c < 4; c++) sheet.autoSizeColumn(c);
        return writeClose(wb);
    }

    private ByteArrayOutputStream generateEnfermedadesReport(List<EventUser> participants) throws IOException {
        Workbook wb    = new XSSFWorkbook();
        Sheet    sheet = wb.createSheet("Enfermedades");

        // Header: Nombre, Colegio, Etapa, Enfermedades
        Row h = sheet.createRow(0);
        h.createCell(0).setCellValue("Nombre");
        h.createCell(1).setCellValue("Colegio");
        h.createCell(2).setCellValue("Etapa");
        h.createCell(3).setCellValue("Enfermedades");

        int r = 1;
        for (EventUser eu : participants) {
            if (eu.getStatus() != 1) continue;
            UserSalle u = eu.getUser();
            if (u.getChronicDiseases() == null || u.getChronicDiseases().isBlank()) continue;

            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(u.getName() + " " + u.getLastName());
            row.createCell(1).setCellValue(getSchool(u));
            row.createCell(2).setCellValue(getGroup(u));
            row.createCell(3).setCellValue(u.getChronicDiseases());
        }

        for (int c = 0; c < 4; c++) sheet.autoSizeColumn(c);
        return writeClose(wb);
    }

    private ByteArrayOutputStream generateAsistenciaReport(List<EventUser> participants) throws IOException {
        Workbook wb    = new XSSFWorkbook();
        Sheet    sheet = wb.createSheet("Asistencia");

        // Header sin columna “Asistió”
        String[] cols = {"Nombre","Correo","Colegio","Etapa","Tel. Padre","Tel. Madre","Intolerancias","Enfermedades"};
        Row h = sheet.createRow(0);
        for (int i = 0; i < cols.length; i++) {
            h.createCell(i).setCellValue(cols[i]);
        }

        int rowNum = 1;
        for (EventUser eu : participants) {
            if (eu.getStatus() != 1) continue;
            UserSalle u = eu.getUser();

            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(u.getName() + " " + u.getLastName());
            row.createCell(1).setCellValue(u.getEmail());
            row.createCell(2).setCellValue(getSchool(u));
            row.createCell(3).setCellValue(getGroup(u));
            row.createCell(4).setCellValue(Optional.ofNullable(u.getFatherPhone()).orElse(""));
            row.createCell(5).setCellValue(Optional.ofNullable(u.getMotherPhone()).orElse(""));
            row.createCell(6).setCellValue(Optional.ofNullable(u.getIntolerances()).orElse(""));
            row.createCell(7).setCellValue(Optional.ofNullable(u.getChronicDiseases()).orElse(""));
        }

        for (int c = 0; c < cols.length; c++) sheet.autoSizeColumn(c);
        return writeClose(wb);
    }

    private ByteArrayOutputStream writeClose(Workbook wb) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();
        return out;
    }

    private String getSchool(UserSalle u) {
        return u.getGroups().stream()
                .map(g -> g.getCenter().getName())
                .findFirst()
                .orElse("");
    }

    private String getGroup(UserSalle u) {
        return u.getGroups().stream()
                .map(g -> {
                    try {
                        return Stage.values()[g.getStage()].name().replace("_", " ");
                    } catch (Exception e) {
                        return "Desconocido";
                    }
                })
                .findFirst()
                .orElse("");
    }
}