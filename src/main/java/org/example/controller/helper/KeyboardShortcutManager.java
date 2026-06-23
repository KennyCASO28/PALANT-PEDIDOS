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
    private final Runnable saveAction;
    private final Runnable saveAsAction;

    public KeyboardShortcutManager(PrendaVisualizer visualizer) {
        this(visualizer, null, null);
    }

    public KeyboardShortcutManager(PrendaVisualizer visualizer, Runnable saveAction, Runnable saveAsAction) {
        this.visualizer = visualizer;
        this.saveAction = saveAction;
        this.saveAsAction = saveAsAction;
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

        // Ctrl + S: Guardar
        if (saveAction != null) {
            scene.getAccelerators().put(
                KeyCombination.valueOf("Shortcut+S"),
                saveAction
            );
        }

        // Ctrl + Shift + S: Guardar como
        if (saveAsAction != null) {
            scene.getAccelerators().put(
                KeyCombination.valueOf("Shortcut+Shift+S"),
                saveAsAction
            );
        }
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

            // Ctrl + B: Bloquear seleccionados o alternar bloqueo de fondo si no hay selección
            if (event.isControlDown() && !event.isShiftDown() && !event.isAltDown() && event.getCode() == KeyCode.B) {
                if (visualizer.getLayerManager() != null && !visualizer.getLayerManager().getSelectedNodes().isEmpty()) {
                    visualizer.setUserLockedOnSelected(true);
                } else {
                    visualizer.toggleBackgroundLock();
                }
                event.consume();
                return;
            }

            // Ctrl + Shift + B: Desbloquear seleccionados
            if (event.isControlDown() && event.isShiftDown() && !event.isAltDown() && event.getCode() == KeyCode.B) {
                visualizer.setUserLockedOnSelected(false);
                event.consume();
                return;
            }

            // Ctrl + S fallback (cuando no hay foco en un TextInputControl)
            if (event.isControlDown() && !event.isShiftDown() && event.getCode() == KeyCode.S) {
                if (saveAction != null && !isTyping) {
                    saveAction.run();
                    event.consume();
                    return;
                }
            }

            // Ctrl + Shift + S fallback (cuando no hay foco en un TextInputControl)
            if (event.isControlDown() && event.isShiftDown() && event.getCode() == KeyCode.S) {
                if (saveAsAction != null && !isTyping) {
                    saveAsAction.run();
                    event.consume();
                    return;
                }
            }

            // Si está escribiendo, no interceptar teclas de letras simples
            if (isTyping) {
                return;
            }
        });
    }
}
