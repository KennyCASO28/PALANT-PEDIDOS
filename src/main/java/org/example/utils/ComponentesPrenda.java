package org.example.utils;

public class ComponentesPrenda {

    // --- CUERPOS BASE (BODIES) ---
    // Cuerpo Estándar (Base Recta)
    public static final String BODY_BASE = "M6,4 L18,4 L18,22 L6,22 Z";
    // Cuerpo Entallado (Mujer - Cintura)
    public static final String BODY_WOMAN = "M6,4 L18,4 L17,10 L18,22 L6,22 L7,10 Z";

    // --- MANGAS (SLEEVES) ---
    // Manga Corta Clásica (Izquierda y Derecha simétricas en path o separadas)
    // Para simplificar, definimos paths que se pegan al cuerpo.
    // Asumimos coordenadas relativas al canvas 24x24 o similar.

    // Manga Izquierda (Corta)
    public static final String SLEEVE_L_SHORT = "M6,4 L2,8 L4,10 L8,6 Z";
    // Manga Derecha (Corta)
    public static final String SLEEVE_R_SHORT = "M18,4 L22,8 L20,10 L16,6 Z";

    // Manga Larga
    public static final String SLEEVE_L_LONG = "M6,4 L2,8 L2,20 L5,20 L8,6 Z";
    public static final String SLEEVE_R_LONG = "M18,4 L22,8 L22,20 L19,20 L16,6 Z";

    // Manga Cuadrada (Recta)
    public static final String SLEEVE_L_SQUARE = "M6,4 L1,4 L1,9 L6,9 Z";
    public static final String SLEEVE_R_SQUARE = "M18,4 L23,4 L23,9 L18,9 Z";

    // --- CUELLOS (COLLARS) ---
    // Cuello Redondo
    public static final String COLLAR_ROUND = "M9,4 C9,6 15,6 15,4 L15,5 C15,7 9,7 9,5 Z";

    // Cuello V
    public static final String COLLAR_V = "M9,4 L12,7 L15,4 L15,5 L12,8 L9,5 Z";

    // Cuello Camisero (Polo)
    public static final String COLLAR_POLO = "M9,4 L7,3 L7,6 L12,8 L17,6 L17,3 L15,4 L12,6 Z";

    // --- EXTRAS ---
    public static final String POCKET_CHEST = "M14,10 L17,10 L17,13 L15.5,14 L14,13 Z";

    // Malla (Costado)
    public static final String MESH_SIDE = "M6,10 L7,10 L7,22 L6,22 Z M18,10 L17,10 L17,22 L18,22 Z"; // Costados
                                                                                                      // simples

    public static final String CUFFS_SHORT = "M2,8 L4,10 L5,9 L3,7 Z M22,8 L20,10 L19,9 L21,7 Z"; // Placeholder para
                                                                                                  // borde de manga

}

