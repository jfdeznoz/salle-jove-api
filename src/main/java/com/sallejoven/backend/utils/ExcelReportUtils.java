package com.sallejoven.backend.utils;

import com.sallejoven.backend.model.entity.EventUser;
import com.sallejoven.backend.model.entity.UserSalle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class ExcelReportUtils {

    public static Workbook buildTwoSheetWorkbook(
            String baseSheetName,
            List<EventUser> participants,
            BiConsumer<Sheet, List<EventUser>> sheetFiller
    ) {
        Workbook wb = new XSSFWorkbook();
        // Filtrar participantes por rol
        List<EventUser> catecumenos = participants.stream()
            .filter(eu -> isCatecumeno(eu.getUserGroup().getUser()))
            .collect(Collectors.toList());
        List<EventUser> catequistas = participants.stream()
            .filter(eu -> !isCatecumeno(eu.getUserGroup().getUser()))
            .collect(Collectors.toList());

        // Crear y rellenar hoja de catecúmenos
        Sheet s1 = wb.createSheet(baseSheetName + " Catecumenos");
        sheetFiller.accept(s1, catecumenos);

        // Crear y rellenar hoja de catequistas
        Sheet s2 = wb.createSheet(baseSheetName + " Catequistas");
        sheetFiller.accept(s2, catequistas);

        return wb;
    }


    public static ByteArrayOutputStream toByteArray(Workbook wb) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();
        return out;
    }

    public static boolean isCatecumeno(UserSalle u) {
        return Arrays.stream(u.getRoles().split(","))
                     .map(String::trim)
                     .anyMatch(r -> r.equals("ROLE_PARTICIPANT"));
    }
}