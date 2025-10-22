package com.sallejoven.backend.utils;

import com.sallejoven.backend.model.entity.EventUser;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class ExcelReportUtils {

    /**
     * Crea un workbook con dos hojas: "… Catecumenos" y "… Catequistas",
     * separando por userType de la membresía (EventUser.userGroup.userType).
     *   0  -> PARTICIPANT (catecúmeno)
     *   1,2,3,5 -> catequistas (ANIMATOR, GROUP_LEADER, PASTORAL_DELEGATE, animador-evento)
     */
    public static Workbook buildTwoSheetWorkbook(
            String baseSheetName,
            List<EventUser> participants,
            BiConsumer<Sheet, List<EventUser>> sheetFiller
    ) {
        Workbook wb = new XSSFWorkbook();

        List<EventUser> catecumenos = participants.stream()
                .filter(ExcelReportUtils::isCatecumeno)
                .collect(Collectors.toList());

        List<EventUser> catequistas = participants.stream()
                .filter(eu -> !isCatecumeno(eu))
                .collect(Collectors.toList());

        Sheet s1 = wb.createSheet(baseSheetName + " Catecumenos");
        sheetFiller.accept(s1, catecumenos);

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

    public static boolean isCatecumeno(EventUser eu) {
        if (eu == null || eu.getUserGroup() == null) return true;
        Integer ut = eu.getUserGroup().getUserType();
        if (ut == null) return true;
        // 0 = participante → catecúmeno; 1/2/3/5 = catequistas
        return ut == 0;
    }
}