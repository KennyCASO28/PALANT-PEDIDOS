package org.example.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Datos para envío/atención del cliente que deben aparecer en la ficha técnica.
 * POJO simple para serialización en ProjectState y para exportación.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DatosEnvioDTO {
    private String nombres;
    private String apellidos;
    private String dni;
    private String celular;
    private String lugarEnvio;
    private String vendedorAtiende;

    // Backward/forward compatibility: older saves may include {"empty": ...}
    // Accept it on load but never serialize it back.
    @JsonProperty(value = "empty", access = JsonProperty.Access.WRITE_ONLY)
    private Boolean empty;

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getCelular() {
        return celular;
    }

    public void setCelular(String celular) {
        this.celular = celular;
    }

    public String getLugarEnvio() {
        return lugarEnvio;
    }

    public void setLugarEnvio(String lugarEnvio) {
        this.lugarEnvio = lugarEnvio;
    }

    public String getVendedorAtiende() {
        return vendedorAtiende;
    }

    public void setVendedorAtiende(String vendedorAtiende) {
        this.vendedorAtiende = vendedorAtiende;
    }

    @JsonIgnore
    public boolean isEmpty() {
        return isBlank(nombres) && isBlank(apellidos) && isBlank(dni) && isBlank(lugarEnvio) && isBlank(vendedorAtiende)
                && isBlank(celular);
    }

    public boolean isComplete() {
        return !isBlank(nombres) && !isBlank(apellidos) && !isBlank(dni) && !isBlank(lugarEnvio)
                && !isBlank(vendedorAtiende);
    }

    public String getNombreCompleto() {
        String n = safeTrim(nombres);
        String a = safeTrim(apellidos);
        String full = (n + " " + a).trim();
        return full.isEmpty() ? null : full;
    }

    public String getResumenCorto() {
        String nombre = getNombreCompleto();
        String d = safeTrim(dni);
        if (nombre == null && d.isEmpty())
            return null;
        if (nombre == null)
            return "DNI " + d;
        if (d.isEmpty())
            return nombre;
        return nombre + " - DNI " + d;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }
}

