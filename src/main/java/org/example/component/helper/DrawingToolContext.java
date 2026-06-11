package org.example.component.helper;

import javafx.scene.paint.Color;

/**
 * Interfaz para desacoplar las capas gráficas (ImageLayer)
 * de los controladores de UI masivos (ShapeManagerController).
 * Define únicamente el contrato del estado de las herramientas de dibujo.
 */
public interface DrawingToolContext {
    boolean isEraserActive();

    double getEraserSize();

    boolean isBrushActive();

    double getBrushSize();

    Color getFillColor();
}
