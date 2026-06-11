package org.example.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.model.DetallePedido;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExcelImporter {

    public static List<DetallePedido> importarJugadores(File file) throws IOException {
        List<DetallePedido> lista = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(file);
                Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            // Saltamos la cabecera si existe
            if (rowIterator.hasNext()) {
                rowIterator.next();
            }

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();

                // Mapping based on USER EXCEL: 0=Nombre, 1=Talla, 2=Número, 3=Género
                String nombre = getCellValue(row.getCell(0));
                String talla = getCellValue(row.getCell(1));
                String numero = getCellValue(row.getCell(2));
                String genero = getCellValue(row.getCell(3));

                if (!nombre.isEmpty()) {
                    DetallePedido p = new DetallePedido(nombre, numero, talla, genero);
                    lista.add(p);
                }
            }
        }
        return lista;
    }

    private static String getCellValue(Cell cell) {
        if (cell == null)
            return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    // Convertimos 10.0 a "10"
                    double val = cell.getNumericCellValue();
                    if (val == (long) val) {
                        return String.format("%d", (long) val);
                    } else {
                        return String.valueOf(val);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }
}

