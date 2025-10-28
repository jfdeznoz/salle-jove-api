package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.Event;
import com.sallejoven.backend.model.entity.EventUser;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.enums.Role;
import com.sallejoven.backend.model.enums.Stage;
import com.sallejoven.backend.model.enums.TshirtSizeEnum;
import com.sallejoven.backend.model.types.ReportType;
import com.sallejoven.backend.repository.projection.SeguroRow;
import com.sallejoven.backend.utils.ExcelReportUtils;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final EventService eventService;
    private final EventUserService eventUserService;
    private final UserGroupService userGroupService;
    private final S3Service s3Service;
    private final EmailService emailService;
    private final AuthService authService;
    private final AcademicStateService academicStateService;

    @Value("${salle.aws.prefix:}")
    private String s3Prefix; // "" en prod, "test/" en local

    public String generateSeguroReportZip() throws IOException, SalleException, MessagingException {
        var rows = userGroupService.findSeguroRowsForCurrentYear();
        ByteArrayOutputStream seguroReport = generateSeguroReport(rows);

        int currentYear = academicStateService.getVisibleYear();
        String baseFolder = currentYear + "/reports/general";
        String folderPath = withPrefix(baseFolder);
        String fileName   = "informe_seguro_salle_joven.xlsx";
        String fullPath   = folderPath + "/" + fileName;

        String url = s3Service.uploadFileReport(seguroReport, folderPath, fileName);

        Map<String, byte[]> attachments = new LinkedHashMap<>();
        attachments.put(fileName, seguroReport.toByteArray());
        String email   = authService.getCurrentUserEmail();
        String subject = "Informe de seguro (general)";
        String body    = "<p>Adjunto encontrarás el <strong>informe de seguro</strong> general.</p>"
                + "<p>También puedes descargarlo desde: <a href=\"" + url + "\">este enlace</a>.</p>";

        emailService.sendEmailWithAttachments(email, subject, body, attachments);

        return url;
    }

    private String withPrefix(String path) {
        if (s3Prefix == null || s3Prefix.isBlank()) return normalize(path);
        String p = s3Prefix;
        if (!p.endsWith("/")) p = p + "/";
        return normalize(p + path);
    }

    private String normalize(String path) {
        String p = path.replaceAll("/{2,}", "/");
        if (p.startsWith("/")) p = p.substring(1);
        if (p.endsWith("/")) p = p.substring(0, p.length() - 1);
        return p;
    }

    public List<String> generateEventReports(Long eventId, List<ReportType> types, boolean overwrite) throws Exception {
        Event event = eventService.findById(eventId).orElseThrow(() -> new IllegalArgumentException("Evento no encontrado ID: " + eventId));
        
        List<EventUser> participants = eventUserService.findConfirmedByEventIdOrdered(eventId);
        int currentYear = academicStateService.getVisibleYear();
        String folder = withPrefix(currentYear + "/reports/event_" + eventId);

        Map<String, byte[]> attachments = new LinkedHashMap<>();
        List<String>      urls        = new ArrayList<>();
        String            email       = authService.getCurrentUserEmail();

        for (ReportType type : types) {
            String filename = String.format("informe_%s_%d_%s.xlsx",
                                type.name().toLowerCase(),
                                event.getId(),
                                event.getName()
                                    .trim()
                                    .toLowerCase()
                                    .replaceAll("\\s+", "_")
                                    .replaceAll("[^a-z0-9_\\-]", ""));
            String fullPath = folder + "/" + filename;

            if (!overwrite && s3Service.exists(fullPath)) {
                String  url       = s3Service.getFileUrl(fullPath);
                byte[]  fileBytes = s3Service.downloadFile(fullPath);
                attachments.put(filename, fileBytes);
                urls.add(url);
                continue;
            }

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
                case AUTORIZACION_IMAGEN:
                    baos = generateAutorizacionImagenReport(participants);
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

    private ByteArrayOutputStream generateSeguroReport(List<SeguroRow> rows) throws IOException {
        Workbook wb    = new XSSFWorkbook();
        Sheet    sheet = wb.createSheet("Informe Seguro");

        String[] cols = {"Nombre y apellidos", "Fecha de nacimiento", "DNI", "Centro - Grupo"};
        Row header = sheet.createRow(0);
        for (int i = 0; i < cols.length; i++) header.createCell(i).setCellValue(cols[i]);

        int r = 1;
        for (SeguroRow sr : rows) {
            String fullName = ((sr.getName() == null ? "" : sr.getName()) + " " +
                    (sr.getLastName() == null ? "" : sr.getLastName())).trim();
            String birth    = (sr.getBirthDate() != null) ? sr.getBirthDate().toString() : "";
            String dni      = (sr.getDni() == null ? "" : sr.getDni());
            String cg       = (sr.getCentersGroups() == null ? "" : sr.getCentersGroups());

            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(fullName);
            row.createCell(1).setCellValue(birth);
            row.createCell(2).setCellValue(dni);
            row.createCell(3).setCellValue(cg);
        }

        for (int c = 0; c < cols.length; c++) sheet.autoSizeColumn(c);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();
        return out;
    }

    private String getStageName(GroupSalle group) {
        if (group == null || group.getStage() == null) return "";
        return Stage
                .values()[group.getStage()]
                .name()
                .replace("_", " ");
    }

    private ByteArrayOutputStream generateCamisetasReport(Event event,
                                                          List<EventUser> participants) throws IOException {
        Workbook wb    = new XSSFWorkbook();
        Sheet    sheet = wb.createSheet("Camisetas");
        String[] sizes = {"XS","S","M","L","XL","XXL","XXXL"};

        Row row0 = sheet.createRow(0);
        Cell c0  = row0.createCell(0);
        c0.setCellValue(event.getName());
        CellStyle center = wb.createCellStyle();
        center.setAlignment(HorizontalAlignment.CENTER);
        c0.setCellStyle(center);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 16));

        Row row1 = sheet.createRow(1);
        Cell c1  = row1.createCell(0);
        c1.setCellValue("Colegio:");
        c1.setCellStyle(center);

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

        Row row3 = sheet.createRow(3);
        for (int i = 0; i < sizes.length; i++) {
            Cell cc = row3.createCell(2 + i);
            cc.setCellValue(sizes[i]);
            cc.setCellStyle(center);
            Cell cq = row3.createCell(10 + i);
            cq.setCellValue(sizes[i]);
            cq.setCellStyle(center);
        }

        // --- Conteo por colegio, talla y rol (0=alumno, 1=catequista) ---
        Map<String, int[][]> bySchool = new LinkedHashMap<>();
        for (EventUser eu : participants) {
            if (eu.getStatus() != 1) continue;

            UserSalle u   = eu.getUserGroup().getUser();
            GroupSalle g  = eu.getUserGroup().getGroup();
            String school = getSchoolForEventUser(eu);
            bySchool.putIfAbsent(school, new int[sizes.length][2]);

            TshirtSizeEnum ts = TshirtSizeEnum.fromIndex(u.getTshirtSize());
            if (ts == null) continue;
            int sizeIdx = ts.ordinal();

            Integer ut = eu.getUserGroup().getUserType();
            int roleIdx = (ut != null && (ut == 1 || ut == 2 || ut == 3 || ut == 5)) ? 1 : 0;

            bySchool.get(school)[sizeIdx][roleIdx]++;
        }

        int rowNum = 4;
        for (Map.Entry<String, int[][]> e : bySchool.entrySet()) {
            Row r = sheet.createRow(rowNum++);
            r.createCell(0).setCellValue(e.getKey());
            int[][] cnt = e.getValue();
            for (int i = 0; i < sizes.length; i++) {
                r.createCell(2 + i).setCellValue(cnt[i][0]);   // catecúmenos
                r.createCell(10 + i).setCellValue(cnt[i][1]);  // catequistas
            }
        }

        for (int c = 0; c <= 16; c++) sheet.autoSizeColumn(c);
        return ExcelReportUtils.toByteArray(wb);
    }

    private String getSchoolForEventUser(EventUser eu) {
        GroupSalle g = eu.getUserGroup() != null ? eu.getUserGroup().getGroup() : null;
        if (g == null || g.getCenter() == null) return "";
        Center center = g.getCenter();
        return (center.getName() == null ? "" : center.getName())
                + " (" + (center.getCity() == null ? "" : center.getCity()) + ")";
    }

    private ByteArrayOutputStream generateIntoleranciasReport(List<EventUser> participants) throws IOException {
        Workbook wb = ExcelReportUtils.buildTwoSheetWorkbook("Intolerancias", participants, (sheet, list) -> {
            Row h = sheet.createRow(0);
            h.createCell(0).setCellValue("Nombre");
            h.createCell(1).setCellValue("Colegio");
            h.createCell(2).setCellValue("Etapa");
            h.createCell(3).setCellValue("Intolerancias");

            int r = 1;
            for (EventUser eu : list) {
                UserSalle u = eu.getUserGroup().getUser();
                GroupSalle group = eu.getUserGroup().getGroup();
                if (u.getIntolerances() == null || u.getIntolerances().isBlank()) continue;
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(u.getName() + " " + u.getLastName());
                row.createCell(1).setCellValue(getSchool(u));
                row.createCell(2).setCellValue(getStageName(group));
                row.createCell(3).setCellValue(u.getIntolerances());
            }
        });
        for (Sheet s : wb) for (int c = 0; c < 4; c++) s.autoSizeColumn(c);
        return ExcelReportUtils.toByteArray(wb);
    }

    private ByteArrayOutputStream generateEnfermedadesReport(List<EventUser> participants) throws IOException {
        Workbook wb = ExcelReportUtils.buildTwoSheetWorkbook("Enfermedades", participants, (sheet, list) -> {
            Row h = sheet.createRow(0);
            h.createCell(0).setCellValue("Nombre");
            h.createCell(1).setCellValue("Colegio");
            h.createCell(2).setCellValue("Etapa");
            h.createCell(3).setCellValue("Enfermedades");
    
            int r = 1;
            for (EventUser eu : list) {
                UserSalle u = eu.getUserGroup().getUser();
                GroupSalle group = eu.getUserGroup().getGroup();
                if (u.getChronicDiseases() == null || u.getChronicDiseases().isBlank()) continue;
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(u.getName() + " " + u.getLastName());
                row.createCell(1).setCellValue(getSchool(u));
                row.createCell(2).setCellValue(getStageName(group));
                row.createCell(3).setCellValue(u.getChronicDiseases());
            }
        });
        for (Sheet s : wb) for (int c = 0; c < 4; c++) s.autoSizeColumn(c);
        return ExcelReportUtils.toByteArray(wb);
    }

    private ByteArrayOutputStream generateAsistenciaReport(List<EventUser> participants) throws IOException {
        Workbook wb = ExcelReportUtils.buildTwoSheetWorkbook("Asistencia", participants, (sheet, list) -> {
            String[] cols = {
                "Nombre", "Correo", "Colegio", "Etapa",
                "Tel. Padre", "Tel. Madre",
                "Intolerancias", "Enfermedades",
                "Talla Camiseta", "Autorizacion Imagen"
            };
            Row h = sheet.createRow(0);
            for (int i = 0; i < cols.length; i++) {
                h.createCell(i).setCellValue(cols[i]);
            }
    
            int rowNum = 1;
            for (EventUser eu : list) {
                UserSalle u = eu.getUserGroup().getUser();
                GroupSalle group = eu.getUserGroup().getGroup();
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(u.getName() + " " + u.getLastName());
                row.createCell(1).setCellValue(Optional.ofNullable(u.getEmail()).orElse(""));
                row.createCell(2).setCellValue(getSchool(u));
                row.createCell(3).setCellValue(getStageName(group));
                row.createCell(4).setCellValue(Optional.ofNullable(u.getFatherPhone()).orElse(""));
                row.createCell(5).setCellValue(Optional.ofNullable(u.getMotherPhone()).orElse(""));
                row.createCell(6).setCellValue(Optional.ofNullable(u.getIntolerances()).orElse(""));
                row.createCell(7).setCellValue(Optional.ofNullable(u.getChronicDiseases()).orElse(""));
                Cell cellTalla = row.createCell(8);
                if (u.getTshirtSize() != null) {
                    TshirtSizeEnum ts = TshirtSizeEnum.fromIndex(u.getTshirtSize());
                    cellTalla.setCellValue(ts != null ? ts.name() : "");
                } else {
                    cellTalla.setCellValue("");
                }
                row.createCell(9).setCellValue(Boolean.TRUE.equals(u.getImageAuthorization()) ? "Sí" : "No");
            }
        });
        for (Sheet s : wb) for (int c = 0; c < 10; c++) s.autoSizeColumn(c);
        return ExcelReportUtils.toByteArray(wb);
    }
    
    private ByteArrayOutputStream generateAutorizacionImagenReport(List<EventUser> participants) throws IOException {
        Workbook wb = ExcelReportUtils.buildTwoSheetWorkbook("Autorizacion Imagen", participants, (sheet, list) -> {
            String[] cols = {"Nombre", "Colegio", "Etapa", "Autorizacion Imagen"};
            Row h = sheet.createRow(0);
            for (int i = 0; i < cols.length; i++) {
                h.createCell(i).setCellValue(cols[i]);
            }
    
            int r = 1;
            for (EventUser eu : list) {
                UserSalle u = eu.getUserGroup().getUser();
                GroupSalle group = eu.getUserGroup().getGroup();
                if (Boolean.TRUE.equals(u.getImageAuthorization())) {
                    continue;
                }
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(u.getName() + " " + u.getLastName());
                row.createCell(1).setCellValue(getSchool(u));
                row.createCell(2).setCellValue(getStageName(group));
                row.createCell(3).setCellValue("No");
            }
        });
        for (Sheet s : wb) {
            for (int c = 0; c < 4; c++) {
                s.autoSizeColumn(c);
            }
        }
        return ExcelReportUtils.toByteArray(wb);
    }

    private String getSchool(UserSalle u) {
        return u.getGroups().stream()
                .map(g -> {
                    Center center = g.getGroup().getCenter();
                    String centerName = center.getName();
                    String cityName = center.getCity();
                    return centerName + " (" + cityName + ")";
                })
                .findFirst()
                .orElse("");
    }

}