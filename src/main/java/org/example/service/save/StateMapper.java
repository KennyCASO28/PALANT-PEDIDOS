package org.example.service.save;

import org.example.model.*;
import org.example.dto.save.*;
import javafx.scene.paint.Color;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.image.Image;

import org.example.component.GroupLayer;
import org.example.component.ImageLayer;
import org.example.component.PrendaVisualizer;
import org.example.component.ShapeLayer;
import org.example.component.TextLayer;
import org.example.component.GroupLayerV2;
import org.example.component.helper.SmartZoneContainer;
import org.example.model.DetallePedido;
import org.example.model.PrendaState;
import org.example.model.ShapeType;
import org.example.component.helper.PrendaColorManager;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;
import java.util.Base64;

public class StateMapper {
    private static double safeScale(double s) {
        return s <= 0.001 ? 1.0 : s;
    }

    public static ProjectState extractState(PrendaVisualizer visualizer, ObservableList<DetallePedido> roster) {
        return extractState(visualizer, roster, null);
    }

    public static PrendaStateDTO extractGarmentConfig(PrendaVisualizer visualizer) {
        if (visualizer == null) return null;
        return mapGarmentConfig(visualizer, visualizer.getState(), visualizer.getColorManager());
    }

    /**
     * Extracts a specific state's configuration without depending on what's active in the visualizer.
     */
    public static PrendaStateDTO extractGarmentConfig(PrendaVisualizer visualizer, PrendaState state, PrendaColorManager colorManager) {
        if (visualizer == null || state == null) return null;
        return mapGarmentConfig(visualizer, state, colorManager);
    }

    public static List<LayerDTO> extractUserLayers(PrendaVisualizer visualizer) {
        if (visualizer == null || visualizer.getUserLayerGroup() == null) return new ArrayList<>();
        return mapLayers(visualizer.getUserLayerGroup().getChildren());
    }

    /**
     * Safely clones a list of layers from a state.
     */
    public static List<LayerDTO> extractUserLayers(PrendaState state) {
        if (state == null || state.getUserLayers() == null) return new ArrayList<>();
        List<LayerDTO> list = new ArrayList<>();
        for (LayerDTO l : state.getUserLayers()) {
            if (l != null) {
                list.add(l.deepCopy());
            }
        }
        return list;
    }

    public static void restoreDesign(PrendaVisualizer visualizer, PrendaStateDTO config, List<LayerDTO> layers) {
        if (visualizer == null) return;
        if (config == null) {
            if (layers != null) restoreLayersOnly(visualizer, layers);
            return;
        }

        // Restore into the CURRENTLY ACTIVE state slot of the visualizer
        PrendaState state = visualizer.getState();
        PrendaColorManager colorManager = visualizer.getColorManager();
        
        restoreGarmentState(visualizer, state, colorManager, config);
        
        if (layers != null) {
            visualizer.clearUserLayers();
            restoreLayers(visualizer.getUserLayerGroup(), layers, visualizer);
            state.setUserLayers(new ArrayList<>(layers));
        }
        
        visualizer.cargarCapas();
    }

    public static ProjectState extractState(PrendaVisualizer visualizer, ObservableList<DetallePedido> roster,
            org.example.dto.DatosEnvioDTO shippingInfo) {
        ProjectState project = new ProjectState();

        // 1. Order Details
        project.setOrderDetails(mapRoster(roster));

        // 2. Garment Config (Camiseta)
        project.setGarmentConfig(mapGarmentConfig(visualizer, visualizer.getCamisetaState(), visualizer.getCamisetaColorManager()));

        // 3. Layers (Camiseta)
        boolean wasArq = visualizer.isEditandoArquero();
        if (wasArq) {
            project.setLayers(visualizer.getCamisetaState().getUserLayers());
        } else if (visualizer.getUserLayerGroup() != null) {
            project.setLayers(mapLayers(visualizer.getUserLayerGroup().getChildren()));
        }

        // 4. Arquero Design
        project.setArqueroGarmentConfig(mapGarmentConfig(visualizer, visualizer.getArqueroState(), visualizer.getArqueroColorManager()));
        if (!wasArq) {
            project.setArqueroLayers(visualizer.getArqueroState().getUserLayers());
        } else if (visualizer.getUserLayerGroup() != null) {
            project.setArqueroLayers(mapLayers(visualizer.getUserLayerGroup().getChildren()));
        }

        // 5. Fonts (Portable)
        Map<String, byte[]> allFonts = new HashMap<>();
        allFonts.putAll(visualizer.getCamisetaState().getCustomFonts());
        allFonts.putAll(visualizer.getArqueroState().getCustomFonts());
        
        List<FontLibraryDTO> fontLibrary = new ArrayList<>();
        allFonts.forEach((family, data) -> {
            fontLibrary.add(new FontLibraryDTO(family, Base64.getEncoder().encodeToString(data), "ttf"));
        });
        project.setFontLibrary(fontLibrary);

        if (shippingInfo != null) {
            project.setShippingInfo(shippingInfo);
        }

        // 13. Arquero Personalization Flag (V2.8+)
        project.setArqueroPersonalizado(visualizer.isArqueroDisenoPersonalizado());

        return project;
    }

    // --- Roster Mapping ---
    private static List<DetallePedidoDTO> mapRoster(List<DetallePedido> roster) {
        List<DetallePedidoDTO> list = new ArrayList<>();
        if (roster == null)
            return list;

        for (DetallePedido dp : roster) {
            DetallePedidoDTO dto = new DetallePedidoDTO();
            dto.setNombre(dp.getNombre());
            dto.setNumero(dp.getNumero());
            dto.setTalla(dp.getTalla());
            dto.setIncludeTop(dp.isIncludeTop());
            dto.setIncludeBottom(dp.isIncludeBottom());
            dto.setIncludeSocks(dp.isIncludeSocks());
            dto.setGenero(dp.getGenero());
            dto.setEsArquero(dp.isEsArquero());
            dto.setArqueroOrdenMarcado(dp.getArqueroOrdenMarcado());
            dto.setTipoMangaArquero(dp.getTipoMangaArquero());
            dto.setColorArquero(colorToHex(dp.getColorArquero()));
            dto.setTipoManga(dp.getTipoManga());
            dto.setTipoBottom(dp.getTipoBottom());
            dto.setTallaShort(dp.getTallaShort());
            list.add(dto);
        }
        return list;
    }

    public static org.example.dto.ConfiguracionPrendaDTO toConfigDTO(PrendaStateDTO state) {
        if (state == null)
            return null;
        org.example.dto.ConfiguracionPrendaDTO.Builder builder = new org.example.dto.ConfiguracionPrendaDTO.Builder();

        try {
            if (state.getCurrentGarmentType() != null) {
                try {
                   builder.tipoPrenda(TipoPrenda.valueOf(state.getCurrentGarmentType()));
                } catch(Exception e) {}
            }
            if (state.getCurrentGenero() != null)
                builder.genero(state.getCurrentGenero());
            if (state.getCurrentCorte() != null)
                builder.corte(state.getCurrentCorte());
            if (state.getCurrentLargo() != null)
                builder.largo(state.getCurrentLargo());
            if (state.getCurrentCuello() != null)
                builder.cuello(state.getCurrentCuello());
            if (state.getCurrentTela() != null)
                builder.tela(state.getCurrentTela());
            if (state.getCustomTela() != null)
                builder.customTela(state.getCustomTela());
            builder.tipoMedias(state.getCurrentTipoMedias());

            if (state.getShirtCrestTech() != null) {
                try {
                    builder.tipoEscudo(TipoEscudo.valueOf(state.getShirtCrestTech().toUpperCase()));
                } catch(Exception e) {}
            }

            builder.conMalla(state.isHasMesh());
            builder.conPunoCamiseta(state.isHasCuffs());
            builder.conFranjaCamiseta(state.isHasShirtStripe());
            builder.conShort(state.isHasShorts());
            builder.conMedias(state.isHasSocks());
            builder.conPunoShort(state.isHasShortsCuff());
            builder.conFranjaShort(state.isHasShortsStripe());
            builder.conPiqueteShort(state.isHasShortsPicket());
            builder.conBolsilloShort(state.isHasShortsPocket());
            builder.conPasadorShort(state.isHasShortsCord());
            builder.conForroShort(state.isHasShortsLining());
            builder.conLigaMedias(state.isHasSocksTop());
            builder.conAcolchado(state.isHasPadding());

            if (state.getCurrentCorteShort() != null)
                builder.corteShort(state.getCurrentCorteShort());

            builder.colors(state.getColors());
            builder.internalCodes(state.getInternalCodes());

        } catch (Exception e) {
            System.err.println("Error mapping to ConfigDTO: " + e.getMessage());
        }

        return builder.build();
    }

    private static PrendaStateDTO mapGarmentConfig(PrendaVisualizer visualizer, PrendaState state, PrendaColorManager colorManager) {
        PrendaStateDTO dto = new PrendaStateDTO();

        if (colorManager != null) {
            Map<String, String> colorHexMap = new HashMap<>();
            colorManager.getColorState().forEach((k, v) -> colorHexMap.put(k, colorToHex(v)));
            dto.setColors(colorHexMap);
            dto.setInternalCodes(new HashMap<>(colorManager.getInternalColorCodes()));
        }
        dto.setReferenceColorArquero(colorToHex(state.getColorReferenciaArquero()));

        dto.setCurrentGenero(state.getGenero());
        dto.setCurrentCorte(state.getCorte());
        dto.setCurrentCorteShort(state.getCorteShort());
        dto.setCurrentLargo(state.getLargo());
        dto.setCurrentCuello(state.getCuello());
        dto.setCurrentGarmentType(state.getGarmentType());
        dto.setCurrentTela(state.getTela());
        dto.setCustomTela(state.getCustomTela());
        dto.setCurrentTipoMedias(state.getTipoMedias());

        dto.setHasShirt(state.hasShirt());
        dto.setHasMesh(state.hasMesh());
        dto.setHasCuffs(state.hasCuffs());
        dto.setHasShirtStripe(state.hasShirtStripe());
        dto.setHasPadding(state.hasPadding());

        dto.setHasShorts(state.hasShorts());
        dto.setHasShortsStripe(state.hasShortsStripe());
        dto.setHasShortsPicket(state.hasShortsPicket());
        dto.setHasShortsPocket(state.hasShortsPocket());
        dto.setHasShortsCuff(state.hasShortsCuff());
        dto.setHasShortsCord(state.hasShortsCord());
        dto.setHasShortsLining(state.hasShortsLining());

        dto.setHasSocks(state.hasSocks());
        dto.setHasSocksTop(state.hasSocksTop());

        dto.setChestBrandVisible(state.isChestBrandVisible());
        dto.setChestBrandPosition(state.getChestBrandPosition());
        dto.setShortBrandVisible(state.isShortBrandVisible());
        dto.setShortBrandPosition(state.getShortBrandPosition());
        dto.setSocksBrandVisible(state.isSocksBrandVisible());
        dto.setBrandTech(state.getBrandTech());
        dto.setShortCrestVisible(state.isShortCrestVisible());
        dto.setSleeveCrestVisible(state.isSleeveCrestVisible());
        dto.setShirtCrestTech(state.getShirtCrestTech());
        dto.setShortCrestTech(state.getShortCrestTech());
        dto.setSleeveCrestTech(state.getSleeveCrestTech());

        dto.setChestNumberVisible(state.isChestNumberVisible());
        dto.setBackNumberVisible(state.isBackNumberVisible());
        dto.setShortNumberVisible(state.isShortNumberVisible());

        // --- Sync Live UI Transforms to DTO ---
        if (state == visualizer.getState()) {
            dto.setChestNumberX(visualizer.getChestNumber().getRoot().getLayoutX());
            dto.setChestNumberY(visualizer.getChestNumber().getRoot().getLayoutY());
            dto.setChestNumberScale(visualizer.getChestNumber().getRoot().getScaleX());

            dto.setBackNumberX(visualizer.getBackNumber().getRoot().getLayoutX());
            dto.setBackNumberY(visualizer.getBackNumber().getRoot().getLayoutY());
            dto.setBackNumberScale(visualizer.getBackNumber().getRoot().getScaleX());

            dto.setShortNumberX(visualizer.getShortNumber().getRoot().getLayoutX());
            dto.setShortNumberY(visualizer.getShortNumber().getRoot().getLayoutY());
            dto.setShortNumberScale(visualizer.getShortNumber().getRoot().getScaleX());
        } else {
            dto.setChestNumberX(state.getChestNumberX());
            dto.setChestNumberY(state.getChestNumberY());
            dto.setChestNumberScale(state.getChestNumberScale());
            dto.setBackNumberX(state.getBackNumberX());
            dto.setBackNumberY(state.getBackNumberY());
            dto.setBackNumberScale(state.getBackNumberScale());
            dto.setShortNumberX(state.getShortNumberX());
            dto.setShortNumberY(state.getShortNumberY());
            dto.setShortNumberScale(state.getShortNumberScale());
        }

        dto.setCurrentChestNumber(state.getCurrentChestNumber());
        dto.setCurrentBackNumber(state.getCurrentBackNumber());
        dto.setCurrentShortNumber(state.getCurrentShortNumber());

        Map<String, List<String>> numColorsMap = new HashMap<>();
        if (state == visualizer.getState()) {
            numColorsMap.put("chest", mapNumberColors(compositionToColorList(visualizer.getChestNumber())));
            numColorsMap.put("back", mapNumberColors(compositionToColorList(visualizer.getBackNumber())));
            numColorsMap.put("short", mapNumberColors(compositionToColorList(visualizer.getShortNumber())));
        } else {
            numColorsMap.put("chest", mapStateNumberColors(state.getChestNumberColors()));
            numColorsMap.put("back", mapStateNumberColors(state.getBackNumberColors()));
            numColorsMap.put("short", mapStateNumberColors(state.getShortNumberColors()));
        }
        dto.setNumberColors(numColorsMap);
        dto.setHotspots(mapHotspots(state.getReferenceHotspots()));

        return dto;
    }

    private static List<Color> compositionToColorList(org.example.component.NumberComposition comp) {
        List<Color> colors = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            colors.add(comp.getLayerColor(i));
        }
        return colors;
    }

    private static List<String> mapNumberColors(List<Color> colors) {
        List<String> hexes = new ArrayList<>();
        for (Color c : colors) { hexes.add(colorToHex(c)); }
        return hexes;
    }
    
    private static List<String> mapStateNumberColors(Map<Integer, Color> colors) {
        List<String> hexes = new ArrayList<>();
        if (colors == null || colors.isEmpty()) return hexes;
        for (int i = 0; i < 5; i++) {
            Color c = colors.get(i);
            hexes.add(colorToHex(c));
        }
        return hexes;
    }

    public static List<LayerDTO> mapLayers(List<Node> nodes) {
        List<LayerDTO> list = new ArrayList<>();
        if (nodes == null) return list;

        for (Node n : nodes) {
            if (n instanceof SmartZoneContainer) {
                SmartZoneContainer szc = (SmartZoneContainer) n;
                list.addAll(mapLayers(szc.getContentGroup().getChildren()));
            } else {
                LayerDTO dto = mapNodeToLayer(n);
                if (dto != null) { list.add(dto); }
            }
        }
        return list;
    }

    private static LayerDTO mapNodeToLayer(Node n) {
        LayerDTO dto = null;

        if (n instanceof ShapeLayer) {
            ShapeLayer sl = (ShapeLayer) n;
            ShapeDTO sDto = new ShapeDTO();
            sDto.setShapeType(sl.getType());
            sDto.setFillColor(colorToHex(sl.getFillColor()));
            sDto.setStrokeColor(colorToHex(sl.getStrokeColor()));
            sDto.setStrokeWidth(sl.getStrokeWidth());
            sDto.setClosed(sl.getIsClosed());

            if (sl.getType() == ShapeType.CUSTOM_PATH) {
                sDto.setSvgContent(sl.getShapeContent());
            }

            sDto.setArcWidth(sl.getArcWidth());
            sDto.setArcHeight(sl.getArcHeight());
            sDto.setGradientTransparency(sl.isTransparencyEnabled());
            sDto.setTransparencyAngle(sl.getTransparencyAngle());
            sDto.setTransparencyStartAlpha(sl.getTransparencyStartAlpha());
            sDto.setTransparencyEndAlpha(sl.getTransparencyEndAlpha());
            sDto.setTransparencyBalance(sl.getTransparencyBalance());

            sDto.setContourSteps(sl.getContourSteps());
            sDto.setContourDistance(sl.getContourDistance());
            sDto.setContourColor(colorToHex(sl.getContourColor()));
            sDto.setContourLineJoin(sl.getContourLineJoin().name());

            dto = sDto;
            dto.setActiveZone(sl.getActiveZone());
        } else if (n instanceof ImageLayer) {
            ImageLayer il = (ImageLayer) n;
            ImageDTO iDto = new ImageDTO();
            String b64 = il.getBase64Content();
            if (b64 == null) {
                b64 = encodeImage(il.getImage());
                il.setBase64Content(b64);
            }
            iDto.setBase64Content(b64);
            iDto.setWidth(il.getLogicalWidth());
            iDto.setHeight(il.getLogicalHeight());
            if (il.getBadgeType() != null) {
                iDto.setBadgeType(il.getBadgeType().name());
            }

            dto = iDto;
            dto.setActiveZone(il.getActiveZone());
        } else if (n instanceof TextLayer) {
            TextLayer tl = (TextLayer) n;
            TextDTO tDto = new TextDTO();
            tDto.setText(tl.getTextContent());
            tDto.setFontFamily(tl.getFont().getFamily());
            tDto.setFontSize(tl.getFont().getSize());
            tDto.setBold(tl.getFont().getStyle().toLowerCase().contains("bold"));
            tDto.setItalic(tl.getFont().getStyle().toLowerCase().contains("italic"));
            tDto.setColor(colorToHex(tl.getTextColor()));
            tDto.setStrokeColor(colorToHex(tl.getStrokeColor()));
            tDto.setStrokeWidth(tl.getStrokeWidth());
            tDto.setShapeType(tl.getShape().name());
            tDto.setArcFactor(tl.getArcFactor());
            tDto.setHeightScale(tl.getHeightScale());
            tDto.setWidthScale(tl.getWidthScale());
            tDto.setCurrentWidth(tl.getLogicalWidth());
            tDto.setCurrentHeight(tl.getLogicalHeight());

            // Contour
            tDto.setContourSteps(tl.getContourSteps());
            tDto.setContourDistance(tl.getContourDistance());
            tDto.setContourColor(colorToHex(tl.getContourColor()));

            dto = tDto;
            dto.setActiveZone(tl.getActiveZone());
        } else if (n instanceof GroupLayer) {
            GroupLayer gl = (GroupLayer) n;
            GroupDTO gDto = new GroupDTO();
            gDto.setChildren(mapLayers(gl.getUserLayers()));
            dto = gDto;
            dto.setActiveZone(gl.getActiveZone());
            dto.setLocked(gl.isUserLocked());
        } else if (n instanceof GroupLayerV2) {
            GroupLayerV2 gv2 = (GroupLayerV2) n;
            GroupDTO gDto = new GroupDTO();
            gDto.setChildren(mapLayers(gv2.getUserLayers()));
            dto = gDto;
            dto.setActiveZone(gv2.getActiveZone());
            dto.setLocked(gv2.isUserLocked());
        }

        if (dto != null) {
            dto.setX(n.getTranslateX());
            dto.setY(n.getTranslateY());

            if (n instanceof ShapeLayer) {
                ShapeLayer sl = (ShapeLayer) n;
                dto.setScaleX(sl.getInternalScaleX());
                dto.setScaleY(sl.getInternalScaleY());
                dto.setRotation(sl.getInternalRotation());
                dto.setWidth(sl.getLogicalWidth());
                dto.setHeight(sl.getLogicalHeight());
                dto.setLocked(sl.isUserLocked());
            } else if (n instanceof TextLayer) {
                TextLayer tl = (TextLayer) n;
                dto.setScaleX(tl.getInternalScaleX());
                dto.setScaleY(tl.getInternalScaleY());
                dto.setRotation(tl.getInternalRotation());
                dto.setWidth(tl.getLogicalWidth());
                dto.setHeight(tl.getLogicalHeight());
                dto.setLocked(tl.isUserLocked());
            } else if (n instanceof ImageLayer) {
                ImageLayer il = (ImageLayer) n;
                dto.setScaleX(il.getInternalScaleX());
                dto.setScaleY(il.getInternalScaleY());
                dto.setRotation(il.getInternalRotation());
                dto.setWidth(il.getLogicalWidth());
                dto.setHeight(il.getLogicalHeight());
                dto.setLocked(il.isUserLocked());
            } else if (n instanceof GroupLayer) {
                GroupLayer gl = (GroupLayer) n;
                dto.setScaleX(gl.getInternalScaleX());
                dto.setScaleY(gl.getInternalScaleY());
                dto.setRotation(gl.getInternalRotation());
            } else if (n instanceof GroupLayerV2) {
                GroupLayerV2 gv2 = (GroupLayerV2) n;
                dto.setScaleX(gv2.getInternalScaleX());
                dto.setScaleY(gv2.getInternalScaleY());
                dto.setRotation(gv2.getInternalRotation());
            } else {
                dto.setScaleX(n.getScaleX());
                dto.setScaleY(n.getScaleY());
                dto.setRotation(n.getRotate());
            }
            dto.setZIndex(0);
        }
        return dto;
    }

    private static String colorToHex(Color c) {
        if (c == null) return "#00000000";
        return String.format("#%02X%02X%02X%02X",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255),
                (int) (c.getOpacity() * 255));
    }

    public static void restoreState(PrendaVisualizer visualizer, ProjectState project) {
        if (visualizer == null || project == null) return;
        visualizer.setNotificationsSuspended(true);
        try {
            visualizer.setVisible(false);

            // 1. Restore Fonts (Portable)
            if (project.getFontLibrary() != null) {
                for (FontLibraryDTO f : project.getFontLibrary()) {
                    try {
                        byte[] data = Base64.getDecoder().decode(f.getBase64Content());
                        javafx.scene.text.Font.loadFont(new java.io.ByteArrayInputStream(data), 12);
                        visualizer.getCamisetaState().addCustomFont(f.getFamilyName(), data);
                        visualizer.getArqueroState().addCustomFont(f.getFamilyName(), data);
                    } catch (Exception e) {}
                }
            }

            // 2. Clear Visualizer Nodes to start fresh
            if (visualizer.getPowerClipManager() != null) visualizer.getPowerClipManager().reset();
            visualizer.clearUserLayers();

            // 3. Restore State Objects (Camiseta & Arquero) - Values and Layer Definitions
            if (project.getGarmentConfig() != null) {
                restoreGarmentState(visualizer, visualizer.getCamisetaState(), visualizer.getCamisetaColorManager(), project.getGarmentConfig());
                // Load saved layers into the state object
                List<LayerDTO> loadedLayers = project.getLayers() != null ? project.getLayers() : new ArrayList<>();
                visualizer.getCamisetaState().setUserLayers(new ArrayList<>(loadedLayers));
            }
            
            if (project.getArqueroGarmentConfig() != null) {
                // Ensure Arquero state exists and initialized
                visualizer.getArqueroColorManager(); 
                restoreGarmentState(visualizer, visualizer.getArqueroState(), visualizer.getArqueroColorManager(), project.getArqueroGarmentConfig());
                // Load saved ARQUERO layers into its state object
                List<LayerDTO> loadedArqLayers = project.getArqueroLayers() != null ? project.getArqueroLayers() : new ArrayList<>();
                visualizer.getArqueroState().setUserLayers(new ArrayList<>(loadedArqLayers));
            }

            // 4. Force UI Sync (Deterministic initialization)
            // SET isSwappingDesign = true to prevent destructive sync while initializing
            visualizer.setSwappingDesign(true); 
            try {
                // Determine the target state after load (Player is default)
                // We swap to Goalie ONLY if there are goalie layers, to "prime" its SVG,
                // then land on Player mode as the default starting view.
                if (project.getArqueroGarmentConfig() != null) {
                    visualizer.setActiveDesign(true); 
                }
                visualizer.setActiveDesign(false); 
            } finally {
                visualizer.setSwappingDesign(false);
            }
            
            // 5. Restore Goalie Personalized Flag (V2.8+)
            visualizer.setArqueroDisenoPersonalizado(project.isArqueroPersonalizado());

        } finally {
            visualizer.setVisible(true);
            visualizer.setNotificationsSuspended(false);
            visualizer.notifyStateChanged();
        }
    }

    public static void restoreLayersOnly(PrendaVisualizer visualizer, List<LayerDTO> layers) {
        if (visualizer == null || layers == null) return;
        visualizer.setNotificationsSuspended(true);
        try {
            visualizer.setVisible(false);
            visualizer.clearUserLayers();
            restoreLayers(visualizer.getUserLayerGroup(), layers, visualizer);
            visualizer.cargarCapas();
        } finally {
            visualizer.setVisible(true);
            visualizer.setNotificationsSuspended(false);
            visualizer.notifyStateChanged();
        }
    }



    private static void restoreLayers(javafx.scene.Group parent, List<LayerDTO> layers, PrendaVisualizer visualizer) {
        for (LayerDTO dto : layers) {
            Node node = createNodeFromDTO(dto, visualizer);
            if (node != null) {
                if (node instanceof ShapeLayer) {
                    ShapeLayer sl = (ShapeLayer) node;
                    sl.setInternalScaleX(safeScale(dto.getScaleX()));
                    sl.setInternalScaleY(safeScale(dto.getScaleY()));
                    sl.setInternalRotation(dto.getRotation());
                } else if (node instanceof ImageLayer) {
                    ImageLayer il = (ImageLayer) node;
                    il.setInternalScaleX(safeScale(dto.getScaleX()));
                    il.setInternalScaleY(safeScale(dto.getScaleY()));
                    il.setInternalRotation(dto.getRotation());
                } else if (node instanceof TextLayer) {
                    TextLayer tl = (TextLayer) node;
                    tl.setInternalScaleX(safeScale(dto.getScaleX()));
                    tl.setInternalScaleY(safeScale(dto.getScaleY()));
                    tl.setInternalRotation(dto.getRotation());
                } else if (node instanceof GroupLayer) {
                    GroupLayer gl = (GroupLayer) node;
                    gl.setInternalScaleX(safeScale(dto.getScaleX()));
                    gl.setInternalScaleY(safeScale(dto.getScaleY()));
                    gl.setInternalRotation(dto.getRotation());
                } else if (node instanceof GroupLayerV2) {
                    GroupLayerV2 gv2 = (GroupLayerV2) node;
                    gv2.setInternalScaleX(safeScale(dto.getScaleX()));
                    gv2.setInternalScaleY(safeScale(dto.getScaleY()));
                    gv2.setInternalRotation(dto.getRotation());
                } else {
                    node.setScaleX(safeScale(dto.getScaleX()));
                    node.setScaleY(safeScale(dto.getScaleY()));
                    node.setRotate(dto.getRotation());
                }

                if (dto.getActiveZone() != null) {
                    visualizer.getPowerClipManager().restoreToContainer(node, dto.getActiveZone());
                    node.setTranslateX(dto.getX());
                    node.setTranslateY(dto.getY());
                    visualizer.getLayerManager().addLayerToContainer(node, (javafx.scene.Group) node.getParent(), false);
                } else {
                    visualizer.getLayerManager().addLayerToContainer(node, parent, false);
                    node.setTranslateX(dto.getX());
                    node.setTranslateY(dto.getY());
                }
            }
        }
    }

    private static Node createNodeFromDTO(LayerDTO dto, PrendaVisualizer visualizer) {
        if (dto instanceof ShapeDTO) {
            ShapeDTO s = (ShapeDTO) dto;
            Color fill = Color.web(s.getFillColor());
            Color stroke = Color.web(s.getStrokeColor());
            ShapeLayer sl = new ShapeLayer(s.getShapeType(), fill, stroke, s.getStrokeWidth());
            sl.setVisualizer(visualizer);
            sl.setIsClosed(s.isClosed());
            if (s.getActiveZone() != null) sl.setActiveZone(s.getActiveZone());
            if (s.getShapeType() == ShapeType.CUSTOM_PATH && s.getSvgContent() != null) sl.setSvgPathData(s.getSvgContent());
            sl.setArcWidth(s.getArcWidth());
            sl.setArcHeight(s.getArcHeight());
            if (s.isGradientTransparency()) {
                sl.setTransparency(true, s.getTransparencyAngle(), s.getTransparencyStartAlpha(), s.getTransparencyEndAlpha());
                sl.setTransparencyBalance(s.getTransparencyBalance());
            }
            if (dto.getScaleX() != 1.0) sl.setInternalScaleX(safeScale(dto.getScaleX()));
            if (dto.getScaleY() != 1.0) sl.setInternalScaleY(safeScale(dto.getScaleY()));
            if (dto.getRotation() != 0) sl.setInternalRotation(dto.getRotation());
            if (dto.getWidth() > 0 && dto.getHeight() > 0) sl.setPrefSize(dto.getWidth(), dto.getHeight());
            if (s.getContourSteps() > 0) {
                Color cColor = s.getContourColor() != null ? Color.web(s.getContourColor()) : Color.BLACK;
                if (s.getContourLineJoin() != null) {
                    try { sl.setContourLineJoin(javafx.scene.shape.StrokeLineJoin.valueOf(s.getContourLineJoin())); } catch (Exception e) {}
                }
                sl.applyContour(s.getContourSteps(), s.getContourDistance(), cColor);
            }
            if (dto.isLocked()) sl.setUserLocked(true);
            return sl;
        } else if (dto instanceof ImageDTO) {
            ImageDTO i = (ImageDTO) dto;
            if (i.getBase64Content() != null) {
                javafx.scene.image.Image img = decodeImage(i.getBase64Content());
                if (img != null) {
                    ImageLayer il = new ImageLayer(img);
                    il.setBase64Content(i.getBase64Content());
                    il.setVisualizer(visualizer);
                    if (i.getActiveZone() != null) il.setActiveZone(i.getActiveZone());
                    if (dto.getScaleX() != 1.0) il.setInternalScaleX(safeScale(dto.getScaleX()));
                    if (dto.getScaleY() != 1.0) il.setInternalScaleY(safeScale(dto.getScaleY()));
                    if (dto.getRotation() != 0) il.setInternalRotation(dto.getRotation());
                    if (dto.getWidth() > 1 && dto.getHeight() > 1) il.resize(dto.getWidth(), dto.getHeight());
                    if (dto.isLocked()) il.setUserLocked(true);
                    if (i.getBadgeType() != null) {
                        try { il.setBadgeType(org.example.model.TipoEscudo.valueOf(i.getBadgeType())); } catch (Exception ex) {}
                    }
                    return il;
                }
            }
        } else if (dto instanceof GroupDTO) {
            GroupDTO g = (GroupDTO) dto;
            GroupLayer gl = new GroupLayer();
            gl.setVisualizer(visualizer);
            if (g.getActiveZone() != null) gl.setActiveZone(g.getActiveZone());
            if (dto.getScaleX() != 1.0) gl.setInternalScaleX(safeScale(dto.getScaleX()));
            if (dto.getScaleY() != 1.0) gl.setInternalScaleY(safeScale(dto.getScaleY()));
            if (dto.getRotation() != 0) gl.setInternalRotation(dto.getRotation());
            if (g.getChildren() != null) {
                for (LayerDTO childDto : g.getChildren()) {
                    Node childNode = createNodeFromDTO(childDto, visualizer);
                    if (childNode != null) gl.getContentGroup().getChildren().add(childNode);
                }
                gl.recalculateBounds();
            }
            if (dto.isLocked()) gl.setUserLocked(true);
            return gl;
        } else if (dto instanceof TextDTO) {
            TextDTO t = (TextDTO) dto;
            javafx.scene.text.Font font = javafx.scene.text.Font.font(t.getFontFamily(), t.getFontSize());
            Color fontFill = Color.web(t.getColor());
            Color fontStroke = t.getStrokeColor() != null ? Color.web(t.getStrokeColor()) : Color.TRANSPARENT;
            org.example.model.TextShape shape = org.example.model.TextShape.STRAIGHT;
            try { shape = org.example.model.TextShape.valueOf(t.getShapeType()); } catch (Exception e) {}
            TextLayer tl = new TextLayer(t.getText(), font, fontFill, shape);
            tl.setVisualizer(visualizer);
            tl.setStrokeColor(fontStroke);
            tl.setStrokeWidth(t.getStrokeWidth());
            tl.setArcFactor(t.getArcFactor());
            tl.setHeightScale(t.getHeightScale());
            tl.setWidthScale(t.getWidthScale());
            javafx.scene.text.FontWeight weight = t.isBold() ? javafx.scene.text.FontWeight.BOLD : javafx.scene.text.FontWeight.NORMAL;
            javafx.scene.text.FontPosture posture = t.isItalic() ? javafx.scene.text.FontPosture.ITALIC : javafx.scene.text.FontPosture.REGULAR;
            tl.setFont(javafx.scene.text.Font.font(t.getFontFamily(), weight, posture, t.getFontSize()));
            tl.setTextSize(t.getCurrentWidth(), t.getCurrentHeight());
            
            // Restore Contour
            if (t.getContourSteps() > 0) {
                Color cContour = t.getContourColor() != null ? Color.web(t.getContourColor()) : Color.TRANSPARENT;
                tl.applyContour(t.getContourSteps(), t.getContourDistance(), cContour);
            }

            if (t.getActiveZone() != null) tl.setActiveZone(t.getActiveZone());
            if (t.getScaleX() != 1.0) tl.setInternalScaleX(safeScale(t.getScaleX()));
            if (t.getScaleY() != 1.0) tl.setInternalScaleY(safeScale(t.getScaleY()));
            if (t.getRotation() != 0) tl.setInternalRotation(t.getRotation());
            if (t.isLocked()) tl.setUserLocked(true);
            return tl;
        }
        return null;
    }

    private static javafx.scene.image.Image decodeImage(String base64) {
        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64);
            return new javafx.scene.image.Image(new java.io.ByteArrayInputStream(imageBytes));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String encodeImage(javafx.scene.image.Image img) {
        try {
            java.awt.image.BufferedImage bImg = SwingFXUtils.fromFXImage(img, null);
            if (bImg == null) return null;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(bImg, "png", bos);
            return Base64.getEncoder().encodeToString(bos.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<String> encodeImageLibrary(List<javafx.scene.image.Image> images) {
        List<String> result = new ArrayList<>();
        if (images == null) return result;
        for (javafx.scene.image.Image img : images) {
            String encoded = encodeImage(img);
            if (encoded != null) result.add(encoded);
        }
        return result;
    }

    public static List<javafx.scene.image.Image> decodeImageLibrary(List<String> base64List) {
        List<javafx.scene.image.Image> result = new ArrayList<>();
        if (base64List == null) return result;
        for (String b64 : base64List) {
            javafx.scene.image.Image img = decodeImage(b64);
            if (img != null) result.add(img);
        }
        return result;
    }

    public static List<ReferenceHotspotDTO> mapHotspots(List<PrendaState.ReferenceHotspot> hotspots) {
        List<ReferenceHotspotDTO> list = new ArrayList<>();
        if (hotspots == null) return list;
        for (PrendaState.ReferenceHotspot hs : hotspots) {
            ReferenceHotspotDTO dto = new ReferenceHotspotDTO();
            dto.setX(hs.getX());
            dto.setY(hs.getY());
            dto.setZone(hs.getZone());
            dto.setLabel(hs.getLabel());
            dto.setDescription(hs.getDescription());
            if (hs.getImageData() != null && hs.getImageData().length > 0) {
                try { dto.setBase64Image(Base64.getEncoder().encodeToString(hs.getImageData())); } catch (Exception e) {}
            } else if (hs.getImagePath() != null) {
                try {
                    javafx.scene.image.Image img = new javafx.scene.image.Image(hs.getImagePath());
                    dto.setBase64Image(encodeImage(img));
                } catch (Exception e) {}
            }
            list.add(dto);
        }
        return list;
    }

    public static void restoreHotspots(PrendaVisualizer visualizer, List<ReferenceHotspotDTO> dtos) {
        if (dtos == null) return;
        List<org.example.model.PrendaState.ReferenceHotspot> list = mapHotspotsToState(dtos);
        visualizer.getState().setReferenceHotspots(list);
    }

    public static List<org.example.model.PrendaState.ReferenceHotspot> mapHotspotsToState(List<ReferenceHotspotDTO> dtos) {
        List<org.example.model.PrendaState.ReferenceHotspot> list = new ArrayList<>();
        if (dtos == null) return list;
        for (ReferenceHotspotDTO dto : dtos) {
            org.example.model.PrendaState.ReferenceHotspot hs = new org.example.model.PrendaState.ReferenceHotspot();
            hs.setX(dto.getX());
            hs.setY(dto.getY());
            hs.setZone(dto.getZone());
            hs.setLabel(dto.getLabel());
            hs.setDescription(dto.getDescription());
            if (dto.getBase64Image() != null) {
                hs.setImagePath("data:image/png;base64," + dto.getBase64Image());
                try { hs.setImageData(Base64.getDecoder().decode(dto.getBase64Image())); } catch (Exception e) {}
            }
            list.add(hs);
        }
        return list;
    }

    private static void restoreGarmentState(PrendaVisualizer visualizer, PrendaState state, PrendaColorManager colorManager, PrendaStateDTO config) {
        if (config.getCurrentGarmentType() != null) state.setGarmentType(config.getCurrentGarmentType());
        if (config.getCurrentGenero() != null) state.setGenero(config.getCurrentGenero());
        if (config.getCurrentCorte() != null) state.setCorte(config.getCurrentCorte());
        if (config.getCurrentCorteShort() != null) state.setCorteShort(config.getCurrentCorteShort());
        if (config.getCurrentLargo() != null) state.setLargo(config.getCurrentLargo());
        if (config.getCurrentCuello() != null) state.setCuello(config.getCurrentCuello());
        if (config.getCurrentTela() != null) state.setTela(config.getCurrentTela());
        if (config.getCustomTela() != null) state.setCustomTela(config.getCustomTela());
        state.setTipoMedias(config.getCurrentTipoMedias());

        if (config.getColors() != null && colorManager != null) {
            config.getColors().forEach((k, v) -> {
                try { colorManager.setPartColor(k, Color.web(v)); } catch (Exception e) {}
            });
            state.setColors(new HashMap<>(config.getColors().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(e -> e.getKey(), e -> {
                    try { return Color.web(e.getValue()); } catch (Exception ex) { return Color.WHITE; }
                }))));
        }
        if (config.getInternalCodes() != null && colorManager != null) {
            colorManager.getInternalColorCodes().clear();
            colorManager.getInternalColorCodes().putAll(config.getInternalCodes());
            state.getInternalCodes().clear();
            state.getInternalCodes().putAll(config.getInternalCodes());
        }
        if (config.getReferenceColorArquero() != null) {
            try { state.setColorReferenciaArquero(Color.web(config.getReferenceColorArquero())); } catch (Exception e) {}
        }

        state.setHasShirt(config.isHasShirt());
        state.setHasMesh(config.isHasMesh());
        state.setHasCuffs(config.isHasCuffs());
        state.setHasShirtStripe(config.isHasShirtStripe());
        state.setHasPadding(config.isHasPadding());
        state.setHasShorts(config.isHasShorts());
        state.setHasShortsStripe(config.isHasShortsStripe());
        state.setHasShortsPicket(config.isHasShortsPicket());
        state.setHasShortsPocket(config.isHasShortsPocket());
        state.setHasShortsCuff(config.isHasShortsCuff());
        state.setHasShortsCord(config.isHasShortsCord());
        state.setHasShortsLining(config.isHasShortsLining());
        state.setHasSocks(config.isHasSocks());
        state.setHasSocksTop(config.isHasSocksTop());

        state.setChestBrandVisible(config.isChestBrandVisible());
        state.setChestBrandPosition(config.getChestBrandPosition());
        state.setShortBrandVisible(config.isShortBrandVisible());
        state.setShortBrandPosition(config.getShortBrandPosition());
        state.setSocksBrandVisible(config.isSocksBrandVisible());
        state.setBrandTech(config.getBrandTech());
        state.setShirtCrestTech(config.getShirtCrestTech() != null ? config.getShirtCrestTech() : "Bordado");
        state.setShortCrestVisible(config.isShortCrestVisible());
        state.setSleeveCrestVisible(config.isSleeveCrestVisible());
        state.setShortCrestTech(config.getShortCrestTech() != null ? config.getShortCrestTech() : "Bordado");
        state.setSleeveCrestTech(config.getSleeveCrestTech() != null ? config.getSleeveCrestTech() : "Bordado");

        state.setChestNumberVisible(config.isChestNumberVisible());
        state.setBackNumberVisible(config.isBackNumberVisible());
        state.setShortNumberVisible(config.isShortNumberVisible());
        state.setCurrentChestNumber(config.getCurrentChestNumber() != null ? config.getCurrentChestNumber() : "9");
        state.setCurrentBackNumber(config.getCurrentBackNumber() != null ? config.getCurrentBackNumber() : "9");
        state.setCurrentShortNumber(config.getCurrentShortNumber() != null ? config.getCurrentShortNumber() : "9");

        state.setChestNumberX(config.getChestNumberX());
        state.setChestNumberY(config.getChestNumberY());
        state.setChestNumberScale(config.getChestNumberScale());
        state.setBackNumberX(config.getBackNumberX());
        state.setBackNumberY(config.getBackNumberY());
        state.setBackNumberScale(config.getBackNumberScale());
        state.setShortNumberX(config.getShortNumberX());
        state.setShortNumberY(config.getShortNumberY());
        state.setShortNumberScale(config.getShortNumberScale());

        if (config.getNumberColors() != null) {
            state.getChestNumberColors().putAll(hexMapToColorMap(config.getNumberColors().get("chest")));
            state.getBackNumberColors().putAll(hexMapToColorMap(config.getNumberColors().get("back")));
            state.getShortNumberColors().putAll(hexMapToColorMap(config.getNumberColors().get("short")));
        }

        if (config.getHotspots() != null) {
            state.getReferenceHotspots().clear();
            state.getReferenceHotspots().addAll(mapHotspotsToState(config.getHotspots()));
        }
    }

    private static Map<Integer, Color> hexMapToColorMap(List<String> hexes) {
        Map<Integer, Color> map = new HashMap<>();
        if (hexes != null) {
            for (int i = 0; i < hexes.size(); i++) {
                try { 
                    if (hexes.get(i) != null && !hexes.get(i).isEmpty())
                        map.put(i, Color.web(hexes.get(i))); 
                } catch (Exception e) {}
            }
        }
        return map;
    }
}
