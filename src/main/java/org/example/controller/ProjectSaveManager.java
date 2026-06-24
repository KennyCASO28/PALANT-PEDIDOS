package org.example.controller;

import java.io.File;
import java.util.List;
import javafx.scene.image.WritableImage;
import javafx.scene.SnapshotParameters;
import javafx.scene.paint.Color;
import org.example.utils.UIFactory;
import org.example.model.DetallePedido;

public class ProjectSaveManager {

    private final PedidoController controller;

    public ProjectSaveManager(PedidoController controller) {
        this.controller = controller;
    }

    public boolean guardarProyecto() {
        if (controller.currentProjectFile != null) {
            return ejecutarGuardadoInterno(controller.currentProjectFile);
        } else {
            return guardarProyectoComo();
        }
    }

    public boolean guardarProyectoComo() {
        if (controller.prendaVisualizer == null)
            return false;

        javafx.stage.Window window = null;
        if (controller.mainTabPane != null && controller.mainTabPane.getScene() != null) {
            window = controller.mainTabPane.getScene().getWindow();
        }

        File file = UIFactory.seleccionarArchivoGuardar(
                window,
                "Guardar Proyecto como...",
                (controller.currentProjectFile != null) ? controller.currentProjectFile.getName() : "Proyecto_" + controller.lblCodigoPedido.getText() + ".tlp",
                "*.tlp"
        );

        if (file != null) {
            return ejecutarGuardadoInterno(file);
        }
        return false;
    }

    private boolean ejecutarGuardadoInterno(File file) {
        try {
            // 1. Capture Snapshot for Thumbnail
            byte[] previewBytes = null;
            try {
                SnapshotParameters params = new SnapshotParameters();
                params.setFill(Color.WHITE);

                // 1a. Hide ALL UI Overlays for a clean snapshot
                boolean wasOverlayVisible = controller.prendaVisualizer.getOverlayManager().getOverlayGroup().isVisible();
                boolean wasLockVisible = controller.prendaVisualizer.getBtnLockBg() != null && controller.prendaVisualizer.getBtnLockBg().isVisible();
                boolean wasFinishVisible = controller.prendaVisualizer.getUiController() != null && controller.prendaVisualizer.getUiController().isEditOverlayVisible();

                controller.prendaVisualizer.getOverlayManager().getOverlayGroup().setVisible(false);
                if (controller.prendaVisualizer.getBtnLockBg() != null) controller.prendaVisualizer.getBtnLockBg().setVisible(false);
                if (controller.prendaVisualizer.getUiController() != null) controller.prendaVisualizer.getUiController().setEditOverlayVisible(false);

                // Snapshot the visualizer directly
                WritableImage image = controller.prendaVisualizer.snapshot(params, null);

                // 1b. Restore UI visibility
                controller.prendaVisualizer.getOverlayManager().getOverlayGroup().setVisible(wasOverlayVisible);
                if (controller.prendaVisualizer.getBtnLockBg() != null) controller.prendaVisualizer.getBtnLockBg().setVisible(wasLockVisible);
                if (controller.prendaVisualizer.getUiController() != null) controller.prendaVisualizer.getUiController().setEditOverlayVisible(wasFinishVisible);

                if (image != null && image.getWidth() > 1 && image.getHeight() > 1) {
                    java.awt.image.BufferedImage bimg = javafx.embed.swing.SwingFXUtils.fromFXImage(image, null);
                    if (bimg != null) {
                        java.io.ByteArrayOutputStream bout = new java.io.ByteArrayOutputStream();
                        boolean success = javax.imageio.ImageIO.write(bimg, "png", bout);
                        if (success) {
                            previewBytes = bout.toByteArray();
                        }
                    }
                }
            } catch (Exception ex) {
                System.err.println("[Thumbnail] Exception during capture: " + ex.getMessage());
            }

            if (previewBytes == null) {
                previewBytes = new byte[] {
                        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x48,
                        0x44, 0x52, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x08, 0x02, 0x00, 0x00, 0x00,
                        (byte) 0x90, 0x77, 0x53, (byte) 0xDE, 0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41, 0x54, 0x08,
                        (byte) 0xD7, 0x63, (byte) 0xF8, (byte) 0xFF, (byte) 0xFF, 0x3F, 0x00, 0x05, (byte) 0xFE, 0x02,
                        (byte) 0xFE, (byte) 0xDC, 0x44, 0x74, (byte) 0x8E, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E,
                        0x44, (byte) 0xAE, 0x42, 0x60, (byte) 0x82
                };
            }

            // 2. Extract State
            org.example.dto.save.ProjectState state = controller.extraerEstadoParaGuardado(file);
            if (controller.personalizacionDelegate != null) {
                state.setLogoLibrary(org.example.service.save.StateMapper.encodeImageLibrary(controller.personalizacionDelegate.getLogoLibrary()));
                state.setShieldLibraryShirt(org.example.service.save.StateMapper.encodeImageLibrary(controller.personalizacionDelegate.getShieldLibraryShirt()));
                state.setShieldLibraryShort(org.example.service.save.StateMapper.encodeImageLibrary(controller.personalizacionDelegate.getShieldLibraryShort()));
                state.setShieldLibrarySleeve(org.example.service.save.StateMapper.encodeImageLibrary(controller.personalizacionDelegate.getShieldLibrarySleeve()));
                state.setShieldLibrary(org.example.service.save.StateMapper.encodeImageLibrary(controller.personalizacionDelegate.getShieldLibrary()));
                state.setReferenceLibrary(org.example.service.save.StateMapper.encodeImageLibrary(controller.personalizacionDelegate.getReferenceLibrary()));
            }

            // 3. Save File
            boolean success;
            try {
                org.example.service.save.ProjectManager.saveProject(state, file, previewBytes);
                success = true;
            } catch (Exception e) {
                success = false;
                e.printStackTrace();
            }
            if (success) {
                controller.currentProjectFile = file;
                controller.actualizarArchivoActualUI();
                controller.projectDirty = false;
                controller.actualizarTituloVentana();
                UIFactory.mostrarAlerta(javafx.scene.control.Alert.AlertType.INFORMATION, "Éxito", "Proyecto guardado correctamente.");
                return true;
            } else {
                UIFactory.mostrarAlerta(javafx.scene.control.Alert.AlertType.ERROR, "Error", "No se pudo guardar el archivo.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            UIFactory.mostrarAlerta(javafx.scene.control.Alert.AlertType.ERROR, "Error", "Error procesando el guardado: " + e.getMessage());
        }
        return false;
    }

    public void cargarProyecto() {
        if (controller.prendaVisualizer == null) return;

        File file = UIFactory.seleccionarArchivoAbrir(
                controller.mainTabPane.getScene().getWindow(),
                "Abrir Proyecto",
                "*.tlp;*.plt"
        );

        if (file != null) {
            cargarProyectoDesdeArchivo(file);
        }
    }

    public boolean cargarProyectoDesdeArchivo(File file) {
        if (file == null || controller.prendaVisualizer == null) return false;

        if (!controller.wizardStarted) {
            controller.iniciarWizard();
        }

        controller.isRestoringDesign = true;
        if (controller.prendaVisualizer.getHistoryManager() != null) {
            controller.prendaVisualizer.getHistoryManager().setRecording(false);
        }
        try {
            org.example.dto.save.ProjectState state = org.example.service.save.ProjectManager.loadProject(file);

            if (state != null) {
                // Restore goalkeeper slots first to avoid overwrites during visualizer loading
                controller.goalkeeperCoordinator.restoreGoalkeeperSlotsFromProject(state);

                org.example.service.save.StateMapper.restoreState(controller.prendaVisualizer, state);

                if (controller.prendaDelegate != null && state.getGarmentConfig() != null) {
                    controller.prendaDelegate.restoreFromState(state.getGarmentConfig());
                    controller.updateActiveBulkSocksCategory();
                }

                controller.editandoDisenoArquero = false;
                controller.disenoCampoConfig = state.getGarmentConfig();
                controller.disenoCampoLayers = (state.getLayers() != null) ? state.getLayers() : new java.util.ArrayList<>();
                controller.disenoArqueroConfig = state.getArqueroGarmentConfig();
                controller.disenoArqueroLayers = (state.getArqueroLayers() != null) ? state.getArqueroLayers() : new java.util.ArrayList<>();
                
                controller.arqueroActivoDesignId = null;
                controller.arqueroFichaDesignId = state.getSelectedGoalkeeperDesignId();

                if (controller.comboModoDiseno != null) {
                    controller.switchingDesignMode = true;
                    try {
                        controller.comboModoDiseno.setDisable(false);
                        controller.comboModoDiseno.setValue("Jugador");
                    } finally {
                        controller.switchingDesignMode = false;
                    }
                }

                if (state.getShippingInfo() != null) {
                    controller.datosEnvio = state.getShippingInfo();
                    controller.aplicarDatosEnvioToHiddenFields();
                    controller.actualizarDatosEnvioUI();
                }

                if (state.getOrderDetails() != null) {
                    controller.listaJugadores.clear();
                    for (org.example.dto.save.DetallePedidoDTO dpDto : state.getOrderDetails()) {
                        DetallePedido dp = new DetallePedido();
                        dp.setNombre(dpDto.getNombre());
                        dp.setNumero(dpDto.getNumero());
                        dp.setTalla(dpDto.getTalla());
                        dp.setIncludeTop(dpDto.isIncludeTop());
                        dp.setIncludeBottom(dpDto.isIncludeBottom());
                        dp.setIncludeSocks(dpDto.isIncludeSocks());
                        dp.setGenero(dpDto.getGenero());

                        dp.setArqueroDesignId(dpDto.getArqueroDesignId());
                        dp.setEsArquero(dpDto.isEsArquero());
                        dp.setArqueroOrdenMarcado(dpDto.getArqueroOrdenMarcado());
                        dp.setTipoMangaArquero(dpDto.getTipoMangaArquero());
                        if (dpDto.getColorArquero() != null) {
                            dp.setColorArquero(Color.web(dpDto.getColorArquero()));
                        }
                        dp.setTipoManga(dpDto.getTipoManga());
                        dp.setTipoBottom(dpDto.getTipoBottom());
                        dp.setTallaShort(dpDto.getTallaShort());
                        if (dpDto.getTipoMedias() != null) {
                            dp.setTipoMedias(dpDto.getTipoMedias());
                        }

                        controller.listaJugadores.add(dp);
                    }
                    controller.recomputeNextArqueroMarkOrder();
                }

                if (controller.personalizacionDelegate != null) {
                    List<javafx.scene.image.Image> logoLib = org.example.service.save.StateMapper.decodeImageLibrary(state.getLogoLibrary());
                    List<javafx.scene.image.Image> refLib = org.example.service.save.StateMapper.decodeImageLibrary(state.getReferenceLibrary());

                    if (!state.getShieldLibraryShirt().isEmpty() || !state.getShieldLibraryShort().isEmpty() || !state.getShieldLibrarySleeve().isEmpty()) {
                        controller.personalizacionDelegate.restoreLibrariesZoned(
                                logoLib,
                                org.example.service.save.StateMapper.decodeImageLibrary(state.getShieldLibraryShirt()),
                                org.example.service.save.StateMapper.decodeImageLibrary(state.getShieldLibraryShort()),
                                org.example.service.save.StateMapper.decodeImageLibrary(state.getShieldLibrarySleeve()),
                                refLib
                        );
                    } else {
                        controller.personalizacionDelegate.restoreLibraries(
                                logoLib,
                                org.example.service.save.StateMapper.decodeImageLibrary(state.getShieldLibrary()),
                                refLib
                        );
                    }
                    controller.personalizacionDelegate.syncReferences();
                }

                controller.currentProjectFile = file;
                controller.actualizarArchivoActualUI();
                controller.projectDirty = false;
                controller.fichaDirty = true;
                controller.actualizarTituloVentana();

                // Explicitly disable the restoration flag before synchronizing the goalkeeper list UI
                controller.isRestoringDesign = false;
                controller.goalkeeperCoordinator.updateOptionStatus();

                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            UIFactory.mostrarAlerta(javafx.scene.control.Alert.AlertType.ERROR, "Error", "Error al cargar el proyecto: " + e.getMessage());
        } finally {
            controller.isRestoringDesign = false;
            if (controller.prendaVisualizer.getHistoryManager() != null) {
                controller.prendaVisualizer.getHistoryManager().setRecording(true);
                controller.prendaVisualizer.getHistoryManager().clearHistory();
            }
        }
        return false;
    }
}
