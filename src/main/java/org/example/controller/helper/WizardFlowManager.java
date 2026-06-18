package org.example.controller.helper;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import org.example.component.PrendaVisualizer;
import org.example.utils.UIFactory;

/**
 * Gestor del flujo del asistente (Wizard) y la navegación entre pestañas.
 * Maneja la habilitación de pasos, el estilo del botón principal y el movimiento
 * del visualizador entre los contenedores de cada pestaña.
 */
public class WizardFlowManager {

    private final TabPane mainTabPane;
    private final Button btnFinalizar;
    private final PrendaVisualizer prendaVisualizer;
    
    // Contenedores de pestañas
    private final StackPane stackVectorContainer;      // Pestaña 1 (Diseño)
    private final StackPane stackVectorContainerLogos; // Pestaña 2 (Personalización)
    
    // Elementos flotantes que se mueven con el visualizador
    private final Button btnResetGlobal;
    private final Button btnCenterView;
    private final Label lblPlaceholder;

    public WizardFlowManager(TabPane tabPane, Button btnMain, PrendaVisualizer visualizer,
                             StackPane designContainer, StackPane logosContainer,
                             Button btnReset, Button btnCenter, Label placeholder) {
        this.mainTabPane = tabPane;
        this.btnFinalizar = btnMain;
        this.prendaVisualizer = visualizer;
        this.stackVectorContainer = designContainer;
        this.stackVectorContainerLogos = logosContainer;
        this.btnResetGlobal = btnReset;
        this.btnCenterView = btnCenter;
        this.lblPlaceholder = placeholder;
    }

    /**
     * Actualiza el texto y estilo del botón principal según la pestaña activa.
     */
    public void actualizarBotonAccion() {
        if (mainTabPane == null || btnFinalizar == null) return;
        
        int index = mainTabPane.getSelectionModel().getSelectedIndex();
        int totalTabs = mainTabPane.getTabs().size();

        if (index < totalTabs - 1) {
            btnFinalizar.setText("CONTINUAR");
            btnFinalizar.getStyleClass().setAll("button", "button-continue");
            btnFinalizar.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        } else {
            btnFinalizar.setText("CONFIRMAR PEDIDO");
            btnFinalizar.getStyleClass().setAll("button", "button-confirm");
            btnFinalizar.setStyle(""); // Usa estilos de CSS
        }
    }

    /**
     * Gestiona el avance del wizard al hacer clic en el botón principal.
     * @return true si se avanzó de pestaña, false si se llegó al final (confirmación).
     */
    public boolean handleMainAction() {
        int index = mainTabPane.getSelectionModel().getSelectedIndex();
        int totalTabs = mainTabPane.getTabs().size();

        if (index < totalTabs - 1) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Continuar");
            alert.setHeaderText(null);
            alert.setContentText("¿Está seguro de que desea continuar al siguiente paso?");
            UIFactory.estilizarDialogo(alert);

            java.util.Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // Desbloquear siguiente pestaña
                mainTabPane.getTabs().get(index + 1).setDisable(false);
                mainTabPane.getSelectionModel().select(index + 1);
                return true;
            }
        }
        return false;
    }

    /**
     * Mueve el visualizador y sus controles flotantes al contenedor de la pestaña activa.
     * Esto evita crear múltiples instancias pesadas de PrendaVisualizer.
     */
    public void moverVisualizador(int tabIndex, org.example.controller.delegate.PersonalizacionDelegate personalizacionDelegate) {
        if (prendaVisualizer == null) return;

        // 1. Desacoplar de cualquier padre previo
        detachFromParent(prendaVisualizer);
        detachFromParent(btnResetGlobal);
        detachFromParent(btnCenterView);

        // 2. Configurar según pestaña
        if (tabIndex == 1) { // Pestaña Logotipos
            prendaVisualizer.setRulersVisible(true);
            stackVectorContainerLogos.getChildren().clear();

            StackPane rootOverlay = new StackPane();
            rootOverlay.setMinSize(0, 0);
            rootOverlay.setPickOnBounds(false);
            rootOverlay.getChildren().add(prendaVisualizer);

            stackVectorContainerLogos.getChildren().setAll(rootOverlay);
            prendaVisualizer.setEditModeContainer(stackVectorContainerLogos);

            // Re-insalar botones flotantes
            if (btnResetGlobal != null) {
                StackPane.setAlignment(btnResetGlobal, Pos.TOP_RIGHT);
                StackPane.setMargin(btnResetGlobal, new Insets(15, 15, 0, 0));
                stackVectorContainerLogos.getChildren().add(btnResetGlobal);
            }

            ToggleButton btnRef = prendaVisualizer.getBtnToggleRefPoints();
            if (btnRef != null) {
                detachFromParent(btnRef);
                StackPane.setAlignment(btnRef, Pos.BOTTOM_LEFT);
                StackPane.setMargin(btnRef, new Insets(20));
                rootOverlay.getChildren().add(btnRef);
            }

            if (btnCenterView != null) {
                StackPane.setAlignment(btnCenterView, Pos.BOTTOM_RIGHT);
                StackPane.setMargin(btnCenterView, new Insets(20));
                rootOverlay.getChildren().add(btnCenterView);
            }

            // Integrar barras de herramientas del delegado
            if (personalizacionDelegate != null) {
                javafx.scene.Node toolsCapsule = personalizacionDelegate.getIntegratedBottomBar();
                if (toolsCapsule != null) {
                    detachFromParent(toolsCapsule);
                    StackPane.setAlignment(toolsCapsule, Pos.BOTTOM_CENTER);
                    StackPane.setMargin(toolsCapsule, new Insets(0, 0, 20, 0));
                    rootOverlay.getChildren().add(toolsCapsule);
                }
                
                javafx.scene.Node verticalBar = personalizacionDelegate.getVerticalZoneBar();
                if (verticalBar != null) {
                    detachFromParent(verticalBar);
                    StackPane.setAlignment(verticalBar, Pos.CENTER_LEFT);
                    StackPane.setMargin(verticalBar, new Insets(0, 0, 0, 30));
                    rootOverlay.getChildren().add(verticalBar);
                }
            }

        } else if (tabIndex == 0) { // Pestaña Diseño
            prendaVisualizer.setRulersVisible(true);
            stackVectorContainer.getChildren().add(prendaVisualizer);
            prendaVisualizer.setEditModeContainer(stackVectorContainer);

            if (btnCenterView != null) {
                StackPane.setAlignment(btnCenterView, Pos.BOTTOM_RIGHT);
                StackPane.setMargin(btnCenterView, new Insets(20));
                stackVectorContainer.getChildren().add(btnCenterView);
            }

            if (btnResetGlobal != null) {
                StackPane.setAlignment(btnResetGlobal, Pos.TOP_RIGHT);
                StackPane.setMargin(btnResetGlobal, new Insets(15, 15, 0, 0));
                stackVectorContainer.getChildren().add(btnResetGlobal);
            }

            ToggleButton btnRef0 = prendaVisualizer.getBtnToggleRefPoints();
            if (btnRef0 != null) {
                detachFromParent(btnRef0);
                StackPane.setAlignment(btnRef0, Pos.BOTTOM_LEFT);
                StackPane.setMargin(btnRef0, new Insets(20));
                stackVectorContainer.getChildren().add(btnRef0);
            }
            if (lblPlaceholder != null) lblPlaceholder.setVisible(false);
        }
        prendaVisualizer.setVisible(true);
    }

    private void detachFromParent(javafx.scene.Node node) {
        if (node != null && node.getParent() instanceof Pane) {
            ((Pane) node.getParent()).getChildren().remove(node);
        }
    }
}
