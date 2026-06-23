package org.example.controller.configurator;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.example.component.PrendaVisualizer;
import org.example.dto.ConfiguracionPrendaDTO;
import org.example.model.TipoCorte;
import org.example.model.TipoGenero;
import org.example.model.TipoPrenda;
import org.example.utils.UIFactory;

/**
 * Configurator for shorts garments.
 * Restored visual styles and persistence logic.
 */
public class ShortConfigurator {

    private final VBox container;
    private final PrendaVisualizer visualizer;
    private ConfiguracionPrendaDTO.Builder configBuilder; // Non-final for injection

    // Keep reference for enabling/disabling
    private VBox boxOpcionesShort;
    private CheckBox cbFranja, cbLinea, cbPiquete, cbBolsillo, cbPuno, cbPasador, cbForro;
    private Label lblOpciones;
    private Runnable onConfigChanged;

    public void setOnConfigChanged(Runnable listener) {
        this.onConfigChanged = listener;
    }

    private void notifyChange() {
        if (onConfigChanged != null) {
            onConfigChanged.run();
        }
    }

    public ShortConfigurator(VBox container, PrendaVisualizer visualizer) {
        this.container = container;
        this.visualizer = visualizer;
        this.configBuilder = new ConfiguracionPrendaDTO.Builder();
    }

    /**
     * Builds the complete UI for short configuration.
     */
    public void buildUI(TipoGenero genero, ConfiguracionPrendaDTO previousConfig) {
        configBuilder.tipoPrenda(TipoPrenda.SHORT);
        configBuilder.genero(genero);

        visualizer.getState().setHasShirt(false);
        visualizer.getState().setHasShorts(true);
        visualizer.getState().setHasSocks(false);

        visualizer.setGenero(genero);
        visualizer.updateActiveLayers("short");

        if (previousConfig != null && previousConfig.getTipoPrenda() == TipoPrenda.SHORT) {
            configBuilder.corteShort(previousConfig.getCorteShort());
            configBuilder.conFranjaShort(previousConfig.llevaFranjaShort());
            configBuilder.conLineaShort(previousConfig.llevaLineaShort());
            configBuilder.conPiqueteShort(previousConfig.llevaPiqueteShort());
            configBuilder.conBolsilloShort(previousConfig.llevaBolsilloShort());
            configBuilder.conPunoShort(previousConfig.llevaPunoShort());
            configBuilder.conPasadorShort(previousConfig.llevaPasadorShort());
            configBuilder.conForroShort(previousConfig.llevaForroShort());

            visualizer.setShortsCorte(previousConfig.getCorteShort());
            visualizer.setShortsStripe(previousConfig.llevaFranjaShort());
            visualizer.setShortsLinea(previousConfig.llevaLineaShort());
            visualizer.setShortsPicket(previousConfig.llevaPiqueteShort());
            visualizer.setShortsPocket(previousConfig.llevaBolsilloShort());
            visualizer.setShortsCuff(previousConfig.llevaPunoShort());
            visualizer.setShortsCord(previousConfig.llevaPasadorShort());
            visualizer.setShortsLining(previousConfig.llevaForroShort());
        } else {
            // Defaults
            configBuilder.corteShort(TipoCorte.CUADRADO);
            configBuilder.conPasadorShort(true); // Default: Pasador ON
            visualizer.setShortsCorte(TipoCorte.CUADRADO);
            visualizer.setShortsCord(true);
        }

        // Validate Compatibility
        TipoCorte corteActual = configBuilder.build().getCorteShort();
        boolean mutable = false;

        // Inline validation since Service might not have it yet or I prefer direct
        // control
        if (genero == TipoGenero.HOMBRE) {
            if (corteActual == TipoCorte.LICRA || corteActual == TipoCorte.PANTALONETA) {
                configBuilder.corteShort(TipoCorte.CUADRADO);
                visualizer.setShortsCorte(TipoCorte.CUADRADO);
                mutable = true;
            }
        } else if (genero == TipoGenero.MUJER) {
            if (corteActual == TipoCorte.NOVA) {
                configBuilder.corteShort(TipoCorte.CUADRADO);
                visualizer.setShortsCorte(TipoCorte.CUADRADO);
                mutable = true;
            }
        }

        if (mutable) {
            // Optional: Notify or just fix
        }

        visualizer.setVisible(true);

        agregarSeccionTipoShort(genero, configBuilder.build().getCorteShort());
        agregarSeccionOpcionesShort(configBuilder.build());

        // Validation on load
        updateShortOptionsAvailability(configBuilder.build().getCorteShort());
    }

    /**
     * Builds ONLY the configuration controls (Type + Options) without resetting the
     * visualizer. Intended for embedding.
     */
    public void buildUIInline(TipoGenero genero, ConfiguracionPrendaDTO.Builder parentBuilder,
            ConfiguracionPrendaDTO previousConfig) {
        this.configBuilder = parentBuilder; // Inject Parent Builder
        configBuilder.genero(genero);

        // Defaults
        TipoCorte corteToLoad = TipoCorte.CUADRADO;

        if (previousConfig != null && previousConfig.llevaShort()) {
            // Restore Details
            corteToLoad = previousConfig.getCorteShort();

            // Check compatibility again
            if (genero == TipoGenero.HOMBRE
                    && (corteToLoad == TipoCorte.LICRA || corteToLoad == TipoCorte.PANTALONETA)) {
                corteToLoad = TipoCorte.CUADRADO; // Reset incompatible
                // Should we clear details? Assuming yes for safety
                configBuilder.conFranjaShort(false);
                configBuilder.conPiqueteShort(false);
                configBuilder.conBolsilloShort(false);
                configBuilder.conPunoShort(false);
                configBuilder.conPasadorShort(false);
            } else if (genero == TipoGenero.MUJER && corteToLoad == TipoCorte.NOVA) {
                corteToLoad = TipoCorte.CUADRADO; // Reset incompatible
                // Should we clear details? Assuming yes for safety
                configBuilder.conFranjaShort(false);
                configBuilder.conPiqueteShort(false);
                configBuilder.conBolsilloShort(false);
                configBuilder.conPunoShort(false);
                configBuilder.conPasadorShort(false);
            } else {
                // Restore options
                configBuilder.conFranjaShort(previousConfig.llevaFranjaShort());
                configBuilder.conLineaShort(previousConfig.llevaLineaShort());
                configBuilder.conPiqueteShort(previousConfig.llevaPiqueteShort());
                configBuilder.conBolsilloShort(previousConfig.llevaBolsilloShort());
                configBuilder.conPunoShort(previousConfig.llevaPunoShort());
                configBuilder.conPasadorShort(previousConfig.llevaPasadorShort());
                configBuilder.conForroShort(previousConfig.llevaForroShort());

                visualizer.setShortsStripe(previousConfig.llevaFranjaShort());
                visualizer.setShortsLinea(previousConfig.llevaLineaShort());
                visualizer.setShortsPicket(previousConfig.llevaPiqueteShort());
                visualizer.setShortsPocket(previousConfig.llevaBolsilloShort());
                visualizer.setShortsCuff(previousConfig.llevaPunoShort());
                visualizer.setShortsCord(previousConfig.llevaPasadorShort());
                visualizer.setShortsLining(previousConfig.llevaForroShort());
            }
        } else {
            // Default for new Short Add-on
            configBuilder.conPasadorShort(true);
            visualizer.setShortsCord(true);
        }

        configBuilder.corteShort(corteToLoad);
        visualizer.setShortsCorte(corteToLoad);

        agregarSeccionTipoShort(genero, corteToLoad);
        agregarSeccionOpcionesShort(configBuilder.build());
        updateShortOptionsAvailability(corteToLoad);
    }

    private void agregarSeccionTipoShort(TipoGenero genero, TipoCorte currentCorte) {
        javafx.scene.layout.GridPane boxTipo = new javafx.scene.layout.GridPane();
        boxTipo.setHgap(10); // Reduced gap to fit 3 buttons nicely
        boxTipo.setVgap(15);
        boxTipo.setAlignment(Pos.CENTER);
        boxTipo.setMaxWidth(Double.MAX_VALUE);
        
        if (genero == TipoGenero.MUJER) {
            // Three columns for Women: Deportivo | Licra | Pantaloneta in a single row
            javafx.scene.layout.ColumnConstraints c1 = new javafx.scene.layout.ColumnConstraints();
            c1.setPercentWidth(33.33);
            c1.setFillWidth(true);
            c1.setHgrow(javafx.scene.layout.Priority.ALWAYS);
            javafx.scene.layout.ColumnConstraints c2 = new javafx.scene.layout.ColumnConstraints();
            c2.setPercentWidth(33.33);
            c2.setFillWidth(true);
            c2.setHgrow(javafx.scene.layout.Priority.ALWAYS);
            javafx.scene.layout.ColumnConstraints c3 = new javafx.scene.layout.ColumnConstraints();
            c3.setPercentWidth(33.33);
            c3.setFillWidth(true);
            c3.setHgrow(javafx.scene.layout.Priority.ALWAYS);
            boxTipo.getColumnConstraints().addAll(c1, c2, c3);

            ToggleGroup grpTipo = new ToggleGroup();
            agregarBotonTipoPNG(boxTipo, grpTipo, "Deportivo", "/custom_icons/short.png", TipoCorte.CUADRADO, currentCorte, 0, 0);
            agregarBotonTipoPNG(boxTipo, grpTipo, "Licra", "/custom_icons/licra.png", TipoCorte.LICRA, currentCorte, 1, 0);
            agregarBotonTipoPNG(boxTipo, grpTipo, "Pantaloneta", "/custom_icons/pantaloneta.png", TipoCorte.PANTALONETA, currentCorte, 2, 0);

            grpTipo.selectedToggleProperty().addListener((o, v, n) -> handleShortTypeSelection(v, n));
        } else {
            // Two columns for Men: Deportivo | NOVA
            javafx.scene.layout.ColumnConstraints c1 = new javafx.scene.layout.ColumnConstraints();
            c1.setPercentWidth(50);
            c1.setFillWidth(true);
            c1.setHgrow(javafx.scene.layout.Priority.ALWAYS);
            javafx.scene.layout.ColumnConstraints c2 = new javafx.scene.layout.ColumnConstraints();
            c2.setPercentWidth(50);
            c2.setFillWidth(true);
            c2.setHgrow(javafx.scene.layout.Priority.ALWAYS);
            boxTipo.getColumnConstraints().addAll(c1, c2);

            ToggleGroup grpTipo = new ToggleGroup();
            agregarBotonTipoPNG(boxTipo, grpTipo, "Deportivo", "/custom_icons/short.png", TipoCorte.CUADRADO, currentCorte, 0, 0);
            agregarBotonTipoPNG(boxTipo, grpTipo, "NOVA", "/custom_icons/nova.png", TipoCorte.NOVA, currentCorte, 1, 0);

            grpTipo.selectedToggleProperty().addListener((o, v, n) -> handleShortTypeSelection(v, n));
        }

        container.getChildren().add(UIFactory.crearSeccionTarjeta("Tipo de Short", null, boxTipo));
    }

    private void handleShortTypeSelection(Toggle oldVal, Toggle newVal) {
        if (newVal == null) {
            if (oldVal != null) {
                oldVal.setSelected(true);
            }
            return;
        }

        TipoCorte corte = (TipoCorte) newVal.getUserData();
        configBuilder.corteShort(corte);
        visualizer.setShortsCorte(corte);
        updateShortOptionsAvailability(corte);
        notifyChange();

        if (corte != TipoCorte.LICRA && corte != TipoCorte.PANTALONETA) {
            if (cbPasador != null) {
                cbPasador.setSelected(true);
                configBuilder.conPasadorShort(true);
                visualizer.setShortsCord(true);
                // notifyChange(); // Unnecessary as already called above
            }
        }
    }

    private ToggleButton agregarBotonTipoPNG(javafx.scene.layout.GridPane grid, ToggleGroup group, String text, String imagePath, TipoCorte corte,
            TipoCorte selected, int col, int row) {
        javafx.scene.image.ImageView iv = null;
        try {
            javafx.scene.image.Image img = new javafx.scene.image.Image(getClass().getResourceAsStream(imagePath));
            if (!img.isError()) {
                iv = new javafx.scene.image.ImageView(img);
                iv.setFitWidth(28);
                iv.setFitHeight(28);
                iv.setPreserveRatio(true);
            }
        } catch (Exception e) {
            System.err.println("Error loading icon: " + imagePath);
        }

        ToggleButton btn = (iv != null) ? UIFactory.crearBotonOpcion(text, iv)
                : UIFactory.crearBotonOpcion(text, "mdi2v-view-column", 20);
        if (iv != null) {
            iv.opacityProperty().bind(
                javafx.beans.binding.Bindings.when(btn.selectedProperty())
                    .then(1.0)
                    .otherwise(0.45)
            );
        }
        btn.setContentDisplay(javafx.scene.control.ContentDisplay.TOP);
        btn.setAlignment(javafx.geometry.Pos.CENTER);
        UIFactory.applyGenderTheme(btn, configBuilder.build().getGenero());
        btn.setUserData(corte);
        btn.setToggleGroup(group);
        btn.setWrapText(true); 

        btn.setMinHeight(82);
        btn.setPrefHeight(86);
        btn.setGraphicTextGap(4);
        btn.setMaxWidth(Double.MAX_VALUE);
        javafx.scene.layout.GridPane.setHgrow(btn, javafx.scene.layout.Priority.ALWAYS);

        btn.getProperties().put("customFontSize", "11px");
        btn.getProperties().put("customPadding", "6 2");
        UIFactory.updateButtonStyle(btn); // Apply immediately

        if (corte == selected)
            btn.setSelected(true);
        grid.add(btn, col, row);
        return btn;
    }

    private void agregarSeccionOpcionesShort(ConfiguracionPrendaDTO config) {
        // Store ref to box for disabling/enabling
        boxOpcionesShort = new VBox(0);

        // Create Toggles
        HBox hbFranja = createOptionToggle("Franjas Laterales", config.llevaFranjaShort(), val -> {
            configBuilder.conFranjaShort(val);
            visualizer.setShortsStripe(val);
            notifyChange();
            if (val) {
                if (cbLinea != null && cbLinea.isSelected()) {
                    cbLinea.setSelected(false);
                    configBuilder.conLineaShort(false);
                    visualizer.setShortsLinea(false);
                }
                if (cbPiquete != null && cbPiquete.isSelected()) {
                    cbPiquete.setSelected(false);
                    configBuilder.conPiqueteShort(false);
                    visualizer.setShortsPicket(false);
                }
            }
        });
        cbFranja = (CheckBox) hbFranja.getChildren().get(0);

        HBox hbLinea = createOptionToggle("Líneas Decorativas", config.llevaLineaShort(), val -> {
            configBuilder.conLineaShort(val);
            visualizer.setShortsLinea(val);
            notifyChange();
            if (val) {
                if (cbFranja != null && cbFranja.isSelected()) {
                    cbFranja.setSelected(false);
                    configBuilder.conFranjaShort(false);
                    visualizer.setShortsStripe(false);
                }
            }
        });
        cbLinea = (CheckBox) hbLinea.getChildren().get(0);

        HBox hbPiquete = createOptionToggle("Piquetes", config.llevaPiqueteShort(), val -> {
            configBuilder.conPiqueteShort(val);
            visualizer.setShortsPicket(val);
            notifyChange();
            if (val) {
                if (cbFranja != null && cbFranja.isSelected()) {
                    cbFranja.setSelected(false);
                    configBuilder.conFranjaShort(false);
                    visualizer.setShortsStripe(false);
                }
                if (cbPuno != null && cbPuno.isSelected()) {
                    cbPuno.setSelected(false);
                    configBuilder.conPunoShort(false);
                    visualizer.setShortsCuff(false);
                }
            }
        });
        cbPiquete = (CheckBox) hbPiquete.getChildren().get(0);

        HBox hbPuno = createOptionToggle("Puños", config.llevaPunoShort(), val -> {
            configBuilder.conPunoShort(val);
            visualizer.setShortsCuff(val);
            notifyChange();
            if (val) {
                if (cbPiquete != null && cbPiquete.isSelected()) {
                    cbPiquete.setSelected(false);
                    configBuilder.conPiqueteShort(false);
                    visualizer.setShortsPicket(false);
                }
            }
        });
        cbPuno = (CheckBox) hbPuno.getChildren().get(0);

        HBox hbBolsillo = createOptionToggle("Bolsillos", config.llevaBolsilloShort(), val -> {
            configBuilder.conBolsilloShort(val);
            visualizer.setShortsPocket(val);
            notifyChange();
        });
        cbBolsillo = (CheckBox) hbBolsillo.getChildren().get(0);

        HBox hbPasador = createOptionToggle("Pasador (Cordón)", config.llevaPasadorShort(), val -> {
            configBuilder.conPasadorShort(val);
            visualizer.setShortsCord(val);
            notifyChange();
        });
        cbPasador = (CheckBox) hbPasador.getChildren().get(0);

        HBox hbForro = createOptionToggle("Forro Interno", config.llevaForroShort(), val -> {
            configBuilder.conForroShort(val);
            visualizer.setShortsLining(val);
            notifyChange();
        });
        cbForro = (CheckBox) hbForro.getChildren().get(0);

        // Lay out in a premium 1-column VBox to prevent narrow text wrapping
        VBox column = new VBox(8);
        column.setMaxWidth(Double.MAX_VALUE);

        column.getChildren().addAll(hbFranja, hbLinea, hbPiquete, hbPuno, hbBolsillo, hbPasador, hbForro);

        boxOpcionesShort.getChildren().add(column);

        // Wrap in Card
        container.getChildren().add(UIFactory.crearSeccionTarjeta("Opciones", null, boxOpcionesShort));
    }

    private HBox createOptionToggle(String text, boolean selected, java.util.function.Consumer<Boolean> action) {
        return UIFactory.crearFilaOpcionCompacta(text, selected, configBuilder.build().getGenero(), action);
    }

    private void updateShortOptionsAvailability(TipoCorte corte) {
        if (lblOpciones != null) {
            String label = "Opciones del Short:";
            if (corte == TipoCorte.LICRA)
                label = "Opciones de Licra:";
            else if (corte == TipoCorte.PANTALONETA)
                label = "Opciones de Pantaloneta:";
            else if (corte == TipoCorte.CUADRADO)
                label = "Opciones del Short:";
            lblOpciones.setText(label);
        }

        final CheckBox fFranja = cbFranja;
        final CheckBox fPiquete = cbPiquete;
        final CheckBox fPuno = cbPuno;
        final CheckBox fBolsillo = cbBolsillo;
        final CheckBox fPasador = cbPasador;
        final CheckBox fForro = cbForro;

        boolean isPantaloneta = (corte == TipoCorte.PANTALONETA);
        boolean isLicra = (corte == TipoCorte.LICRA);

        if (fFranja != null)
            fFranja.setDisable(false);

        if (fPiquete != null) {
            if (isPantaloneta) {
                fPiquete.setSelected(false);
                configBuilder.conPiqueteShort(false);
                visualizer.setShortsPicket(false);
                fPiquete.setDisable(true);
            } else {
                fPiquete.setDisable(false);
            }
        }

        if (isPantaloneta || isLicra) {
            disableAndUncheck(fPuno);
            disableAndUncheck(fBolsillo);
            disableAndUncheck(fPasador);
            disableAndUncheck(fForro);
            disableAndUncheck(cbLinea);

            // Sync dto
            configBuilder.conPunoShort(false);
            configBuilder.conBolsilloShort(false);
            configBuilder.conPasadorShort(false);
            configBuilder.conForroShort(false);
            configBuilder.conLineaShort(false);

            visualizer.setShortsCuff(false);
            visualizer.setShortsPocket(false);
            visualizer.setShortsCord(false);
            visualizer.setShortsLining(false);
            visualizer.setShortsLinea(false);
        } else {
            enable(cbPuno);
            enable(cbBolsillo);
            enable(cbPasador);
            enable(cbForro);
            enable(cbLinea);
        }
    }

    private void disableAndUncheck(CheckBox cb) {
        if (cb == null)
            return;
        cb.setSelected(false);
        cb.setDisable(true);
    }

    private void enable(CheckBox cb) {
        if (cb == null)
            return;
        cb.setDisable(false);
    }

    public ConfiguracionPrendaDTO.Builder getConfigBuilder() {
        return configBuilder;
    }
}
