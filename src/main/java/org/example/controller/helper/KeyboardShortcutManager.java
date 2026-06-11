package org.example.controller.helper;

import javafx.scene.Scene;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.example.component.PrendaVisualizer;
import org.example.component.ui.ColorSearchReplaceDialog;

/**
 * Gestor centralizado de atajos de teclado y aceleradores para la aplicación.
 * Desacopla la configuración de shortcuts del controlador principal.
 */
public class KeyboardShortcutManager {

    private final PrendaVisualizer visualizer;

    public KeyboardShortcutManager(PrendaVisualizer visualizer) {
        this.visualizer = visualizer;
    }

    /**
     * Configura los aceleradores globales (Ctrl+F, etc.) en la escena.
     */
    public void setupAccelerators(Scene scene) {
        if (scene == null || visualizer == null) return;

        // Ctrl + F: Buscar y Reemplazar Colores
        scene.getAccelerators().put(
            KeyCombination.valueOf("Shortcut+F"),
            () -> {
                if (visualizer.isVisible()) {
                    ColorSearchReplaceDialog dialog = new ColorSearchReplaceDialog(
                        (Stage) scene.getWindow(), visualizer);
                    dialog.show();
                }
            }
        );
    }

    /**
     * Instala un filtro de eventos para capturar shortcuts de bajo nivel (teclas simples o Alt).
     */
    public void installEventFilter(Scene scene) {
        if (scene == null || visualizer == null) return;

        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            javafx.scene.Node focusOwner = scene.getFocusOwner();
            boolean isTyping = focusOwner instanceof TextInputControl;

            // Ctrl + P: Reset View (Funciona incluso si está escribiendo)
            if (event.isControlDown() && event.getCode() == KeyCode.P) {
                visualizer.resetView();
                event.consume();
                return;
            }

            // Alt + B: Bloquear seleccionados
            if (event.isAltDown() && !event.isControlDown() && event.getCode() == KeyCode.B) {
                visualizer.setUserLockedOnSelected(true);
                event.consume();
                return;
            }

            // Alt + D: Desbloquear seleccionados
            if (event.isAltDown() && !event.isControlDown() && event.getCode() == KeyCode.D) {
                visualizer.setUserLockedOnSelected(false);
                event.consume();
                return;
            }

            // Si está escribiendo, no interceptar teclas de letras simples
            if (isTyping) {
                return;
            }
        });
    }
}
