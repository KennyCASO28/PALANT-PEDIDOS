package org.example.service.save;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.example.dto.save.ProjectState;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ProjectManager {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private static final byte[] MAGIC_SEPARATOR = "[PALANT_PROJECT_DATA]".getBytes(StandardCharsets.UTF_8);

    /**
     * Saves the project. Si hay un preview PNG válido, inyecta los datos como
     * un chunk especial (paLt) antes del IEND para mantener el PNG 100% válido.
     */
    public static void saveProject(ProjectState state, File file, byte[] pngPreview) throws IOException {
        byte[] jsonBytes = objectMapper.writeValueAsBytes(state);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            if (pngPreview != null && pngPreview.length >= 12) {
                // Verificar si termina en el chunk IEND
                // IEND signature: 00 00 00 00 49 45 4E 44 AE 42 60 82
                byte[] iendSig = new byte[] { 0, 0, 0, 0, 0x49, 0x45, 0x4E, 0x44, (byte) 0xAE, 0x42, 0x60,
                        (byte) 0x82 };
                boolean validIend = true;
                for (int i = 0; i < 12; i++) {
                    if (pngPreview[pngPreview.length - 12 + i] != iendSig[i]) {
                        validIend = false;
                        break;
                    }
                }

                if (validIend) {
                    // Escribir el PNG hasta antes del IEND
                    fos.write(pngPreview, 0, pngPreview.length - 12);

                    // Preparar nuestro chunk "paLt"
                    byte[] chunkType = "paLt".getBytes(StandardCharsets.US_ASCII);

                    java.util.zip.CRC32 crc = new java.util.zip.CRC32();
                    crc.update(chunkType);
                    crc.update(jsonBytes);
                    int crcValue = (int) crc.getValue();

                    // Write Length
                    fos.write(java.nio.ByteBuffer.allocate(4).putInt(jsonBytes.length).array());
                    // Write Type
                    fos.write(chunkType);
                    // Write Data
                    fos.write(jsonBytes);
                    // Write CRC
                    fos.write(java.nio.ByteBuffer.allocate(4).putInt(crcValue).array());

                    // Finalizar con el IEND original
                    fos.write(iendSig);
                    return; // Listo
                }
            }

            // Fallback agresivo (Formato viejo) si falla la inyección
            if (pngPreview != null && pngPreview.length > 0) {
                fos.write(pngPreview);
                fos.write(MAGIC_SEPARATOR);
            }
            fos.write(jsonBytes);
        }
    }

    public static ProjectState loadProject(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] allBytes = fis.readAllBytes();

            // 1. INTENTAR BUSCAR EL CHUNK "paLt" EN FORMATO PNG VALIDO
            if (allBytes.length > 8 && allBytes[0] == (byte) 137 && allBytes[1] == 80 && allBytes[2] == 78
                    && allBytes[3] == 71) {
                int offset = 8;
                while (offset + 8 < allBytes.length) {
                    int length = java.nio.ByteBuffer.wrap(allBytes, offset, 4).getInt();
                    String type = new String(allBytes, offset + 4, 4, StandardCharsets.US_ASCII);

                    if ("paLt".equals(type)) {
                        // CHUNK ENCONTRADO!
                        return objectMapper.readValue(allBytes, offset + 8, length, ProjectState.class);
                    }
                    offset += 12 + length; // skip length, type, data, crc
                }
            }

            // 2. BACKUP: FORMATO VIEJO MAGIC_SEPARATOR
            int jsonStart = 0;
            for (int i = 0; i <= allBytes.length - MAGIC_SEPARATOR.length; i++) {
                boolean match = true;
                for (int j = 0; j < MAGIC_SEPARATOR.length; j++) {
                    if (allBytes[i + j] != MAGIC_SEPARATOR[j]) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    jsonStart = i + MAGIC_SEPARATOR.length;
                    break;
                }
            }

            if (jsonStart > 0) {
                return objectMapper.readValue(allBytes, jsonStart, allBytes.length - jsonStart, ProjectState.class);
            } else {
                return objectMapper.readValue(allBytes, ProjectState.class);
            }
        }
    }
}

