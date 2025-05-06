package com.sallejoven.backend.service;

import com.sallejoven.backend.model.entity.Event;
import com.sallejoven.backend.model.entity.EventUser;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.enums.Stage;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final EventService eventService;
    private final S3Service s3Service;

    public String generateReports(Long eventId) throws IOException {
        Event event = eventService.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado con ID: " + eventId));

        List<EventUser> participants = eventService.getUsersByEventOrdered(eventId);

        ByteArrayOutputStream seguroReport = generateSeguroReport(event, participants);
        ByteArrayOutputStream camisetasReport = generateCamisetasReport(event, participants);
        ByteArrayOutputStream infoReport = generateInfoReport(event, participants);

        ByteArrayOutputStream zipOutput = new ByteArrayOutputStream();
        try (ZipOutputStream zipStream = new ZipOutputStream(zipOutput)) {

            zipStream.putNextEntry(new ZipEntry("informe_seguro_" + event.getName() + "_" + event.getId() + ".xlsx"));
            zipStream.write(seguroReport.toByteArray());
            zipStream.closeEntry();

            zipStream.putNextEntry(new ZipEntry("informe_tallas_" + event.getName() + "_" + event.getId() + ".xlsx"));
            zipStream.write(camisetasReport.toByteArray());
            zipStream.closeEntry();

            zipStream.putNextEntry(new ZipEntry("informe_" + event.getName() + "_" + event.getId() + ".xlsx"));
            zipStream.write(infoReport.toByteArray());
            zipStream.closeEntry();
        }

        String folderPath = "reports/event_" + event.getId();
        String zipFileName = "informes_evento_" + event.getName() + "_" + event.getId() + ".zip";

        return s3Service.uploadFileReport(zipOutput, folderPath, zipFileName);
    }

    private ByteArrayOutputStream generateSeguroReport(Event event, List<EventUser> participants) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Informe Seguro");
    
        Row header = sheet.createRow(0);
        String[] columns = {"Nombre y apellidos", "Fecha de nacimiento", "DNI", "Colegio", "Grupo"};
        for (int i = 0; i < columns.length; i++) {
            header.createCell(i).setCellValue(columns[i]);
        }
    
        int rowNum = 1;
        for (EventUser eu : participants) {
            UserSalle user = eu.getUser();
            Row row = sheet.createRow(rowNum++);
            Date birthdate = user.getBirthDate();
            row.createCell(0).setCellValue(user.getName() + " " + user.getLastName());
            row.createCell(1).setCellValue(birthdate != null ? birthdate.toString() :"");
            row.createCell(2).setCellValue(user.getDni());
            row.createCell(3).setCellValue(getSchool(user));
            row.createCell(4).setCellValue(getGroup(user));
        }
    
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();
    
        return baos;
    }    

    private ByteArrayOutputStream generateCamisetasReport(Event event, List<EventUser> participants) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Camisetas");
    
        String[] roles = {"CATECÚMENOS", "CATEQUISTAS"};
        String[] sizes = {"XS", "S", "M", "L", "XL", "XXL", "XXXL"};
    
        Row header1 = sheet.createRow(0);
        header1.createCell(0).setCellValue("NOMBRE DEL ENCUENTRO");
        Row header2 = sheet.createRow(1);
        header2.createCell(0).setCellValue("Colegio");
    
        int colIndex = 1;
        for (String role : roles) {
            header1.createCell(colIndex).setCellValue(role);
            for (String size : sizes) {
                header2.createCell(colIndex++).setCellValue(size);
            }
        }
    
        Map<String, int[]> countsBySchool = new HashMap<>();
        for (EventUser eu : participants) {
            String school = getSchool(eu.getUser());
            String role = getRoleForReport(eu.getUser());
            int tshirt = eu.getUser().getTshirtSize(); // Assuming 0 = XS, 1 = S, etc.
    
            int index = "CATEQUISTAS".equals(role) ? 7 + tshirt : tshirt;
            countsBySchool.putIfAbsent(school, new int[14]);
            countsBySchool.get(school)[index]++;
        }
    
        int rowNum = 2;
        for (Map.Entry<String, int[]> entry : countsBySchool.entrySet()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(entry.getKey());
            int[] counts = entry.getValue();
            for (int i = 0; i < counts.length; i++) {
                row.createCell(i + 1).setCellValue(counts[i]);
            }
        }
    
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();
    
        return baos;
    }    

    private ByteArrayOutputStream generateInfoReport(Event event, List<EventUser> participants) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Informe General");
    
        String[] columns = {"Nombre y apellidos", "Colegio", "Grupo", "Teléfono padres", "Intolerancias", "Talla camiseta", "Correo electrónico"};
        Row header = sheet.createRow(0);
        for (int i = 0; i < columns.length; i++) {
            header.createCell(i).setCellValue(columns[i]);
        }
    
        int rowNum = 1;
        for (EventUser eu : participants) {
            UserSalle u = eu.getUser();
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(u.getName() + " " + u.getLastName());
            row.createCell(1).setCellValue(getSchool(u));
            row.createCell(2).setCellValue(getGroup(u));
            row.createCell(3).setCellValue(u.getFatherPhone() + " / " + u.getMotherPhone());
            row.createCell(4).setCellValue(u.getIntolerances() != null ? u.getIntolerances() : "");
            row.createCell(5).setCellValue(getSizeString(u.getTshirtSize()));
            row.createCell(6).setCellValue(u.getEmail());
        }
    
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();
    
        return baos;
    }    

    private void uploadExcel(Workbook workbook, String folderPath, String filename) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();
    
        s3Service.uploadFileReport(baos, folderPath, filename);
    }    

    private String getSchool(UserSalle u) {
        return u.getGroups().stream()
                .map((GroupSalle g) -> g.getCenter().getName())
                .findFirst()
                .orElse("");
    }

    private String getGroup(UserSalle u) {
        return u.getGroups().stream()
                .map(GroupSalle::getStage)
                .map(stage -> {
                    try {
                        return Stage.values()[stage].name().replace("_", " ");
                    } catch (Exception e) {
                        return "Desconocido";
                    }
                })
                .findFirst()
                .orElse("");
    }

    private String getSizeString(int index) {
        return switch (index) {
            case 0 -> "XS";
            case 1 -> "S";
            case 2 -> "M";
            case 3 -> "L";
            case 4 -> "XL";
            case 5 -> "XXL";
            case 6 -> "XXXL";
            default -> "";
        };
    }

    private String getRoleForReport(UserSalle user) {
        return user.getRoles().equalsIgnoreCase("PARTICIPANTE") ? "CATECÚMENOS" : "CATEQUISTAS";
    }
}