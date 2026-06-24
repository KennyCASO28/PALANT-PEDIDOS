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

            // Leemos la cabecera para identificar columnas dinámicamente
            int colNombre = 0;
            int colTalla = 1;
            int colNumero = 2;
            int colGenero = 3;

            if (rowIterator.hasNext()) {
                Row headerRow = rowIterator.next();
                for (int i = 0; i < Math.max(4, headerRow.getLastCellNum()); i++) {
                    String headerText = getCellValue(headerRow.getCell(i)).toLowerCase().trim();
                    if (headerText.contains("nombre") || headerText.contains("jugador")) {
                        colNombre = i;
                    } else if (headerText.contains("talla")) {
                        colTalla = i;
                    } else if (headerText.contains("numero") || headerText.contains("número") || headerText.contains("nº") || headerText.contains("num")) {
                        colNumero = i;
                    } else if (headerText.contains("genero") || headerText.contains("género") || headerText.contains("sexo")) {
                        colGenero = i;
                    }
                }
            }

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();

                // Usamos los índices detectados de la cabecera
                String nombre = getCellValue(row.getCell(colNombre)).trim();
                String talla = getCellValue(row.getCell(colTalla)).trim();
                String numero = getCellValue(row.getCell(colNumero)).trim();
                String generoRaw = getCellValue(row.getCell(colGenero)).trim();

                // Normalizar género a HOMBRE o MUJER
                String genero = "HOMBRE";
                if (!generoRaw.isEmpty()) {
                    String gClean = generoRaw.toUpperCase();
                    if (gClean.equals("MUJER") || gClean.equals("FEMENINO") || gClean.equals("FEM") || gClean.equals("F") || gClean.contains("FEM")) {
                        genero = "MUJER";
                    } else if (gClean.equals("HOMBRE") || gClean.equals("MASCULINO") || gClean.equals("MAS") || gClean.equals("M") || gClean.equals("H") || gClean.contains("MAS")) {
                        genero = "HOMBRE";
                    }
                }

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

