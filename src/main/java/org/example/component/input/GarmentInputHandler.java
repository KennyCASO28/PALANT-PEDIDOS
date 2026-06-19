package org.example.component.input;

import javafx.scene.Node;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;
import org.example.component.ImageLayer;
import org.example.component.PrendaVisualizer;
import org.example.component.ShapeLayer;
import org.example.component.TextLayer;
import org.example.component.helper.PrendaEditModeManager;
import org.example.component.helper.PrendaLayerFactory;
import org.example.component.UserLayerManager;
import org.example.utils.UIFactory;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import javafx.geometry.Point2D;
import javafx.scene.control.Tooltip;
import javafx.scene.Cursor;
import javafx.scene.input.MouseButton;
import javafx.event.ActionEvent;
import javafx.collections.ObservableList;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles global input events for the PrendaVisualizer
 */
public class GarmentInputHandler {

    private final PrendaVisualizer visualizer;
    private final UserLayerManager layerManager;
    private final PrendaLayerFactory layerFactory;

    private ContextMenu activeGlobalMenu;
    private Runnable activeToolCanceler;

    public GarmentInputHandler(PrendaVisualizer visualizer,
            UserLayerManager layerManager,
            PrendaEditModeManager editManager,
            PrendaLayerFactory layerFactory) {
        this.visualizer = visualizer;
        this.layerManager = layerManager;
        this.layerFactory = layerFactory;

        setupHandlers();
    }

    private void setupHandlers() {
        setupContextMenu();
        setupSelectionFilter();
        setupBackgroundClick();
        setupKeyboardShortcuts();
        setupToolHandlers(); // New Tool Handlers
    }

    private void setupToolHandlers() {
        visualizer.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.isPrimaryButtonDown()) {
                // --- CROP COMMIT LOGIC ---
                Node selected = layerManager.getSelectedNode();
                if (selected instanceof ImageLayer) {
                    ImageLayer il = (ImageLayer) selected;
                    if (il.isCropMode() && !il.isPartOfLayer((Node) e.getTarget())) {
                        il.commitIfInCropMode();
                    }
                }

                org.example.controller.uicomponent.ShapeManagerController smc = visualizer.getShapeManagerController();
                if (smc == null)
                    return;
            }
        });
    }

    /**
     * Checks whether the currently focused node is a real text-input inside the
     * side-panel (NOT related to the canvas/visualizer). Returns true only when
     * the user is actively typing in a sidebar field, stopping canvas shortcuts
     * from interfering with normal text editing.
     *
     * The key insight: the canvas visualizer always grabs focus on click, so if
     * a TextInputControl has focus it is the side panel. We still allow Delete
     * for canvas objects if the selected node is a TextLayer on the canvas — the
     * user clicked on the canvas text, the visualizer captured focus, and we
     * should still delete it.
     */
    private boolean isSidePanelTextFieldFocused() {
        if (visualizer.getScene() == null) return false;
        javafx.scene.Node focusOwner = visualizer.getScene().getFocusOwner();
        if (!(focusOwner instanceof javafx.scene.control.TextInputControl)) return false;

        // Walk up the scene graph — if we reach the visualizer, the "TextField" is
        // actually an overlay inside the canvas (no such thing currently, but
        // future-proof). If we never reach the visualizer, it is a side-panel field.
        // Walk up the scene graph — if we reach the visualizer, the "TextField" is
        // actually an overlay inside the canvas (no such thing currently, but
        // future-proof). If we never reach the visualizer, it is a side-panel field.
        javafx.scene.Node current = focusOwner;
        while (current != null) {
            if (current == visualizer) return false; // inside canvas — not a side-panel
            current = current.getParent();
        }
        return true; // outside the visualizer — real side-panel field
    }

    private final long[] lastUndoTime = {0};

    private void setupKeyboardShortcuts() {
        javafx.event.EventHandler<javafx.scene.input.KeyEvent> keyHandler = e -> {
            boolean isControlDown = e.isControlDown() || e.isShortcutDown();
            boolean isAltDown    = e.isAltDown();
            boolean isShiftDown  = e.isShiftDown();

            // True only when user actively types in a SIDE-PANEL text field.
            // Canvas TextLayers do NOT count — pressing Delete on the canvas should
            // still delete the selected layer.
            boolean inSidePanel = isSidePanelTextFieldFocused();

            // ----------------------------------------------------------------
            // 1. UNDO / REDO  (always highest priority, never blocked by canvas)
            // ----------------------------------------------------------------
            if (isControlDown && !isAltDown) {
                if (e.getCode() == javafx.scene.input.KeyCode.Z) {
                    if (inSidePanel) return;
                    
                    long now = System.currentTimeMillis();
                    if (now - lastUndoTime[0] < 250) {
                        e.consume();
                        return; // Ignore fast auto-repeats to prevent disappearing objects
                    }
                    lastUndoTime[0] = now;
                    
                    if (visualizer.getHistoryManager() != null) {
                        if (isShiftDown) visualizer.getHistoryManager().redo();
                        else            visualizer.getHistoryManager().undo();
                        e.consume();
                        return;
                    }
                }
                if (e.getCode() == javafx.scene.input.KeyCode.Y && !inSidePanel) {
                    if (visualizer.getHistoryManager() != null) {
                        visualizer.getHistoryManager().redo();
                        e.consume();
                        return;
                    }
                }
            }

            // ----------------------------------------------------------------
            // 2. DESIGN ACTIONS — only when NOT typing in sidebar
            // ----------------------------------------------------------------

            // Alt + B → Lock selected objects (figures, texts, images, objects)
            if (isAltDown && e.getCode() == javafx.scene.input.KeyCode.B) {
                visualizer.setUserLockedOnSelected(true);
                e.consume();
                return;
            }

            // Alt + D → Unlock selected objects
            if (isAltDown && e.getCode() == javafx.scene.input.KeyCode.D) {
                visualizer.setUserLockedOnSelected(false);
                e.consume();
                return;
            }

            // -------- UNIVERSAL DESIGN SHORTCUTS (C, E, X, V, D, G) --------
            boolean isC = (e.getCode() == javafx.scene.input.KeyCode.C);
            boolean isE = (e.getCode() == javafx.scene.input.KeyCode.E);
            
            if (isC || isE) {
                int selCount = layerManager.getSelectedNodes().size();
                javafx.scene.Node activeAnchor = visualizer.getActiveReferenceAnchor();
                boolean hasSelection = (selCount > 0);
                boolean multiSelection = (selCount > 1 || (selCount == 1 && activeAnchor != null));
                String type = isC ? "C" : "E";

                // CRITICAL FIX: Alignment logic for C/E
                boolean shouldAlign = false;
                if (hasSelection) {
                    if (isAltDown) shouldAlign = true;
                    // If anchor is active, BOTH C and Ctrl+C should align (as requested)
                    // But we MUST check if the anchor is actually there.
                    else if (activeAnchor != null) shouldAlign = true; 
                    else if (isE && isControlDown) shouldAlign = true;
                    else if (!isControlDown && multiSelection) shouldAlign = true;
                }

                if (shouldAlign) {
                    visualizer.alignSelected(type);
                    visualizer.requestFocus(); 
                    e.consume();
                    return;
                }
                
                // Block typing C/E in side panel IF we have a multi-selection
                if (inSidePanel && !isControlDown && !isAltDown && multiSelection) {
                    e.consume();
                    return;
                }
            }

            if (isControlDown) {
                int selCount = layerManager.getSelectedNodes().size();
                boolean hasSelection = selCount > 0;

                // Ctrl + G  →  Group
                // Ctrl + U  →  Ungroup
                if (e.getCode() == javafx.scene.input.KeyCode.G && !isShiftDown) {
                    if (selCount > 1) {
                        layerManager.groupSelected();
                    }
                    visualizer.requestFocus();
                    e.consume();
                    return;
                }
                if (e.getCode() == javafx.scene.input.KeyCode.U || (e.getCode() == javafx.scene.input.KeyCode.G && isShiftDown)) {
                    Node sel = layerManager.getSelectedNode();
                    if (sel != null) layerManager.ungroup(sel);
                    visualizer.requestFocus();
                    e.consume();
                    return;
                }

                // Ctrl + D → Duplicate
                if (e.getCode() == javafx.scene.input.KeyCode.D && hasSelection) {
                    visualizer.copySelectedLayer();
                    visualizer.pasteLayer();
                    visualizer.requestFocus();
                    e.consume();
                    return;
                }

                // Ctrl + V → Paste (Universal)
                if (e.getCode() == javafx.scene.input.KeyCode.V) {
                    visualizer.pasteLayer();
                    visualizer.requestFocus();
                    e.consume();
                    return;
                }

                // Ctrl + X → Cut Layer (Universal)
                if (e.getCode() == javafx.scene.input.KeyCode.X && hasSelection) {
                    visualizer.cutSelectedLayer();
                    visualizer.requestFocus();
                    e.consume();
                    return;
                }
                
                // Ctrl + C → Copy Layer (Universal)
                if (isC && hasSelection) {
                    visualizer.copySelectedLayer();
                    visualizer.requestFocus();
                    e.consume();
                    return;
                }

                // I will remove the background lock shortcut from Ctrl+B to prevent accidental unlocks.
            }

            // ----------------------------------------------------------------
            // 3. DELETE / BACKSPACE
            // ----------------------------------------------------------------
            if (e.getCode() == javafx.scene.input.KeyCode.DELETE) {
                if (!inSidePanel && layerManager.getSelectedNodes().size() > 0) {
                    boolean isNodeEditing = false;
                    for (javafx.scene.Node node : layerManager.getSelectedNodes()) {
                        if (node instanceof org.example.component.ShapeLayer && ((org.example.component.ShapeLayer) node).isNodeEditing()) {
                            isNodeEditing = true;
                            break;
                        }
                    }
                    if (isNodeEditing) return;
                    visualizer.deleteSelectedLayer();
                    visualizer.requestFocus();
                    e.consume();
                    return;
                }
            }

            if (e.getCode() == javafx.scene.input.KeyCode.BACK_SPACE) {
                // Only delete layer with backspace if NOT in a text field
                if (!inSidePanel && layerManager.getSelectedNodes().size() > 0) {
                    boolean isNodeEditing = false;
                    for (javafx.scene.Node node : layerManager.getSelectedNodes()) {
                        if (node instanceof org.example.component.ShapeLayer && ((org.example.component.ShapeLayer) node).isNodeEditing()) {
                            isNodeEditing = true;
                            break;
                        }
                    }
                    if (isNodeEditing) return;
                    visualizer.deleteSelectedLayer();
                    visualizer.requestFocus();
                    e.consume();
                    return;
                }
            }
        };

        // Attach to Scene to ensure global hotkeys work regardless of focus.
        // We MUST remove the old handler if the scene changes (e.g. visualizer swapped out)
        // to prevent duplicate firing of Ctrl+Z.
        if (visualizer.getScene() != null) {
            visualizer.getScene().addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, keyHandler);
        }
        visualizer.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null) {
                oldScene.removeEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, keyHandler);
            }
            if (newScene != null) {
                newScene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, keyHandler);
            }
        });
    }

    private void setupSelectionFilter() {
        // Block interaction with PowerClipped items if not in edit mode
        javafx.event.EventHandler<MouseEvent> blockLocked = e -> {

            // CRITICAL FIX: If panning is enabled, let mouse events through to ViewportController
            if (visualizer.getViewportController() != null && visualizer.getViewportController().isPanningEnabled()) {
                return; // Don't consume - let ViewportController handle panning
            }

            // --- 0. SMART SELECTION LOGIC (Ctrl to Select Locked) ---
            if (e.getEventType() == MouseEvent.MOUSE_PRESSED && e.isPrimaryButtonDown()) {
                // ENSURE: If a tool like Eyedropper, Pencil, or Picker is active, 
                // we DO NOT want to change the selection. The tool itself should handle it.
                org.example.component.helper.ShapeInteractionHelper sh = visualizer.getShapeHelper();
                if (sh.isEyedropperActive() || sh.isCreatingShape() || sh.isPowerClipPickerActive()) {
                    return;
                }
                
                org.example.controller.uicomponent.ShapeManagerController smc = visualizer.getShapeManagerController();
                if (smc != null && (smc.isBrushActive() || smc.isEraserActive())) {
                    return;
                }

                // Ensure Visualizer has focus so Key Modifiers (Ctrl) are read correctly
                visualizer.requestFocus();

                // --- NODE EDITING PROTECTION ---
                // If we are clicking on a Bezier Node or Handle, skip selection logic
                // to allow the ShapeInteractionHelper to handle it.
                if (visualizer.getShapeHelper().isNodeEditHandle((Node) e.getTarget())) {
                    return;
                }
                
                // NEW: If clicking on a reference point or hotspot, skip selection logic
                Node t = (Node) e.getTarget();
                while (t != null && t != visualizer) {
                    if (t == visualizer.getHotspotLayer() || t == visualizer.getReferenceLayer()) {
                        return; 
                    }
                    t = t.getParent();
                }

                // 1. Find all candidates under cursor (Top-Down order)
                List<Node> candidates = new ArrayList<>();

                // A. Check User Layers (Shapes, Images) - Nestes in 'userLayerGroup'
                ObservableList<Node> userLayers = layerManager.getLayerGroup().getChildren();
                for (int i = userLayers.size() - 1; i >= 0; i--) {
                    Node child = userLayers.get(i);
                    if (child.isVisible() && !child.isMouseTransparent()) {
                        Point2D local = child.sceneToLocal(e.getSceneX(), e.getSceneY());
                        if (local != null && child.contains(local)) {
                            candidates.add(child);
                        }
                    }
                }

                // B. Check direct children of ContentGroup (e.g. TextLayers / Numbers)
                ObservableList<Node> directChildren = visualizer.getContentGroup().getChildren();
                for (int i = directChildren.size() - 1; i >= 0; i--) {
                    Node child = directChildren.get(i);
                    if (child.isVisible() && !child.isMouseTransparent()) {
                        // Skip userLayerGroup as we already checked it
                        if (child == layerManager.getLayerGroup())
                            continue;

                        if (child instanceof TextLayer) {
                            Point2D local = child.sceneToLocal(e.getSceneX(), e.getSceneY());
                            if (local != null && child.contains(local))
                                candidates.add(child);
                        } else if (child instanceof javafx.scene.Group) {
                            Point2D local = child.sceneToLocal(e.getSceneX(), e.getSceneY());
                            if (local != null && child.contains(local)) {
                                // NEW: If it's a standard number root (Digits/Name), allow selecting it.
                                if (visualizer.isNumberRoot(child)) {
                                    candidates.add(child);
                                }
                            }
                        }
                    }
                }

                // C. Check Active Container Content (if Editing)
                if (visualizer.getPowerClipManager().isEditing()) {
                    String zone = visualizer.getPowerClipManager().getCurrentEditingZone();
                    org.example.component.helper.SmartZoneContainer container = visualizer.getPowerClipManager()
                            .getContainer(zone);

                    if (container != null) {
                        ObservableList<Node> containerChildren = container.getContentGroup().getChildren();
                        // Scan top-down (reverse) and prepend to candidates to prioritize Inner Items
                        List<Node> innerCandidates = new ArrayList<>();
                        for (int i = containerChildren.size() - 1; i >= 0; i--) {
                            Node child = containerChildren.get(i);
                            if (child.isVisible() && !child.isMouseTransparent()) {
                                Point2D local = child.sceneToLocal(e.getSceneX(), e.getSceneY());
                                if (local != null && child.contains(local)) {
                                    innerCandidates.add(child);
                                }
                            }
                        }

                        // ISOLATION FIX: If editing a PowerClip, ONLY allow selection of items inside
                        // that container.
                        candidates.clear();
                        candidates.addAll(innerCandidates);
                    }
                }

                // 2. Filter Candidates based on Lock State & Modifier
                Node targetToSelect = null;
                boolean hitLockedItemsOnly = false;

                for (Node n : candidates) {
                    boolean isLocked = false;
                    if (n instanceof ShapeLayer)
                        isLocked = ((ShapeLayer) n).isUserLocked();
                    else if (n instanceof ImageLayer)
                        isLocked = ((ImageLayer) n).isUserLocked();
                    else if (n instanceof TextLayer)
                        isLocked = ((TextLayer) n).isUserLocked();
                    else if (n instanceof org.example.component.GroupLayerV2)
                        isLocked = ((org.example.component.GroupLayerV2) n).isUserLocked();
                    else if (n instanceof org.example.component.GroupLayer)
                        isLocked = ((org.example.component.GroupLayer) n).isUserLocked();

                    // Rule: Locked items are "transparent" to clicks UNLESS Ctrl/Cmd is down
                    boolean isControl = e.isControlDown() || e.isShortcutDown();

                    if (!isLocked || isControl) {
                        targetToSelect = n;
                        break; // Found the top-most valid item
                    } else {
                        hitLockedItemsOnly = true; // We skipped this because it was locked
                    }
                }

                if (targetToSelect != null) {
                    // We found a valid target (either Unlocked, or Locked+Ctrl)
                    // Select it explicitly

                    boolean isControl = e.isControlDown() || e.isShortcutDown();

                    if (isControl) {
                        layerManager.toggleSelection(targetToSelect);
                    } else {
                        // CANCEL ACTIVE TOOLS ON SELECTION CHANGE
                        if (layerManager.getSelectedNode() != targetToSelect && activeToolCanceler != null) {
                            activeToolCanceler.run();
                        }
                        
                        layerManager.selectNode(targetToSelect);
                    }

                    // CRITICAL FIX: Only consume if the item is LOCKED.
                    // If it is UNLOCKED, we MUST let the event propagate so the
                    // Layer's own handlers (Drag, Crop, Resize) can function.
                    boolean isTargetLocked = false;
                    if (targetToSelect instanceof ShapeLayer)
                        isTargetLocked = ((ShapeLayer) targetToSelect).isUserLocked();
                    else if (targetToSelect instanceof ImageLayer)
                        isTargetLocked = ((ImageLayer) targetToSelect).isUserLocked();
                    else if (targetToSelect instanceof TextLayer)
                        isTargetLocked = ((TextLayer) targetToSelect).isUserLocked();
                    else if (targetToSelect instanceof org.example.component.GroupLayerV2)
                        isTargetLocked = ((org.example.component.GroupLayerV2) targetToSelect).isUserLocked();
                    else if (targetToSelect instanceof org.example.component.GroupLayer)
                        isTargetLocked = ((org.example.component.GroupLayer) targetToSelect).isUserLocked();

                    if (isTargetLocked && !e.isShiftDown()) {
                        e.consume(); // Prevent moving/interaction on Locked items
                    }

                    // Return without consuming for Unlocked items -> Enables Drag & Crop
                    return;
                } else if (hitLockedItemsOnly) {
                    // We clicked on Locked items, but didn't hold Ctrl.
                    // Treat as "Click Through" -> If no other items were behind, it's a background
                    // click.
                    // So we Deselect All.
                    layerManager.clearSelection();
                    visualizer.deselectAllNames();
                    if (!e.isShiftDown()) {
                        e.consume(); // Consume so we don't trigger other weird behaviors
                    }
                    return;
                }
            }
            // -----------------------------------------------------

            // Only hide menu on PRIMARY click to avoid hiding it immediately when trying to
            // show a NEW one
            if (e.getEventType() == MouseEvent.MOUSE_PRESSED && e.isPrimaryButtonDown() && activeGlobalMenu != null) {
                activeGlobalMenu.hide();
                activeGlobalMenu = null;
            }

            if (e.isPrimaryButtonDown()) {
                Node n = (Node) e.getTarget();
                Node layerNode = null;
                String zoneOnLayer = null;
                Node temp = n;
                while (temp != null && temp != visualizer) {
                    if (temp instanceof ShapeLayer) {
                        layerNode = temp;
                        zoneOnLayer = ((ShapeLayer) temp).getActiveZone();
                        break;
                    } else if (temp instanceof ImageLayer) {
                        layerNode = temp;
                        zoneOnLayer = ((ImageLayer) temp).getActiveZone();
                        break;
                    }
                    temp = temp.getParent();
                }

                if (layerNode != null && zoneOnLayer != null) {
                    // Check if we are currently editing ANY zone
                    boolean isGlobalEditing = visualizer.getPowerClipManager().isEditing();

                    if (isGlobalEditing) {
                        // Check if we are editing THIS specific zone
                        boolean isEditingThis = zoneOnLayer
                                .equals(visualizer.getPowerClipManager().getCurrentEditingZone());

                        if (!isEditingThis) {
                            // LOCKED: Eat the event completely when editing DIFFERENT zone
                            e.consume();

                            // If it was a click, ensure we deselect everything to avoid confusion
                            if (e.getEventType() == MouseEvent.MOUSE_PRESSED) {
                                layerManager.clearSelection();
                                visualizer.deselectAllNames();
                            }
                        }
                    }
                }
            }
        };

        visualizer.addEventFilter(MouseEvent.MOUSE_PRESSED, blockLocked);
        visualizer.addEventFilter(MouseEvent.MOUSE_RELEASED, blockLocked);

        // Strict Isolation Filter for Edit Mode (Blocks EVERYTHING outside active zone)
        javafx.event.EventHandler<MouseEvent> isolationFilter = e -> {
            if (visualizer.getPowerClipManager().isEditing()) {
                if (e.isPrimaryButtonDown()) {
                    Node target = (Node) e.getTarget();
                    String activeZone = visualizer.getPowerClipManager().getCurrentEditingZone();

                    // 1. Allow Overlay (UI Handles, Buttons, etc.)
                    Node temp = target;
                    boolean isOverlay = false;
                    while (temp != null) {
                        // Allow Overlay Group (Resize Handles)
                        if (temp == visualizer.getOverlayManager().getOverlayGroup()) {
                            isOverlay = true;
                            break;
                        }
                        // Allow Finish Button (Check by Reference OR ID)
                        if ((visualizer.getUiController() != null && temp == visualizer.getUiController().getFinishEditButton())
                                || "btnFinishEditOverlay".equals(temp.getId())) {
                            isOverlay = true;
                            break;
                        }
                        // Allow Lock BG Button
                        if (temp == visualizer.getBtnLockBg()) {
                            isOverlay = true;
                            break;
                        }
                        // Allow Reference Points and Hotspots
                        if (temp == visualizer.getHotspotLayer() || temp == visualizer.getReferenceLayer()) {
                            isOverlay = true;
                            break;
                        }

                        temp = temp.getParent();
                    }
                    if (isOverlay)
                        return; // Allow UI interaction

                    // 2. Allow descendants of the Active Container
                    boolean isDescendant = false;
                    if (activeZone != null) {
                        // We need access to the container.
                        // Check via hierarchy: simpler than getting container object if we don't have
                        // public getter.
                        // But we know PowerClipManager puts content in a container group.
                        // Let's rely on checking if the node is effectively inside the PowerClip
                        // structure.
                        // Or simpler: Use the PowerClipManager to get the container node.
                        org.example.component.helper.SmartZoneContainer container = visualizer.getPowerClipManager()
                                .getContainer(activeZone);

                        if (container != null) {
                            temp = target;
                            while (temp != null) {
                                if (temp == container) {
                                    isDescendant = true;
                                    break;
                                }
                                temp = temp.getParent();
                            }
                        }
                    }

                    if (!isDescendant) {
                        // NEW FIX: Allow clicks on the "Background" of the active zone (e.g. Shirt
                        // Body)
                        // This allows the event to bubble up to setupBackgroundClick for deselection.
                        boolean isZoneBackground = false;
                        String detected = visualizer.getShapeHelper().detectZone(e.getSceneX(), e.getSceneY());
                        if (activeZone != null && activeZone.equals(detected)) {
                            isZoneBackground = true;
                        }

                        // CRITICAL: Ensure we didn't just click a floating layer that happens to be
                        // OVER the zone.
                        // If the target is a Layer, it is NOT the background.
                        Node nodeCheck = target;
                        while (nodeCheck != null && nodeCheck != visualizer) {
                            if (nodeCheck instanceof org.example.component.ShapeLayer ||
                                    nodeCheck instanceof org.example.component.ImageLayer ||
                                    nodeCheck instanceof org.example.component.TextLayer ||
                                    nodeCheck instanceof org.example.component.GroupLayer ||
                                    nodeCheck instanceof org.example.component.GroupLayerV2) {
                                isZoneBackground = false;
                                break;
                            }
                            nodeCheck = nodeCheck.getParent();
                        }

                        // USER REQUEST: Allow clicking on EMPTY SPACE (Void) to Deselect, even if
                        // outside zone
                        // If we clicked on NOTHING (no layer, not an overlay), we should allow it to
                        // Deselect.
                        // The 'detected' zone being null means we clicked outside the garment entirely.
                        boolean isVoidClick = (detected == null) && !isOverlay; // If no zone detected, it's void

                        if (!isZoneBackground && !isVoidClick) {
                            // System.out.println("DEBUG: Blocking event on " + target + " (Active Zone: " +
                            // activeZone + ")");
                            e.consume(); // BLOCK everything else ONLY if it's not the zone background OR the void
                        } else if (isVoidClick) {
                            // Explicitly clear selection if clicking void
                            layerManager.clearSelection();
                            visualizer.deselectAllNames();
                            // Optional: e.consume() if we want to stop other processing?
                            // Usually background click handler does this, but isolation filter might block
                            // it reaching there?
                            // Actually, isolation filter is on Visualizer, so it runs before Scene
                            // handlers?
                            // Let's just allow it to bubble if we want standard beahvior, OR force deselect
                            // here.
                            // Forcing here is safer since we are in a special mode.
                        }
                    }
                }
            }
        };
        visualizer.addEventFilter(MouseEvent.MOUSE_PRESSED, isolationFilter);
        visualizer.addEventFilter(MouseEvent.MOUSE_RELEASED, isolationFilter);
        visualizer.addEventFilter(MouseEvent.MOUSE_CLICKED, isolationFilter);

        setupKeyHandlers();
    }

    private void setupKeyHandlers() {
        // Alignment and Grouping moved to Global keyHandler for better focus management.
    }

    private void setupContextMenu() {
        // Use addEventFilter to capture clicks even if children consume them
        visualizer.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                // Logic to determine if we clicked inside a layer or garment zone
                Node n = (Node) e.getTarget();

                // FIX: If clicking a Node Handle, let the specific handle menu work (don't show
                // global/overlap)
                if (visualizer.getShapeHelper().isNodeEditHandle(n)) {
                    return;
                }

                // 1. Check for Layer
                Node layerNode = null;
                String zoneOnLayer = null;
                Node temp = n;
                while (temp != null && temp != visualizer) {
                    // PRIORITIZE GROUP SELECTION
                    if ("USER_GROUP".equals(temp.getId())) {
                        layerNode = temp;
                        // zoneOnLayer? Groups likely don't have activeZone property directly unless
                        // wrapper.
                        // We assume null or we might need to check children?
                        // For now, Group acts as floating object usually.
                        break;
                    }

                    if (temp instanceof ShapeLayer) {
                        layerNode = temp;
                        zoneOnLayer = ((ShapeLayer) temp).getActiveZone();
                        break;
                    } else if (temp instanceof ImageLayer) {
                        layerNode = temp;
                        zoneOnLayer = ((ImageLayer) temp).getActiveZone();
                        break;
                    } else if (temp instanceof TextLayer) {
                        layerNode = temp;
                        zoneOnLayer = ((TextLayer) temp).getActiveZone();
                        break;
                    }
                    temp = temp.getParent();
                }

                // --- LOCKED ITEM PROTECTION ---
                // If the item is LOCKED and NOT SELECTED, we ignore it for Right-Click.
                // This allows Right-Click to pass through to the background/zone.
                if (layerNode != null) {
                    boolean isLocked = false;
                    // Check generic lock
                    if (layerNode instanceof ShapeLayer)
                        isLocked = ((ShapeLayer) layerNode).isUserLocked();
                    else if (layerNode instanceof ImageLayer)
                        isLocked = ((ImageLayer) layerNode).isUserLocked();
                    else if (layerNode instanceof TextLayer)
                        isLocked = ((TextLayer) layerNode).isUserLocked();

                    boolean isSelected = (layerManager.getSelectedNode() == layerNode);

                    if (isLocked && !isSelected) {
                        // Ignore this layer for selection/reparenting logic!
                        layerNode = null;
                        // zoneOnLayer = null; // CRITICAL: PRESERVE this to keep context for
                        // handleGlobalContextMenu
                    }
                }
                // ------------------------------

                // 2. Detect Zone under Coordinates (Garment Body/Sleeves)
                String detectedZone = visualizer.getShapeHelper().detectZone(e.getSceneX(), e.getSceneY());
                String finalZone = (zoneOnLayer != null) ? zoneOnLayer : detectedZone; // Priority to layer's zone

                // 4. Input Routing
                // STRICT CHECK: Only show Layer Menu if it is ALREADY SELECTED.
                // This reinforces the "Ctrl+Click to Select Locked" workflow.
                boolean isSelected = (layerNode != null && layerManager.getSelectedNodes().contains(layerNode));
                int selCount = layerManager.getSelectedNodes().size();

                if (layerNode != null && isSelected) {
                    boolean isEditingThis = visualizer.getPowerClipManager().isEditing()
                            && finalZone != null
                            && finalZone.equals(visualizer.getPowerClipManager().getCurrentEditingZone());

                    if (isEditingThis) {
                        // In edit mode: Always show full layer menu
                        handleLayerContextMenu(e, layerNode);
                    } else if (selCount > 1) {
                        // UNIFIED MENU: If multiple items selected, show Global Menu
                        // (Group/Insert/Align)
                        // This prevents showing the single-object menu which lacks these features.
                        handleGlobalContextMenu(e, zoneOnLayer);
                    } else if (zoneOnLayer != null) {
                        // Clipped layer NOT in edit mode -> Show global menu with "EDITAR" as fallback
                        // OR if single selection of clipped item, maybe allow context menu?
                        // Current logic: force global if not editing.
                        handleGlobalContextMenu(e, zoneOnLayer);
                    } else {
                        // Floating layer (Single Selection)
                        handleLayerContextMenu(e, layerNode);
                    }
                } else {
                    // Not selected? Treat as background click (Global Menu)
                    // USE finalZone (Layer Zone or Detected Zone) to ensure context for clipped
                    // items
                    handleGlobalContextMenu(e, finalZone);
                }
                e.consume();
            } else if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY && e.isShiftDown()) {
                if (visualizer.getViewportController() != null && visualizer.getViewportController().isPanningEnabled()) {
                    return; // Do not open edit mode if panning is enabled
                }
                // Prevent opening checks if already editing (User Request: "Don't open other
                // containers while editing one")
                if (visualizer.getPowerClipManager().isEditing()) {
                    e.consume();
                    return;
                }

                String zone = visualizer.getShapeHelper().detectZone(e.getSceneX(), e.getSceneY());
                if (zone != null) {
                    visualizer.getPowerClipManager().enterEditMode(zone);
                    e.consume();
                }
            }
        });
    }

    private void handleLayerContextMenu(MouseEvent e, Node layer) {
        if (activeGlobalMenu != null) {
            activeGlobalMenu.hide();
        }

        if (layer instanceof ShapeLayer) {
            activeGlobalMenu = org.example.component.helper.ShapeMenuHelper.showContextMenu((ShapeLayer) layer,
                    e.getScreenX(),
                    e.getScreenY());
        } else if (layer instanceof ImageLayer) {
            createImageContextMenu((ImageLayer) layer, e);
        } else if (layer instanceof TextLayer) {
            createTextContextMenu((TextLayer) layer, e);
        }
    }

    private void createImageContextMenu(ImageLayer layer, MouseEvent e) {
        ContextMenu menu = new ContextMenu();
        activeGlobalMenu = menu;

        boolean isClipped = (layer.getActiveZone() != null);
        boolean isEditingThis = visualizer.getPowerClipManager().isEditing()
                && layer.getActiveZone() != null
                && layer.getActiveZone().equals(visualizer.getPowerClipManager().getCurrentEditingZone());

        // --- 1. CLIPPED (OUTSIDE): Minimal Menu ---
        if (isClipped && !isEditingThis) {
            MenuItem editZone = new MenuItem(
                    "EDITAR CONTENIDO: " + formatZoneName(layer.getActiveZone()).toUpperCase());
            editZone.setGraphic(UIFactory.crearIcono("mdi2c-crop-free", 16, "#27ae60"));
            editZone.setOnAction(ev -> visualizer.getPowerClipManager().enterEditMode(layer.getActiveZone()));
            menu.getItems().add(editZone);

            if (ImageLayer.hasClipboard() || ShapeLayer.hasClipboard() || TextLayer.hasClipboard()) {
                menu.getItems().add(new javafx.scene.control.SeparatorMenuItem());
                addPasteOptionsToMenu(menu);
            }
            menu.show(visualizer, e.getScreenX(), e.getScreenY());
            return;
        }

        // --- 2. FLOATING / EDITING: Full Hierarchy ---
        // A. Header
        MenuItem header = new MenuItem("Imagen");
        header.setDisable(true);
        header.setStyle("-fx-font-weight: bold; -fx-opacity: 1.0; -fx-text-fill: #95a5a6; -fx-font-size: 11px;");
        menu.getItems().add(header);

        // B. PowerClip (Only for floating)
        if (!isClipped) {
            javafx.scene.control.Menu pcMenu = new javafx.scene.control.Menu("PowerClip (Insertar en...)");
            pcMenu.setGraphic(UIFactory.crearIcono("mdi2a-arrow-right-bold-box", 16, "#555"));
            java.util.List<String> available = visualizer.getAvailableZones();
            if (available.contains("PECHO")) {
                addNodeZoneMenuItem(pcMenu, layer, "PECHO", "mdi2t-tshirt-crew", "#2c3e50", true);
                addNodeZoneMenuItem(pcMenu, layer, "ESPALDA", "mdi2t-tshirt-crew-outline", "#2c3e50", false);
            }
            if (available.contains("MANGA_DELANTERA")) {
                pcMenu.getItems().add(new javafx.scene.control.SeparatorMenuItem());
                addNodeZoneMenuItem(pcMenu, layer, "MANGA_DELANTERA", "mdi2a-arm-flex", "#16a085", true);
                addNodeZoneMenuItem(pcMenu, layer, "MANGA_TRASERA", "mdi2a-arm-flex-outline", "#16a085", false);
            }
            if (available.contains("SHORT_FRONT")) {
                pcMenu.getItems().add(new javafx.scene.control.SeparatorMenuItem());
                addNodeZoneMenuItem(pcMenu, layer, "SHORT_FRONT", "mdi2v-view-column", "#d35400", true);
                addNodeZoneMenuItem(pcMenu, layer, "SHORT_BACK", "mdi2v-view-column-outline", "#d35400", false);
            }
            if (!pcMenu.getItems().isEmpty()) {
                menu.getItems().add(pcMenu);
            }
        }
        menu.getItems().add(new javafx.scene.control.SeparatorMenuItem());

        // C. Clipboard
        MenuItem copyItem = new MenuItem("Copiar");
        copyItem.setGraphic(UIFactory.crearIcono("mdi2c-content-copy", 16, "#555"));
        copyItem.setOnAction(ev -> layer.copyToClipboard());

        MenuItem cutItem = new MenuItem("Cortar");
        cutItem.setGraphic(UIFactory.crearIcono("mdi2c-content-cut", 16, "#555"));
        cutItem.setOnAction(ev -> layer.cutToClipboard());

        menu.getItems().addAll(copyItem, cutItem);

        // D. Full Tools
        menu.getItems().add(new javafx.scene.control.SeparatorMenuItem());

        // Order
        javafx.scene.control.Menu orderMenu = new javafx.scene.control.Menu("Orden / Capas");
        orderMenu.setGraphic(UIFactory.crearIcono("mdi2l-layers", 16, "#555"));
        MenuItem itemFront = new MenuItem("Traer al Frente");
        itemFront.setGraphic(UIFactory.crearIcono("mdi2c-chevron-double-up", 16, "#27ae60"));
        itemFront.setOnAction(ev -> visualizer.getLayerManager().bringToFront(layer));
        MenuItem itemBack = new MenuItem("Enviar al Fondo");
        itemBack.setGraphic(UIFactory.crearIcono("mdi2c-chevron-double-down", 16, "#c0392b"));
        itemBack.setOnAction(ev -> visualizer.getLayerManager().sendToBack(layer));
        orderMenu.getItems().addAll(itemFront, itemBack);
        menu.getItems().add(orderMenu);

        // Recorte
        javafx.scene.control.Menu cropMenu = new javafx.scene.control.Menu("Recorte");
        cropMenu.setGraphic(UIFactory.crearIcono("mdi2c-crop", 16, "#e67e22"));

        MenuItem cropModeItem = new MenuItem("Modo Recorte");
        cropModeItem.setGraphic(UIFactory.crearIcono("mdi2c-crop", 16, "#3498db"));
        cropModeItem.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
        cropModeItem.setOnAction(ev -> {
            if (activeToolCanceler != null) {
                activeToolCanceler.run();
                activeToolCanceler = null; 
            }
            layer.setCropMode(true);
        });

        MenuItem autoCropItem = new MenuItem("Auto-Recortar (Eliminar bordes)");
        autoCropItem.setGraphic(UIFactory.crearIcono("mdi2c-crop-free", 16, "#27ae60"));
        autoCropItem.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        autoCropItem.setOnAction(ev -> layer.trimWhitespace());

        MenuItem cropReset = new MenuItem("Resetear Recorte");
        cropReset.setGraphic(UIFactory.crearIcono("mdi2r-refresh", 16, "#c0392b"));
        cropReset.setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold;");
        cropReset.setOnAction(ev -> layer.resetCrop());

        cropMenu.getItems().addAll(cropModeItem, autoCropItem, new SeparatorMenuItem(), cropReset);
        menu.getItems().add(cropMenu);

        // Eliminación de Fondo
        javafx.scene.control.Menu bgMenu = new javafx.scene.control.Menu("Eliminación de Fondo");
        bgMenu.setGraphic(UIFactory.crearIcono("mdi2e-eraser", 16, "#3498db"));

        MenuItem removeWhite = new MenuItem("Quitar Fondo Blanco");
        removeWhite.setGraphic(UIFactory.crearIcono("mdi2i-image-filter-none", 16, "#555"));
        removeWhite.setOnAction(ev -> {
            if (activeToolCanceler != null) activeToolCanceler.run();
            if (layer.isProcessing()) return;
            
            // CAPTURE ON FX THREAD
            layer.editingService().prepareUndoState("Quitar Fondo");
            javafx.scene.image.Image snapshot = layer.snapshotCanvas();
            
            javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<Void>() {
                @Override protected Void call() throws Exception {
                    layer.removeWhiteBackground(snapshot);
                    return null;
                }
            };
            task.setOnRunning(t -> visualizer.setCursor(Cursor.WAIT));
            task.setOnSucceeded(t -> visualizer.setCursor(Cursor.DEFAULT));
            task.setOnFailed(t -> visualizer.setCursor(Cursor.DEFAULT));
            new Thread(task).start();
        });

        MenuItem removeBlack = new MenuItem("Quitar Fondo Negro");
        removeBlack.setGraphic(UIFactory.crearIcono("mdi2i-image-filter-none", 16, "#000"));
        removeBlack.setOnAction(ev -> {
            if (activeToolCanceler != null) activeToolCanceler.run();
            if (layer.isProcessing()) return;

            // CAPTURE ON FX THREAD
            layer.editingService().prepareUndoState("Quitar Fondo");
            javafx.scene.image.Image snapshot = layer.snapshotCanvas();

            javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<Void>() {
                @Override protected Void call() throws Exception {
                    layer.removeBlackBackground(snapshot);
                    return null;
                }
            };
            task.setOnRunning(t -> visualizer.setCursor(Cursor.WAIT));
            task.setOnSucceeded(t -> visualizer.setCursor(Cursor.DEFAULT));
            task.setOnFailed(t -> visualizer.setCursor(Cursor.DEFAULT));
            new Thread(task).start();
        });

        MenuItem removePoint = new MenuItem("Quitar fondo por zona (Click)");
        removePoint.setGraphic(UIFactory.crearIcono("mdi2t-target-variant", 16, "#e74c3c"));
        Tooltip.install(removePoint.getGraphic(), new Tooltip("Selecciona una zona del fondo para eliminarla"));
        removePoint.setOnAction(ev -> {
            if (activeToolCanceler != null) activeToolCanceler.run();
            layer.setRotationMode(false);
            layer.commitIfInCropMode();
            
            // ENTER PERSISTENT MAGIC WAND MODE
            layer.setCursor(Cursor.CROSSHAIR);
            
            final Object[] handlers = new Object[2];
            final javafx.event.EventHandler<MouseEvent> clickHandler = new javafx.event.EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent e) {
                    // CROP HANDLES SAFETY: If clicking a handle, don't consume/process wand
                    // CROP HANDLES SAFETY: Absolute priority to handles when in crop mode
                    if (layer.isCropMode()) {
                        Node target = (Node)e.getTarget();
                        while (target != null && target != layer) {
                            if (target.getStyleClass().contains("resize-handle")) {
                                return; // LET THE HANDLE PROCESS THE CLICK
                            }
                            target = target.getParent();
                        }
                    }

                    if (e.getButton() == MouseButton.PRIMARY) {
                        // ALLOW ROTATION MODE TOGGLE: If double click, don't consume/process wand
                        if (e.getClickCount() >= 2) return;

                        e.consume();
                        
                        // DEBOUNCE: Don't process if already working
                        if (layer.isProcessing()) return;

                        javafx.geometry.Point2D local = layer.getCanvas().sceneToLocal(e.getSceneX(), e.getSceneY());
                        int tx = (int)local.getX();
                        int ty = (int)local.getY();
                        double tol = layer.getMagicWandTolerance();

                        // CAPTURE ON FX THREAD
                        layer.editingService().prepareUndoState("Quitar Fondo");
                        javafx.scene.image.Image snapshot = layer.snapshotCanvas();

                        // ASYNC TASK
                        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<Void>() {
                            @Override protected Void call() throws Exception {
                                layer.removeBackgroundAt(snapshot, tx, ty, tol);
                                return null;
                            }
                        };
                        
                        task.setOnRunning(t -> visualizer.setCursor(Cursor.WAIT));
                        task.setOnSucceeded(t -> visualizer.setCursor(Cursor.CROSSHAIR));
                        task.setOnFailed(t -> {
                            visualizer.setCursor(Cursor.CROSSHAIR);
                            if (task.getException() != null) task.getException().printStackTrace();
                        });

                        new Thread(task).start();

                    } else if (e.getButton() == MouseButton.SECONDARY) {
                         e.consume();
                         finish();
                    }
                }
                
                private void finish() {
                    layer.setCursor(Cursor.DEFAULT);
                    layer.removeEventFilter(MouseEvent.MOUSE_PRESSED, (javafx.event.EventHandler<MouseEvent>)handlers[0]);
                    visualizer.removeEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, (javafx.event.EventHandler<javafx.scene.input.KeyEvent>)handlers[1]);
                    activeToolCanceler = null;
                }
            };


            final javafx.event.EventHandler<javafx.scene.input.KeyEvent> escHandler = k -> {
                if (k.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                    k.consume();
                    ((javafx.event.EventHandler<MouseEvent>)handlers[0]).handle(new MouseEvent(MouseEvent.MOUSE_PRESSED, 0, 0, 0, 0, MouseButton.SECONDARY, 0, false, false, false, false, false, false, false, false, false, false, null));
                }
            };
            
            handlers[0] = clickHandler;
            handlers[1] = escHandler;
            activeToolCanceler = () -> {
                layer.setCursor(Cursor.DEFAULT);
                layer.removeEventFilter(MouseEvent.MOUSE_PRESSED, clickHandler);
                visualizer.removeEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, escHandler);
                activeToolCanceler = null;
            };
            
            layer.addEventFilter(MouseEvent.MOUSE_PRESSED, clickHandler);
            visualizer.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, escHandler);
        });

        MenuItem manualEraser = new MenuItem("Limpiar con Borrador (Manual)");
        manualEraser.setGraphic(UIFactory.crearIcono("mdi2e-eraser-variant", 16, "#2980b9"));
        manualEraser.setOnAction(ev -> {
             if (activeToolCanceler != null) activeToolCanceler.run();
             if(visualizer.getShapeManagerController() != null) {
                 visualizer.getShapeManagerController().getEraserButton().setSelected(true);
                 visualizer.getShapeManagerController().getEraserButton().fireEvent(new ActionEvent());
             }
        });

        bgMenu.getItems().addAll(removeWhite, removeBlack, new SeparatorMenuItem(), removePoint, manualEraser);
        
        // --- ADICIÓN: Sliders de Ajuste ---
        bgMenu.getItems().add(new SeparatorMenuItem());
        bgMenu.getItems().add(createSliderAdjustmentItem("Tolerancia", layer.getMagicWandTolerance(), 0.05, 0.5, 
                val -> layer.setMagicWandTolerance(val)));
        
        if(visualizer.getShapeManagerController() != null) {
            bgMenu.getItems().add(createSliderAdjustmentItem("Tamaño Borrador", 
                    visualizer.getShapeManagerController().getEraserSize(), 1.0, 100.0, 
                    val -> visualizer.getShapeManagerController().setEraserSize(val)));
        }

        menu.getItems().add(bgMenu);

        // Ajustes (Brightness, Contrast, Saturation)
        javafx.scene.control.Menu adjMenu = new javafx.scene.control.Menu("Ajustes");
        adjMenu.setGraphic(UIFactory.crearIcono("mdi2m-movie-filter-outline", 16, "#9b59b6"));

        adjMenu.getItems()
                .add(createSliderAdjustmentItem("Brillo", layer.getBrightness(), -1.0, 1.0, layer::setBrightness));
        adjMenu.getItems()
                .add(createSliderAdjustmentItem("Contraste", layer.getContrast(), -1.0, 1.0, layer::setContrast));
        adjMenu.getItems()
                .add(createSliderAdjustmentItem("Saturación", layer.getSaturation(), -1.0, 1.0, layer::setSaturation));

        adjMenu.getItems().add(new SeparatorMenuItem());

        MenuItem resetAdj = new MenuItem("Resetear Ajustes");
        resetAdj.setOnAction(ev -> layer.resetAdjustments());

        MenuItem applyAdj = new MenuItem("Aplicar Ajustes (Bake)");
        applyAdj.setStyle("-fx-font-weight: bold;");
        applyAdj.setOnAction(ev -> layer.applyAdjustmentsToPixels());

        adjMenu.getItems().addAll(resetAdj, applyAdj);
        menu.getItems().add(adjMenu);

        // Transform
        javafx.scene.control.Menu transMenu = new javafx.scene.control.Menu("Transformar");
        transMenu.setGraphic(UIFactory.crearIcono("mdi2f-format-rotate-90", 16, "#555"));
        MenuItem rot90 = new MenuItem("Girar 90°");
        rot90.setGraphic(UIFactory.crearIcono("mdi2r-rotate-right", 16, "#3498db"));
        rot90.setOnAction(ev -> layer.rotateBy(90));
        MenuItem flipH = new MenuItem("Espejo Horizontal");
        flipH.setGraphic(UIFactory.crearIcono("mdi2f-flip-horizontal", 16, "#e67e22"));
        flipH.setOnAction(ev -> layer.flipHorizontal());
        MenuItem flipV = new MenuItem("Espejo Vertical");
        flipV.setGraphic(UIFactory.crearIcono("mdi2f-flip-vertical", 16, "#9b59b6"));
        flipV.setOnAction(ev -> layer.flipVertical());
        MenuItem resetTrans = new MenuItem("Resetear Transformaciones");
        resetTrans.setGraphic(UIFactory.crearIcono("mdi2r-restore", 16, "#c0392b"));
        resetTrans.setOnAction(ev -> {
            if (visualizer != null && visualizer.getHistoryManager() != null) {
                org.example.pattern.NodeMemento before = new org.example.pattern.NodeMemento(layer);
                layer.resetTransforms();
                visualizer.getHistoryManager().addCommand(new org.example.pattern.TransformCommand(layer, before, new org.example.pattern.NodeMemento(layer), layer.getActiveZone()));
            } else {
                layer.resetTransforms();
            }
        });
        transMenu.getItems().addAll(rot90, flipH, flipV, new javafx.scene.control.SeparatorMenuItem(), resetTrans);
        menu.getItems().add(transMenu);

        // Extract (if in zone)
        if (isClipped) {
            MenuItem extractItem = new MenuItem("Extraer del Contenedor");
            extractItem.setGraphic(UIFactory.crearIcono("mdi2e-eject", 16, "#555"));
            extractItem.setOnAction(ev -> visualizer.applySmartPowerClip(layer, null, false));
            menu.getItems().add(extractItem);
        }

        // Delete
        menu.getItems().add(new javafx.scene.control.SeparatorMenuItem());
        MenuItem delItem = new MenuItem("Eliminar");
        delItem.setGraphic(UIFactory.crearIcono("mdi2d-delete", 16, "#e74c3c"));
        delItem.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        delItem.setOnAction(ev -> visualizer.removeUserLayer(layer));
        menu.getItems().add(delItem);

        menu.show(visualizer, e.getScreenX(), e.getScreenY());
    }

    private void handleGlobalContextMenu(MouseEvent e, String forceZone) {
        // Close any existing menu
        if (activeGlobalMenu != null) {
            activeGlobalMenu.hide();
            activeGlobalMenu = null;
        }

        ContextMenu bgMenu = new ContextMenu();
        activeGlobalMenu = bgMenu;

        int selCount = layerManager.getSelectedNodes().size();
        boolean isEditingMode = visualizer.getPowerClipManager().isEditing();

        // --- 1. HIGHEST HIERARCHY: Edit Content (If clicking over a zone) ---
        String detectedZone = (forceZone != null) ? forceZone
                : visualizer.getShapeHelper().detectZone(e.getSceneX(), e.getSceneY());

        if (detectedZone != null && !isEditingMode) {
            boolean hasContent = !visualizer.getPowerClipManager().isZoneEmpty(detectedZone);
            if (hasContent) {
                MenuItem editZone = new MenuItem("EDITAR CONTENIDO: " + formatZoneName(detectedZone).toUpperCase());
                editZone.setGraphic(UIFactory.crearIcono("mdi2c-crop-free", 16, "#27ae60"));
                editZone.setOnAction(ev -> {
                    bgMenu.hide();
                    activeGlobalMenu = null;
                    visualizer.getPowerClipManager().enterEditMode(detectedZone);
                });
                bgMenu.getItems().add(editZone);
                bgMenu.getItems().add(new javafx.scene.control.SeparatorMenuItem());
            }
        }

        // --- 2. CLIPBOARD OPTIONS (Promoted to 2nd position) ---
        if (ShapeLayer.hasClipboard() || ImageLayer.hasClipboard() || TextLayer.hasClipboard()) {
            addPasteOptionsToMenu(bgMenu);
            bgMenu.getItems().add(new javafx.scene.control.SeparatorMenuItem());
        }

        // --- 3. SELECTION ACTIONS (PowerClip / Grouping) ---
        if (selCount > 0) {
            // PowerClip (Insert selection) - ONLY if NOT already clipped
            boolean anyClipped = layerManager.getSelectedNodes().stream()
                    .anyMatch(n -> (n instanceof ImageLayer && ((ImageLayer) n).getActiveZone() != null) ||
                            (n instanceof ShapeLayer && ((ShapeLayer) n).getActiveZone() != null) ||
                            (n instanceof TextLayer && ((TextLayer) n).getActiveZone() != null));

            if (!isEditingMode && !anyClipped) {
                java.util.List<String> available = visualizer.getAvailableZones();
                if (!available.isEmpty()) {
                    javafx.scene.control.Menu multiPcMenu = new javafx.scene.control.Menu(
                            "PowerClip (Insertar Selección...)");
                    multiPcMenu.setGraphic(UIFactory.crearIcono("mdi2a-arrow-right-bold-box", 16, "#555"));
                    if (available.contains("PECHO")) {
                        addMultiZoneMenuItem(multiPcMenu, "PECHO", "mdi2t-tshirt-crew", "#2c3e50", true);
                        addMultiZoneMenuItem(multiPcMenu, "ESPALDA", "mdi2t-tshirt-crew-outline", "#2c3e50", false);
                    }
                    if (available.contains("MANGA_DELANTERA")) {
                        multiPcMenu.getItems().add(new javafx.scene.control.SeparatorMenuItem());
                        addMultiZoneMenuItem(multiPcMenu, "MANGA_DELANTERA", "mdi2a-arm-flex", "#16a085", true);
                        addMultiZoneMenuItem(multiPcMenu, "MANGA_TRASERA", "mdi2a-arm-flex-outline", "#16a085", false);
                    }
                    if (available.contains("SHORT_FRONT")) {
                        multiPcMenu.getItems().add(new javafx.scene.control.SeparatorMenuItem());
                        addMultiZoneMenuItem(multiPcMenu, "SHORT_FRONT", "mdi2v-view-column", "#d35400", true);
                        addMultiZoneMenuItem(multiPcMenu, "SHORT_BACK", "mdi2v-view-column-outline", "#d35400", false);
                    }
                    bgMenu.getItems().add(multiPcMenu);
                }
            }

            // Grouping
            if (selCount > 1) {
                MenuItem groupItem = new MenuItem("Agrupar (Ctrl+G)");
                groupItem.setGraphic(UIFactory.crearIcono("mdi2g-group", 16, "#555"));
                groupItem.setOnAction(ev -> layerManager.groupSelected());
                bgMenu.getItems().add(groupItem);
            } else {
                Node sel = layerManager.getSelectedNodes().iterator().next();
                if (sel instanceof org.example.component.GroupLayer || sel instanceof org.example.component.GroupLayerV2) {
                    MenuItem ungroupItem = new MenuItem("Desagrupar (Ctrl+U)");
                    ungroupItem.setGraphic(UIFactory.crearIcono("mdi2g-group", 16, "#555"));
                    ungroupItem.setOnAction(ev -> layerManager.ungroup(sel));
                    bgMenu.getItems().add(ungroupItem);
                }
            }

            // ALIGNMENT
            if (selCount > 1) {
                bgMenu.getItems().add(new javafx.scene.control.SeparatorMenuItem());
                // (Simplified align menu if needed, but keeping for now)
            }

            // Only show Order/Delete if NOT clipped or IF editing
            if (!anyClipped || isEditingMode) {
                bgMenu.getItems().add(new javafx.scene.control.SeparatorMenuItem());
                javafx.scene.control.Menu orderMenu = new javafx.scene.control.Menu("Orden / Capas");
                orderMenu.setGraphic(UIFactory.crearIcono("mdi2l-layers", 16, "#555"));

                MenuItem front = new MenuItem("Traer al Frente");
                front.setOnAction(ev -> new java.util.ArrayList<>(layerManager.getSelectedNodes())
                        .forEach(layerManager::bringToFront));
                MenuItem back = new MenuItem("Enviar al Fondo");
                back.setOnAction(ev -> new java.util.ArrayList<>(layerManager.getSelectedNodes())
                        .forEach(layerManager::sendToBack));
                orderMenu.getItems().addAll(front, back);
                bgMenu.getItems().add(orderMenu);

                MenuItem delItem = new MenuItem("Eliminar");
                delItem.setGraphic(UIFactory.crearIcono("mdi2d-delete", 16, "#e74c3c"));
                delItem.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                delItem.setOnAction(ev -> new java.util.ArrayList<>(layerManager.getSelectedNodes())
                        .forEach(visualizer::removeUserLayer));
                bgMenu.getItems().add(delItem);
            }
        }

        // Only show menu if there's something to show
        if (!bgMenu.getItems().isEmpty()) {
            bgMenu.show(visualizer, e.getScreenX(), e.getScreenY());
            e.consume();
        }
    }

    private void createTextContextMenu(TextLayer layer, MouseEvent e) {
        ContextMenu menu = new ContextMenu();
        activeGlobalMenu = menu;

        boolean isClipped = (layer.getActiveZone() != null);
        boolean isEditingThis = visualizer.getPowerClipManager().isEditing()
                && layer.getActiveZone() != null
                && layer.getActiveZone().equals(visualizer.getPowerClipManager().getCurrentEditingZone());

        // --- 1. CLIPPED (OUTSIDE): Minimal Menu ---
        if (isClipped && !isEditingThis) {
            MenuItem editZone = new MenuItem(
                    "EDITAR CONTENIDO: " + formatZoneName(layer.getActiveZone()).toUpperCase());
            editZone.setGraphic(UIFactory.crearIcono("mdi2c-crop-free", 16, "#27ae60"));
            editZone.setOnAction(ev -> visualizer.getPowerClipManager().enterEditMode(layer.getActiveZone()));
            menu.getItems().add(editZone);

            if (TextLayer.hasClipboard() || ImageLayer.hasClipboard() || ShapeLayer.hasClipboard()) {
                menu.getItems().add(new javafx.scene.control.SeparatorMenuItem());
                addPasteOptionsToMenu(menu);
            }
            menu.show(visualizer, e.getScreenX(), e.getScreenY());
            return;
        }

        // --- 2. FLOATING / EDITING: Full Hierarchy ---
        // A. Header
        MenuItem header = new MenuItem("TEXTO");
        header.setDisable(true);
        header.setStyle("-fx-font-weight: bold; -fx-opacity: 1.0; -fx-text-fill: #2c3e50;");
        menu.getItems().add(header);

        // B. PowerClip
        if (!isClipped) {
            javafx.scene.control.Menu pcMenu = new javafx.scene.control.Menu("PowerClip (Mover a...)");
            pcMenu.setGraphic(UIFactory.crearIcono("mdi2a-arrow-right-bold-box", 16, "#3498db"));
            java.util.List<String> available = visualizer.getAvailableZones();
            if (available.contains("PECHO")) {
                addNodeZoneMenuItem(pcMenu, layer, "PECHO", "mdi2t-tshirt-crew", "#2c3e50", true);
                addNodeZoneMenuItem(pcMenu, layer, "ESPALDA", "mdi2t-tshirt-crew-outline", "#2c3e50", false);
            }
            if (available.contains("MANGA_DELANTERA")) {
                pcMenu.getItems().add(new javafx.scene.control.SeparatorMenuItem());
                addNodeZoneMenuItem(pcMenu, layer, "MANGA_DELANTERA", "mdi2a-arm-flex", "#16a085", true);
                addNodeZoneMenuItem(pcMenu, layer, "MANGA_TRASERA", "mdi2a-arm-flex-outline", "#16a085", false);
            }
            if (available.contains("SHORT_FRONT")) {
                pcMenu.getItems().add(new javafx.scene.control.SeparatorMenuItem());
                addNodeZoneMenuItem(pcMenu, layer, "SHORT_FRONT", "mdi2v-view-column", "#d35400", true);
                addNodeZoneMenuItem(pcMenu, layer, "SHORT_BACK", "mdi2v-view-column-outline", "#d35400", false);
            }
            if (!pcMenu.getItems().isEmpty()) {
                menu.getItems().add(pcMenu);
            }
        }
        menu.getItems().add(new javafx.scene.control.SeparatorMenuItem());

        // C. Clipboard
        MenuItem copyItem = new MenuItem("Copiar");
        copyItem.setGraphic(UIFactory.crearIcono("mdi2c-content-copy", 16, "#555"));
        copyItem.setOnAction(ev -> layer.copyToClipboard());

        MenuItem cutItem = new MenuItem("Cortar");
        cutItem.setGraphic(UIFactory.crearIcono("mdi2c-content-cut", 16, "#555"));
        cutItem.setOnAction(ev -> layer.cutToClipboard());

        menu.getItems().addAll(copyItem, cutItem);

        // D. Full Tools
        menu.getItems().add(new javafx.scene.control.SeparatorMenuItem());
        // Edit Texto
        MenuItem edit = new MenuItem("Editar Texto...");
        edit.setGraphic(UIFactory.crearIcono("mdi2p-pencil", 16, "#555"));
        edit.setOnAction(ev -> visualizer.selectLayer(layer));
        menu.getItems().add(edit);

        MenuItem convertToCurves = new MenuItem("Convertir a Curvas (Vectores)");
        convertToCurves.setGraphic(UIFactory.crearIcono("mdi2v-vector-curve", 16, "#f59e0b"));
        convertToCurves.setOnAction(ev -> {
            java.util.List<org.example.component.ShapeLayer> vectorLayers = org.example.component.helper.TextVectorizationHelper
                    .convertToShapeLayers(layer);
            if (!vectorLayers.isEmpty()) {
                String activeZone = layer.getActiveZone();
                javafx.scene.Group parent = layer.getParent() instanceof javafx.scene.Group
                        ? (javafx.scene.Group) layer.getParent()
                        : null;
                int insertionIndex = parent != null ? parent.getChildren().indexOf(layer) : -1;
                visualizer.getLayerManager().removeLayer(layer);
                for (int i = 0; i < vectorLayers.size(); i++) {
                    org.example.component.ShapeLayer vectorLayer = vectorLayers.get(i);
                    vectorLayer.setActiveZone(activeZone);
                    if (parent != null) {
                        visualizer.addShapeLayerToContainer(vectorLayer, parent, insertionIndex + i, false);
                    } else {
                        visualizer.addShapeLayer(vectorLayer);
                    }
                }
                visualizer.getLayerManager().clearSelection();
                for (org.example.component.ShapeLayer vectorLayer : vectorLayers) {
                    visualizer.getLayerManager().addToSelection(vectorLayer);
                }
            } else {
                org.example.component.ShapeLayer vectorLayer = org.example.component.helper.TextVectorizationHelper
                        .convertToShapeLayer(layer);
                if (vectorLayer != null) {
                    String activeZone = layer.getActiveZone();
                    visualizer.getLayerManager().removeLayer(layer);
                    vectorLayer.setActiveZone(activeZone);
                    visualizer.addShapeLayer(vectorLayer);
                }
            }
        });
        menu.getItems().add(convertToCurves);

        MenuItem putStraight = new MenuItem("Poner Recto");
        putStraight.setGraphic(UIFactory.crearIcono("mdi2f-format-variant", 16, "#3b82f6"));
        putStraight.setOnAction(ev -> {
            layer.setTrajectoryType(org.example.model.TrajectoryPath.Type.STRAIGHT);
        });
        menu.getItems().add(putStraight);
        menu.getItems().add(new javafx.scene.control.SeparatorMenuItem());

        // Order
        javafx.scene.control.Menu orderMenu = new javafx.scene.control.Menu("Orden / Capas");
        orderMenu.setGraphic(UIFactory.crearIcono("mdi2l-layers", 16, "#555"));
        MenuItem front = new MenuItem("Traer al Frente");
        front.setOnAction(ev -> visualizer.getLayerManager().bringToFront(layer));
        MenuItem up = new MenuItem("Subir Capa");
        up.setOnAction(ev -> visualizer.getLayerManager().bringForward(layer));
        MenuItem down = new MenuItem("Bajar Capa");
        down.setOnAction(ev -> visualizer.getLayerManager().sendBackward(layer));
        MenuItem back = new MenuItem("Enviar al Fondo");
        back.setOnAction(ev -> visualizer.getLayerManager().sendToBack(layer));
        orderMenu.getItems().addAll(front, up, down, back);
        menu.getItems().add(orderMenu);

        // Transform
        javafx.scene.control.Menu transMenu = new javafx.scene.control.Menu("Transformar");
        transMenu.setGraphic(UIFactory.crearIcono("mdi2f-format-rotate-90", 16, "#555"));
        MenuItem rot90 = new MenuItem("Girar 90°");
        rot90.setGraphic(UIFactory.crearIcono("mdi2r-rotate-right", 16, "#3498db"));
        rot90.setOnAction(ev -> layer.setRotate(layer.getRotate() + 90));
        MenuItem flipH = new MenuItem("Espejo Horizontal");
        flipH.setGraphic(UIFactory.crearIcono("mdi2f-flip-horizontal", 16, "#e67e22"));
        flipH.setOnAction(ev -> layer.flipHorizontal());
        MenuItem flipV = new MenuItem("Espejo Vertical");
        flipV.setGraphic(UIFactory.crearIcono("mdi2f-flip-vertical", 16, "#9b59b6"));
        flipV.setOnAction(ev -> layer.flipVertical());
        MenuItem resetTrans = new MenuItem("Resetear Transformaciones");
        resetTrans.setGraphic(UIFactory.crearIcono("mdi2r-restore", 16, "#c0392b"));
        resetTrans.setOnAction(ev -> {
            if (visualizer != null && visualizer.getHistoryManager() != null) {
                org.example.pattern.NodeMemento before = new org.example.pattern.NodeMemento(layer);
                layer.setRotate(0);
                layer.setScaleX(1);
                layer.setScaleY(1);
                visualizer.getHistoryManager().addCommand(new org.example.pattern.TransformCommand(layer, before, new org.example.pattern.NodeMemento(layer), layer.getActiveZone()));
            } else {
                layer.setRotate(0);
                layer.setScaleX(1);
                layer.setScaleY(1);
            }
        });
        transMenu.getItems().addAll(rot90, flipH, flipV, new javafx.scene.control.SeparatorMenuItem(), resetTrans);
        menu.getItems().add(transMenu);

        // Extract
        if (isClipped) {
            MenuItem extractItem = new MenuItem("Extraer del Contenedor");
            extractItem.setGraphic(UIFactory.crearIcono("mdi2e-eject", 16, "#555"));
            extractItem.setOnAction(ev -> visualizer.applySmartPowerClip(layer, null, false));
            menu.getItems().add(new javafx.scene.control.SeparatorMenuItem());
            menu.getItems().add(extractItem);
        }

        // Delete
        menu.getItems().add(new javafx.scene.control.SeparatorMenuItem());
        MenuItem delItem = new MenuItem("Eliminar");
        delItem.setGraphic(UIFactory.crearIcono("mdi2d-delete", 16, "#e74c3c"));
        delItem.setOnAction(ev -> visualizer.removeUserLayer(layer));
        menu.getItems().add(delItem);

        menu.show(visualizer, e.getScreenX(), e.getScreenY());
    }

    private void addPasteOptionsToMenu(ContextMenu menu) {
        boolean isEditingMode = visualizer.getPowerClipManager().isEditing();
        String zone = isEditingMode ? visualizer.getPowerClipManager().getCurrentEditingZone() : null;

        if (ShapeLayer.hasClipboard()) {
            MenuItem pasteShape = new MenuItem("Pegar Figura");
            pasteShape.setGraphic(UIFactory.crearIcono("mdi2c-content-paste", 16, "#555"));
            pasteShape.setOnAction(ev -> {
                ShapeLayer sl = ShapeLayer.getClipboardCopy();
                if (sl != null) {
                    visualizer.addShapeLayer(sl);
                    if (isEditingMode) {
                        visualizer.applySmartPowerClip(sl, zone, false);
                        sl.setLocked(false);
                    }
                }
            });
            menu.getItems().add(pasteShape);
        }

        if (ImageLayer.hasClipboard()) {
            MenuItem pasteImg = new MenuItem("Pegar Imagen");
            pasteImg.setGraphic(UIFactory.crearIcono("mdi2c-content-paste", 16, "#555"));
            pasteImg.setOnAction(ev -> {
                ImageLayer il = ImageLayer.getClipboardCopy();
                if (il != null) {
                    layerFactory.addImageLayer(il);
                    if (isEditingMode) {
                        visualizer.applySmartPowerClip(il, zone, false);
                        il.setLocked(false);
                    }
                }
            });
            menu.getItems().add(pasteImg);
        }

        if (TextLayer.hasClipboard()) {
            MenuItem pasteText = new MenuItem("Pegar Texto");
            pasteText.setGraphic(UIFactory.crearIcono("mdi2c-content-paste", 16, "#555"));
            pasteText.setOnAction(ev -> {
                TextLayer tl = TextLayer.getClipboardCopy();
                if (tl != null) {
                    visualizer.addTextLayer(tl);
                    if (isEditingMode) {
                        visualizer.applySmartPowerClip(tl, zone, false);
                        tl.setLocked(false);
                    }
                }
            });
            menu.getItems().add(pasteText);
        }
    }

    private String formatZoneName(String zone) {
        if (zone == null)
            return "Global";

        if (zone.startsWith("SHORT")) {
            org.example.model.TipoCorte corte = visualizer.getState().getCorteShort();
            String base = "Short";
            boolean isFem = false;

            if (corte == org.example.model.TipoCorte.PANTALONETA) {
                base = "Pantaloneta";
                isFem = true;
            } else if (corte == org.example.model.TipoCorte.LICRA) {
                base = "Licra";
                isFem = true;
            }

            String suffix = "";
            if (zone.endsWith("FRONT")) {
                suffix = isFem ? "Delantera" : "Delantero";
            } else {
                suffix = isFem ? "Trasera" : "Trasero";
            }
            return base + " " + suffix;
        }

        switch (zone) {
            case "PECHO":
                return "Pecho";
            case "ESPALDA":
                return "Espalda";
            case "MANGA_DELANTERA":
                return "Manga Delantera";
            case "MANGA_TRASERA":
                return "Manga Trasera";
            default:
                return zone;
        }
    }

    private void addNodeZoneMenuItem(javafx.scene.control.Menu parent, Node layer, String zone, String icon,
            String color, boolean bold) {
        String label = formatZoneName(zone);
        MenuItem item = new MenuItem(label);
        item.setGraphic(UIFactory.crearIcono(icon, 16, color));
        item.setStyle("-fx-text-fill: " + color + (bold ? "; -fx-font-weight: bold;" : ""));
        item.setOnAction(ev -> visualizer.applySmartPowerClip(layer, zone, false));
        parent.getItems().add(item);
    }

    private void addMultiZoneMenuItem(javafx.scene.control.Menu parent, String zone, String icon, String color,
            boolean bold) {
        String label = formatZoneName(zone);
        MenuItem item = new MenuItem(label);
        item.setGraphic(UIFactory.crearIcono(icon, 16, color));
        item.setStyle("-fx-text-fill: " + color + (bold ? "; -fx-font-weight: bold;" : ""));

        item.setOnAction(ev -> {
            // Copy list to avoid concurrent modification issues if selection changes
            List<Node> selected = new ArrayList<>(layerManager.getSelectedNodes());
            for (Node n : selected) {
                // Determine Layer Type and Apply logic
                // Now supports Groups via generic overload
                visualizer.applySmartPowerClip(n, zone, false);
            }
        });
        parent.getItems().add(item);
    }

    private void setupBackgroundClick() {
        visualizer.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
            // Close Context Menu on any click
            if (activeGlobalMenu != null) {
                activeGlobalMenu.hide();
                activeGlobalMenu = null;
            }

            // CLEANUP: Close any active layer tools if we clicked on background
            if (activeToolCanceler != null) {
                activeToolCanceler.run();
                activeToolCanceler = null;
            }

            // If the event bubbles up to here, it wasn't consumed by a layer/handle
            // So we treat it as a background click (deselect)
            layerManager.clearSelection();
            visualizer.deselectAllNames();
            visualizer.clearGlobalSelection();
        });
    }

    private void performAlignmentWithUndo(String name, Runnable action) {
        if (layerManager.getSelectedNodes().isEmpty())
            return;

        // 1. Capture State BEFORE
        List<Node> nodes = new ArrayList<>(layerManager.getSelectedNodes());
        List<double[]> startStates = new ArrayList<>();

        for (Node n : nodes) {
            startStates.add(new double[] {
                    n.getTranslateX(), n.getTranslateY(),
                    n.getScaleX(), n.getScaleY(),
                    n.getRotate()
            });
        }

        // 2. Perform Action
        action.run();

        // 3. Capture State AFTER & Build Composite Command
        org.example.pattern.CompositeCommand composite = new org.example.pattern.CompositeCommand(name);
        boolean anyChange = false;

        for (int i = 0; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            double[] start = startStates.get(i);

            double endTx = n.getTranslateX();
            double endTy = n.getTranslateY();
            double endSx = n.getScaleX();
            double endSy = n.getScaleY();
            double endRot = n.getRotate();

            if (Math.abs(start[0] - endTx) > 0.1 || Math.abs(start[1] - endTy) > 0.1) {
                composite.addCommand(new org.example.pattern.TransformCommand(
                        n,
                        start[0], start[1], start[2], start[3], start[4],
                        endTx, endTy, endSx, endSy, endRot));
                anyChange = true;
            }
        }

        if (anyChange && visualizer.getHistoryManager() != null) {
            visualizer.getHistoryManager().addCommand(composite);
        }
    }

    private CustomMenuItem createSliderAdjustmentItem(String label, double initialValue, double min, double max,
            java.util.function.Consumer<Double> consumer) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #555; -fx-min-width: 85px;");

        Slider slider = new Slider(min, max, initialValue);
        slider.setPrefWidth(100);

        Label valueLbl = new Label();
        valueLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-min-width: 35px;");
        
        java.lang.Runnable updateVal = () -> {
            double v = slider.getValue();
            if (label.toLowerCase().contains("tolerancia")) {
                valueLbl.setText(String.format("%.0f%%", v * 100));
            } else {
                valueLbl.setText(String.format("%.0f", v));
            }
        };
        updateVal.run();

        slider.valueProperty().addListener((obs, old, val) -> {
            consumer.accept(val.doubleValue());
            updateVal.run();
        });

        HBox box = new HBox(5, lbl, slider, valueLbl);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(5, 10, 5, 10));

        CustomMenuItem item = new CustomMenuItem(box);
        item.setHideOnClick(false);
        return item;
    }
}

