# Guía de Creación de Vectores para PALANT

Para que el sistema de capas funcione perfectamente (que las mangas coincidan con el cuerpo, y el cuello quede en su sitio), debes seguir esta **REGLA DE ORO**:

## ⚠️ La Regla del Lienzo (Artboard)

Todas las partes (Cuerpo, Mangas, Cuellos) deben exportarse usando el **MISMO TAMAÑO DE MESA DE TRABAJO (Lienzo/Artboard)**.

**NO recortes** el SVG al tamaño de la pieza.

### Ejemplo Correcto:
Imagina un lienzo de **500x600 px**.
1.  **Cuerpo:** Dibujas el cuerpo centrado. Exportas el SVG de toda la mesa de trabajo (500x600).
2.  **Manga:** Dibujas la manga **en la posición exacta** donde iría pegada al cuerpo. Borras el cuerpo (o lo ocultas) y exportas el SVG de **toda la mesa de trabajo (500x600)**.

De esta forma, cuando el programa apile "Manga" sobre "Cuerpo", encajarán perfecto porque ambos comparten el mismo sistema de coordenadas (0,0 a 500,600).

---

## Dimensiones Recomendadas

Te sugiero un tamaño estándar para todos tus archivos:

*   **Ancho:** 500 px
*   **Alto:** 600 px (o 700 px si son prendas largas)
*   **Unidad:** Píxeles (px)

Si usas Adobe Illustrator:
1.  Crea un archivo nuevo de 500x600.
2.  Dibuja tu prenda completa (Boceto base).
3.  Separa en capas (Cuerpo, Manga Izq, Manga Der, Cuello).
4.  Exporta cada capa manteniendo el tamaño del Artboard (Desmarcar "Ajustar a contenido").

## Formato de Exportación
*   **Formato:** SVG 1.1.
*   **Copiar Código:** Simplemente abre el SVG con el Bloc de Notas y copia el texto que está dentro del atributo `d="..."` de la etiqueta `<path>`. O guarda el archivo directamente en las carpetas que creamos.
