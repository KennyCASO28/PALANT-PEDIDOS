package org.example.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class GeneradorCodigo {

    public static String generarCodigo(int numeroSecuencia) {
        LocalDate hoy = LocalDate.now();

        // 1. Obtener Año (26)
        String anio = hoy.format(DateTimeFormatter.ofPattern("yy"));

        // 2. Obtener Letra del Mes (E, F, M...)
        String letraMes = obtenerLetraMes(hoy.getMonthValue());

        // 3. Obtener Día (01, 15, 30...)
        String dia = hoy.format(DateTimeFormatter.ofPattern("dd"));

        // 4. Armar el código base: P26-E01
        String codigoBase = "P" + anio + "-" + letraMes + dia;

        // 5. Agregar la secuencia si es el pedido 2, 3, etc.
        if (numeroSecuencia > 1) {
            return codigoBase + "-" + (numeroSecuencia);
        } else {
            return codigoBase; // El primero del día queda limpio
        }
    }

    private static String obtenerLetraMes(int mes) {
        String[] letras = { "", "E", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D" };
        return letras[mes]; // Enero=1 -> letras[1]="E"
    }
}

