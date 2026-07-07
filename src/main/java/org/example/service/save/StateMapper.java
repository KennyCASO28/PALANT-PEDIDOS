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
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.TimeUnit;

public class StateMapper {
    private static double safeScale(double s) {
        return Math.abs(s) <= 0.001 ? 1.0 : s;
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
        
        visualizer.cargarCapas();
        
        if (layers != null) {
            visualizer.clearUserLayers();
            restoreLayers(visualizer.getUserLayerGroup(), layers, visualizer);
            state.setUserLayers(new ArrayList<>(layers));
        }
    }

    public static ProjectState extractState(PrendaVisualizer visualizer, ObservableList<DetallePedido> roster,
            org.example.dto.DatosEnvioDTO shippingInfo) {
        ProjectState project = new ProjectState();

        // 1. Order Details
        project.setOrderDetails(mapRoster(roster));

        // 2. Garment Config & Layers
        boolean wasArq = visualizer.isEditandoArquero();
        
        try {
            // Force player mode to accurately extract player's active layer group
            if (wasArq) {
                visualizer.setActiveDesign(false);
            }
            
            project.setGarmentConfig(extractGarmentConfig(visualizer, visualizer.getCamisetaState(), visualizer.getCamisetaColorManager()));
            project.setLayers(mapLayers(visualizer.getUserLayerGroup().getChildren()));
            
            // Force goalie mode to accurately extract goalie's active layer group
            visualizer.setActiveDesign(true);
            project.setArqueroGarmentConfig(extractGarmentConfig(visualizer, visualizer.getArqueroState(), visualizer.getArqueroColorManager()));
            project.setArqueroLayers(visualizer.getArqueroState().getUserLayers());
            
        } finally {
            // Restore original view mode
            visualizer.setActiveDesign(wasArq);
        }

        // 4. Goalie Personalization Flag
        project.setArqueroPersonalizado(visualizer.isArqueroDisenoPersonalizado());

        // 5. Shipping Info
        if (shippingInfo != null) {
            project.setShippingInfo(shippingInfo);
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
            dto.setTipoMedias(dp.getTipoMedias());
            dto.setArqueroDesignId(dp.getArqueroDesignId());
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
                    builder.tipoEscudo(TipoEscudo.valueOfTech(state.getShirtCrestTech()));
                } catch(Exception e) {}
            }

            builder.conMalla(state.isHasMesh());
            builder.conPunoCamiseta(state.isHasCuffs());
            builder.conFranjaCamiseta(state.isHasShirtStripe());
            builder.conLineaCamiseta(state.isHasShirtLinea());
            builder.conShort(state.isHasShorts());
            builder.conMedias(state.isHasSocks());
            builder.conPunoShort(state.isHasShortsCuff());
            builder.conFranjaShort(state.isHasShortsStripe());
            builder.conLineaShort(state.isHasShortsLinea());
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
        dto.setHasShirtLinea(state.hasShirtLinea());
        dto.setHasPadding(state.hasPadding());

        dto.setHasShorts(state.hasShorts());
        dto.setHasShortsStripe(state.hasShortsStripe());
        dto.setHasShortsLinea(state.hasShortsLinea());
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
                if (sl.getBezierNodes() != null) {
                    java.util.List<org.example.dto.save.BezierNodeDTO> bnDtos = new java.util.ArrayList<>();
                    for (org.example.model.BezierNode bn : sl.getBezierNodes()) {
                        org.example.dto.save.BezierNodeDTO bnDto = new org.example.dto.save.BezierNodeDTO();
                        bnDto.setAnchorX(bn.anchor.getX());
                        bnDto.setAnchorY(bn.anchor.getY());
                        if (bn.control1 != null) {
                            bnDto.setHasControl1(true);
                            bnDto.setControl1X(bn.control1.getX());
                            bnDto.setControl1Y(bn.control1.getY());
                        }
                        if (bn.control2 != null) {
                            bnDto.setHasControl2(true);
                            bnDto.setControl2X(bn.control2.getX());
                            bnDto.setControl2Y(bn.control2.getY());
                        }
                        bnDto.setMoveTo(bn.isMoveTo);
                        if (bn.type != null) bnDto.setType(bn.type.name());
                        if (bn.segmentType != null) bnDto.setSegmentType(bn.segmentType.name());
                        bnDtos.add(bnDto);
                    }
                    sDto.setBezierNodes(bnDtos);
                }
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
                dto.setShearX(sl.getInternalShearX());
                dto.setShearY(sl.getInternalShearY());
                dto.setCustomPivotX(sl.getCustomPivotX());
                dto.setCustomPivotY(sl.getCustomPivotY());
                dto.setRotation(sl.getInternalRotation());
                dto.setWidth(sl.getLogicalWidth());
                dto.setHeight(sl.getLogicalHeight());
                dto.setLocked(sl.isUserLocked());
            } else if (n instanceof TextLayer) {
                TextLayer tl = (TextLayer) n;
                dto.setScaleX(tl.getInternalScaleX());
                dto.setScaleY(tl.getInternalScaleY());
                dto.setShearX(tl.getInternalShearX());
                dto.setShearY(tl.getInternalShearY());
                dto.setCustomPivotX(tl.getCustomPivotX());
                dto.setCustomPivotY(tl.getCustomPivotY());
                dto.setRotation(tl.getInternalRotation());
                dto.setWidth(tl.getLogicalWidth());
                dto.setHeight(tl.getLogicalHeight());
                dto.setLocked(tl.isUserLocked());
            } else if (n instanceof ImageLayer) {
                ImageLayer il = (ImageLayer) n;
                dto.setScaleX(il.getInternalScaleX());
                dto.setScaleY(il.getInternalScaleY());
                dto.setShearX(il.getInternalShearX());
                dto.setShearY(il.getInternalShearY());
                dto.setCustomPivotX(il.getCustomPivotX());
                dto.setCustomPivotY(il.getCustomPivotY());
                dto.setRotation(il.getInternalRotation());
                dto.setWidth(il.getLogicalWidth());
                dto.setHeight(il.getLogicalHeight());
                dto.setLocked(il.isUserLocked());
            } else if (n instanceof GroupLayer) {
                GroupLayer gl = (GroupLayer) n;
                dto.setScaleX(gl.getInternalScaleX());
                dto.setScaleY(gl.getInternalScaleY());
                dto.setShearX(gl.getInternalShearX());
                dto.setShearY(gl.getInternalShearY());
                dto.setCustomPivotX(gl.getCustomPivotX());
                dto.setCustomPivotY(gl.getCustomPivotY());
                dto.setRotation(gl.getInternalRotation());
            } else if (n instanceof GroupLayerV2) {
                GroupLayerV2 gv2 = (GroupLayerV2) n;
                dto.setScaleX(gv2.getInternalScaleX());
                dto.setScaleY(gv2.getInternalScaleY());
                dto.setShearX(gv2.getInternalShearX());
                dto.setShearY(gv2.getInternalShearY());
                dto.setCustomPivotX(gv2.getCustomPivotX());
                dto.setCustomPivotY(gv2.getCustomPivotY());
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
        
        // --- BACKWARDS COMPATIBILITY FOR V2 FILES ---
        // If the project was saved while looking at the Arquero, the player config was skipped or is empty.
        // We use the Arquero config as a fallback so the UI can load and the player has a valid garment state.
        if (project.getGarmentConfig() == null) {
            if (project.getArqueroGarmentConfig() != null && project.getArqueroGarmentConfig().getCurrentGarmentType() != null) {
                project.setGarmentConfig(project.getArqueroGarmentConfig());
            } else {
                // If both are null, create a default player config
                PrendaStateDTO def = new PrendaStateDTO();
                def.setCurrentGarmentType("CAMISETA");
                def.setCurrentGenero(TipoGenero.HOMBRE);
                def.setCurrentTela(TipoTela.WIN);
                project.setGarmentConfig(def);
            }
        } else if (project.getGarmentConfig().getCurrentGarmentType() == null) {
            if (project.getArqueroGarmentConfig() != null && project.getArqueroGarmentConfig().getCurrentGarmentType() != null) {
                project.setGarmentConfig(project.getArqueroGarmentConfig());
            } else {
                // Default to Camiseta if no goalie config exists
                project.getGarmentConfig().setCurrentGarmentType("CAMISETA");
                if (project.getGarmentConfig().getCurrentGenero() == null) {
                    project.getGarmentConfig().setCurrentGenero(TipoGenero.HOMBRE);
                }
                if (project.getGarmentConfig().getCurrentTela() == null) {
                    project.getGarmentConfig().setCurrentTela(TipoTela.WIN);
                }
            }
        }

        // Also ensure goalie config has a garment type if it is present
        if (project.getArqueroGarmentConfig() != null && project.getArqueroGarmentConfig().getCurrentGarmentType() == null) {
            project.getArqueroGarmentConfig().setCurrentGarmentType(project.getGarmentConfig().getCurrentGarmentType());
            if (project.getArqueroGarmentConfig().getCurrentGenero() == null) {
                project.getArqueroGarmentConfig().setCurrentGenero(project.getGarmentConfig().getCurrentGenero());
            }
            if (project.getArqueroGarmentConfig().getCurrentTela() == null) {
                project.getArqueroGarmentConfig().setCurrentTela(project.getGarmentConfig().getCurrentTela());
            }
        }

        visualizer.setNotificationsSuspended(true);
        visualizer.invalidateSignatures();
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

            // 2.5 Restore Goalie Personalized Flag (V2.8+)
            visualizer.setArqueroDisenoPersonalizado(project.isArqueroPersonalizado());

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
                    visualizer.setActiveDesign(false); 
                }
                
                StateMapper.restoreDesign(visualizer, null, visualizer.getCamisetaState().getUserLayers());
                // Use isSwappingDesign=false so numbers are immediately shown with correct visibility
                visualizer.getNumberManager().restoreNumbersFromState(visualizer.getCamisetaState(), false, false);
                visualizer.applyVisibility();
            } finally {
                visualizer.setSwappingDesign(false);
            }
            
            // 5. Restore Goalie Personalized Flag (V2.8+)
            visualizer.setArqueroDisenoPersonalizado(project.isArqueroPersonalizado());

        } finally {
            visualizer.setVisible(true);
            visualizer.setNotificationsSuspended(false);
            // Clear the node signature cache so cargarCapas() does a complete re-evaluation
            // This prevents a stale cache from hiding numbers that are marked visible in the saved state
            visualizer.invalidateSignatures();
            visualizer.cargarCapas(); // Forces regeneration of SVGs and PowerClip refresh
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
        } finally {
            visualizer.setVisible(true);
            visualizer.setNotificationsSuspended(false);
            visualizer.cargarCapas();
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
                    sl.setInternalShearX(dto.getShearX());
                    sl.setInternalShearY(dto.getShearY());
                    sl.setCustomPivotX(dto.getCustomPivotX());
                    sl.setCustomPivotY(dto.getCustomPivotY());
                } else if (node instanceof ImageLayer) {
                    ImageLayer il = (ImageLayer) node;
                    il.setInternalScaleX(safeScale(dto.getScaleX()));
                    il.setInternalScaleY(safeScale(dto.getScaleY()));
                    il.setInternalRotation(dto.getRotation());
                    il.setInternalShearX(dto.getShearX());
                    il.setInternalShearY(dto.getShearY());
                    il.setCustomPivotX(dto.getCustomPivotX());
                    il.setCustomPivotY(dto.getCustomPivotY());
                } else if (node instanceof TextLayer) {
                    TextLayer tl = (TextLayer) node;
                    tl.setInternalScaleX(safeScale(dto.getScaleX()));
                    tl.setInternalScaleY(safeScale(dto.getScaleY()));
                    tl.setInternalRotation(dto.getRotation());
                    tl.setInternalShearX(dto.getShearX());
                    tl.setInternalShearY(dto.getShearY());
                    tl.setCustomPivotX(dto.getCustomPivotX());
                    tl.setCustomPivotY(dto.getCustomPivotY());
                } else if (node instanceof GroupLayer) {
                    GroupLayer gl = (GroupLayer) node;
                    gl.setInternalScaleX(safeScale(dto.getScaleX()));
                    gl.setInternalScaleY(safeScale(dto.getScaleY()));
                    gl.setInternalRotation(dto.getRotation());
                    gl.setInternalShearX(dto.getShearX());
                    gl.setInternalShearY(dto.getShearY());
                    gl.setCustomPivotX(dto.getCustomPivotX());
                    gl.setCustomPivotY(dto.getCustomPivotY());
                } else if (node instanceof GroupLayerV2) {
                    GroupLayerV2 gv2 = (GroupLayerV2) node;
                    gv2.setInternalScaleX(safeScale(dto.getScaleX()));
                    gv2.setInternalScaleY(safeScale(dto.getScaleY()));
                    gv2.setInternalRotation(dto.getRotation());
                    gv2.setInternalShearX(dto.getShearX());
                    gv2.setInternalShearY(dto.getShearY());
                    gv2.setCustomPivotX(dto.getCustomPivotX());
                    gv2.setCustomPivotY(dto.getCustomPivotY());
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
            if (s.getShapeType() == ShapeType.CUSTOM_PATH) {
                if (s.getSvgContent() != null) sl.setSvgPathData(s.getSvgContent());
                if (s.getBezierNodes() != null && !s.getBezierNodes().isEmpty()) {
                    java.util.List<org.example.model.BezierNode> restoredNodes = new java.util.ArrayList<>();
                    for (org.example.dto.save.BezierNodeDTO bnDto : s.getBezierNodes()) {
                        org.example.model.BezierNode bn = new org.example.model.BezierNode(
                            new javafx.geometry.Point2D(bnDto.getAnchorX(), bnDto.getAnchorY()),
                            bnDto.isHasControl1() ? new javafx.geometry.Point2D(bnDto.getControl1X(), bnDto.getControl1Y()) : null,
                            bnDto.isHasControl2() ? new javafx.geometry.Point2D(bnDto.getControl2X(), bnDto.getControl2Y()) : null
                        );
                        bn.isMoveTo = bnDto.isMoveTo();
                        if (bnDto.getType() != null) {
                            bn.type = org.example.model.BezierNode.NodeType.valueOf(bnDto.getType());
                        }
                        if (bnDto.getSegmentType() != null) {
                            bn.segmentType = org.example.model.BezierNode.SegmentType.valueOf(bnDto.getSegmentType());
                        }
                        restoredNodes.add(bn);
                    }
                    sl.setBezierNodes(restoredNodes);
                }
            }
            sl.setArcWidth(s.getArcWidth());
            sl.setArcHeight(s.getArcHeight());
            if (s.isGradientTransparency()) {
                sl.setTransparency(true, s.getTransparencyAngle(), s.getTransparencyStartAlpha(), s.getTransparencyEndAlpha());
                sl.setTransparencyBalance(s.getTransparencyBalance());
            }
            if (dto.getScaleX() != 1.0) sl.setInternalScaleX(safeScale(dto.getScaleX()));
            if (dto.getScaleY() != 1.0) sl.setInternalScaleY(safeScale(dto.getScaleY()));
            if (dto.getShearX() != 0) sl.setInternalShearX(dto.getShearX());
            if (dto.getShearY() != 0) sl.setInternalShearY(dto.getShearY());
            if (dto.getCustomPivotX() != -1) sl.setCustomPivotX(dto.getCustomPivotX());
            if (dto.getCustomPivotY() != -1) sl.setCustomPivotY(dto.getCustomPivotY());
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
                    if (dto.getShearX() != 0) il.setInternalShearX(dto.getShearX());
                    if (dto.getShearY() != 0) il.setInternalShearY(dto.getShearY());
                    if (dto.getCustomPivotX() != -1) il.setCustomPivotX(dto.getCustomPivotX());
                    if (dto.getCustomPivotY() != -1) il.setCustomPivotY(dto.getCustomPivotY());
                    if (dto.getRotation() != 0) il.setInternalRotation(dto.getRotation());
                    if (dto.getWidth() > 1 && dto.getHeight() > 1) il.resize(dto.getWidth(), dto.getHeight());
                    if (dto.isLocked()) il.setUserLocked(true);
                    if (i.getBadgeType() != null) {
                        try { il.setBadgeType(org.example.model.TipoEscudo.valueOfTech(i.getBadgeType())); } catch (Exception ex) {}
                    }
                    return il;
                }
            }
        } else if (dto instanceof GroupDTO) {
            GroupDTO g = (GroupDTO) dto;
            org.example.component.GroupLayerV2 gl = new org.example.component.GroupLayerV2();
            gl.setVisualizer(visualizer);
            if (g.getActiveZone() != null) gl.setActiveZone(g.getActiveZone());
            if (dto.getScaleX() != 1.0) gl.setInternalScaleX(safeScale(dto.getScaleX()));
            if (dto.getScaleY() != 1.0) gl.setInternalScaleY(safeScale(dto.getScaleY()));
            if (dto.getShearX() != 0) gl.setInternalShearX(dto.getShearX());
            if (dto.getShearY() != 0) gl.setInternalShearY(dto.getShearY());
            if (dto.getCustomPivotX() != -1) gl.setCustomPivotX(dto.getCustomPivotX());
            if (dto.getCustomPivotY() != -1) gl.setCustomPivotY(dto.getCustomPivotY());
            if (dto.getRotation() != 0) gl.setInternalRotation(dto.getRotation());
            if (g.getChildren() != null) {
                for (LayerDTO childDto : g.getChildren()) {
                    try {
                        Node childNode = createNodeFromDTO(childDto, visualizer);
                        if (childNode != null) {
                            childNode.setTranslateX(childDto.getX());
                            childNode.setTranslateY(childDto.getY());
                            gl.addChild(childNode);
                        }
                    } catch (Exception ex) {
                        System.err.println("Error restoring group child node: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }
                gl.recalculateBounds();
            }
            if (dto.isLocked()) gl.setUserLocked(true);
            return gl;
        } else if (dto instanceof TextDTO) {
            TextDTO t = (TextDTO) dto;
            String family = t.getFontFamily() != null ? t.getFontFamily() : "Arial";
            javafx.scene.text.Font font = javafx.scene.text.Font.font(family, t.getFontSize());
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
            
            // Explicitly set bold and italic properties to sync internal TextLayer flags
            tl.setBold(t.isBold());
            tl.setItalic(t.isItalic());

            javafx.scene.text.FontWeight weight = t.isBold() ? javafx.scene.text.FontWeight.BOLD : javafx.scene.text.FontWeight.NORMAL;
            javafx.scene.text.FontPosture posture = t.isItalic() ? javafx.scene.text.FontPosture.ITALIC : javafx.scene.text.FontPosture.REGULAR;
            tl.setFont(javafx.scene.text.Font.font(family, weight, posture, t.getFontSize()));
            tl.setTextSize(t.getCurrentWidth(), t.getCurrentHeight());
            
            // Restore Contour
            if (t.getContourSteps() > 0) {
                Color cContour = t.getContourColor() != null ? Color.web(t.getContourColor()) : Color.TRANSPARENT;
                tl.applyContour(t.getContourSteps(), t.getContourDistance(), cContour);
            }

            if (t.getActiveZone() != null) tl.setActiveZone(t.getActiveZone());
            if (t.getScaleX() != 1.0) tl.setInternalScaleX(safeScale(t.getScaleX()));
            if (t.getScaleY() != 1.0) tl.setInternalScaleY(safeScale(t.getScaleY()));
            if (t.getShearX() != 0) tl.setInternalShearX(t.getShearX());
            if (t.getShearY() != 0) tl.setInternalShearY(t.getShearY());
            if (t.getCustomPivotX() != -1) tl.setCustomPivotX(t.getCustomPivotX());
            if (t.getCustomPivotY() != -1) tl.setCustomPivotY(t.getCustomPivotY());
            if (t.getRotation() != 0) tl.setInternalRotation(t.getRotation());
            if (t.isLocked()) tl.setUserLocked(true);
            return tl;
        }
        return null;
    }

    private static final java.util.Map<String, javafx.scene.image.Image> imageCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .weakValues()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .<String, javafx.scene.image.Image>build()
            .asMap();

    private static javafx.scene.image.Image decodeImage(String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        javafx.scene.image.Image cached = imageCache.get(base64);
        if (cached != null) return cached;
        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64);
            javafx.scene.image.Image img = new javafx.scene.image.Image(new java.io.ByteArrayInputStream(imageBytes));
            imageCache.put(base64, img);
            return img;
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
        state.setHasShirtLinea(config.isHasShirtLinea());
        state.setHasPadding(config.isHasPadding());
        state.setHasShorts(config.isHasShorts());
        state.setHasShortsStripe(config.isHasShortsStripe());
        state.setHasShortsLinea(config.isHasShortsLinea());
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
