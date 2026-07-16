package org.example.component;

import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Node;
import org.example.component.helper.PrendaHistoryManager;
import org.example.pattern.LayerActionCommand;
import org.example.pattern.GroupCommand;

import java.util.function.Consumer;

/**
 * Manages user-added layers (Images and Text) on the garment.
 * Handles selection state, addition, removal, and grouping.
 * Extracted from PrendaVisualizer to reduce complexity.
 */
public class UserLayerManager {

    private final Group layerGroup;
    private final javafx.collections.ObservableList<Node> allLayers = javafx.collections.FXCollections
            .observableArrayList();
    private final java.util.Map<String, Group> zoneBuckets = new java.util.HashMap<>();

    // Multi-selection support (LinkedHashSet maintains insertion order for
    // Alignment Anchor logic)
    private final java.util.Set<Node> selectedNodes = new java.util.LinkedHashSet<>();
    private Node primarySelectedNode; // The most recently selected node (for UI context)

    // List-based Listeners for multi-component sync
    private final java.util.List<Consumer<Node>> selectionListeners = new java.util.ArrayList<>();
    private final java.util.List<Consumer<Node>> removalListeners = new java.util.ArrayList<>();
    private final java.util.List<Consumer<Node>> additionListeners = new java.util.ArrayList<>();

    private PrendaHistoryManager historyManager;
    private boolean isPerformingHistoryAction = false;

    // A/B TESTING: Flag to use new GroupLayerV2 architecture
    private static final boolean USE_V2_GROUPS = true;

    // Concurrency Guard
    private boolean isGrouping = false;

    public UserLayerManager() {
        this.layerGroup = new Group();
        // GRAN FINAL: Forzar renderizado plano desde la raíz
        this.layerGroup.setDepthTest(javafx.scene.DepthTest.DISABLE);

        // --- ADVANCED STABILITY: FORCE QUALITY CACHE ON ROOT ---
        this.layerGroup.setCache(false);
        // this.layerGroup.setCacheHint(javafx.scene.CacheHint.QUALITY);

        // REMOVED: Explicit SRC_OVER can trigger unstable renderNodeBlendMode path in
        // Prism D3D
    }

    public void setHistoryManager(PrendaHistoryManager historyManager) {
        this.historyManager = historyManager;
    }

    public void setPerformingHistoryAction(boolean performing) {
        this.isPerformingHistoryAction = performing;
    }

    public boolean isPerformingHistoryAction() {
        return this.isPerformingHistoryAction;
    }

    public void getOrCreateBucket(String zoneName) {
        // Obsolete: All zone logic moved to PowerClipManager
    }

    public void setBucketClip(String zoneName, String svgContent) {
        // Obsolete: All zone logic moved to PowerClipManager
    }

    public Group getLayerGroup() {
        return layerGroup;
    }

    public void addLayer(Node layer) {
        if (!allLayers.contains(layer)) {
            allLayers.add(layer);
        }

        // ASEGURAR 2D PURO
        layer.setDepthTest(javafx.scene.DepthTest.DISABLE);
        layer.setCache(false);

        // Safe reparenting to ROOT group
        if (layer.getParent() != null && layer.getParent() instanceof Group) {
            ((Group) layer.getParent()).getChildren().remove(layer);
        }

        if (!layerGroup.getChildren().contains(layer)) {
            layerGroup.getChildren().add(layer);
        }

        // Notify Listeners
        for (Consumer<Node> listener : new java.util.ArrayList<>(additionListeners)) {
            listener.accept(layer);
        }

        // Record History
        if (historyManager != null && !isPerformingHistoryAction) {
            historyManager.addCommand(new LayerActionCommand(this, layer, LayerActionCommand.ActionType.ADD));
        }

        selectNode(layer);
    }

    public void addLayerToZone(Node layer, String zoneName) {
        // This is now purely for logical registration or is handled by
        // PrendaVisualizer/PowerClipManager
        addLayer(layer);
    }

    public void addLayerToZone(Node layer, String zoneName, boolean autoSelect) {
        addLayer(layer);
        if (autoSelect) {
            selectNode(layer);
        }
    }

    public void addLayerToContainer(Node layer, Group container, boolean autoSelect) {
        if (!allLayers.contains(layer)) {
            allLayers.add(layer);
        }
        // Safe reparenting
        if (layer.getParent() != null && layer.getParent() instanceof Group) {
            Group oldParent = (Group) layer.getParent();
            oldParent.getChildren().remove(layer);
            if (oldParent.getParent() instanceof org.example.component.GroupLayerV2) {
                ((org.example.component.GroupLayerV2) oldParent.getParent()).syncUserLayersOrder();
            } else if (oldParent instanceof org.example.component.GroupLayerV2) {
                ((org.example.component.GroupLayerV2) oldParent).syncUserLayersOrder();
            }
        }

        if (container instanceof org.example.component.GroupLayerV2) {
            ((org.example.component.GroupLayerV2) container).addChild(layer);
        } else if (container.getParent() instanceof org.example.component.GroupLayerV2) {
            ((org.example.component.GroupLayerV2) container.getParent()).addChild(layer);
        } else {
            if (!container.getChildren().contains(layer)) {
                container.getChildren().add(layer);
            }
        }

        // Notify Listeners
        for (Consumer<Node> listener : new java.util.ArrayList<>(additionListeners)) {
            listener.accept(layer);
        }

        if (autoSelect) {
            selectNode(layer);
        }
    }

    public void removeLayer(Node layer) {
        allLayers.remove(layer);
        if (layer.getParent() != null) {
            javafx.scene.Parent parent = layer.getParent();
            javafx.scene.Parent grandParent = parent.getParent();
            if (grandParent instanceof org.example.component.GroupLayerV2) {
                ((org.example.component.GroupLayerV2) grandParent).removeChild(layer);
            } else if (parent instanceof org.example.component.GroupLayerV2) {
                ((org.example.component.GroupLayerV2) parent).removeChild(layer);
            }
        }
        if (layer.getParent() instanceof Group) {
            ((Group) layer.getParent()).getChildren().remove(layer);
        }
        if (selectedNodes.contains(layer)) {
            selectedNodes.remove(layer);
            if (layer == primarySelectedNode) {
                primarySelectedNode = selectedNodes.isEmpty() ? null : selectedNodes.iterator().next();
            }
            notifySelection();
        }
        // Notify Listeners
        for (Consumer<Node> listener : new java.util.ArrayList<>(removalListeners)) {
            listener.accept(layer);
        }

        // Record History
        if (historyManager != null && !isPerformingHistoryAction) {
            historyManager.addCommand(new LayerActionCommand(this, layer, LayerActionCommand.ActionType.REMOVE));
        }
    }

    public void trackRestoredLayer(Node layer) {
        if (!allLayers.contains(layer)) {
            allLayers.add(layer);
        }
        selectNode(layer);
    }

    public void moveNodeBehind(Node nodeToMove, Node referenceNode) {
        if (nodeToMove == null || referenceNode == null || nodeToMove.getParent() == null) {
            return;
        }
        if (!(nodeToMove.getParent() instanceof Group)) {
            return;
        }
        Group parentGroup = (Group) nodeToMove.getParent();
        if (parentGroup != referenceNode.getParent()) {
            return; // Must be in same group
        }
        int refIndex = parentGroup.getChildren().indexOf(referenceNode);
        if (refIndex != -1) {
            parentGroup.getChildren().remove(nodeToMove);
            // Re-calculate indexOf referenceNode because removing nodeToMove might have
            // shifted it
            refIndex = parentGroup.getChildren().indexOf(referenceNode);
            parentGroup.getChildren().add(refIndex, nodeToMove);
        }
    }

    public void removeLayerIf(java.util.function.Predicate<Node> filter) {
        boolean changed = false;
        java.util.Iterator<Node> it = allLayers.iterator();
        while (it.hasNext()) {
            Node node = it.next();
            if (filter.test(node)) {
                it.remove();
                if (node.getParent() instanceof Group) {
                    ((Group) node.getParent()).getChildren().remove(node);
                }
                if (selectedNodes.remove(node)) {
                    changed = true;
                }
                if (node == primarySelectedNode) {
                    primarySelectedNode = null;
                }
                for (Consumer<Node> listener : new java.util.ArrayList<>(removalListeners)) {
                    listener.accept(node);
                }
            }
        }

        if (primarySelectedNode == null && !selectedNodes.isEmpty()) {
            primarySelectedNode = selectedNodes.iterator().next();
        }

        if (changed) {
            notifySelection();
        }
    }

    public void addOnNodeRemoved(Consumer<Node> listener) {
        if (listener != null)
            this.removalListeners.add(listener);
    }

    public void addRemovalListener(Consumer<Node> listener) {
        addOnNodeRemoved(listener);
    }

    // Compat method
    public void setOnNodeRemoved(Consumer<Node> listener) {
        addOnNodeRemoved(listener);
    }

    public void addAdditionListener(Consumer<Node> listener) {
        if (listener != null)
            this.additionListeners.add(listener);
    }

    public void clearAll() {
        java.util.List<Node> copy = new java.util.ArrayList<>(allLayers);
        allLayers.clear();
        zoneBuckets.values().forEach(b -> b.getChildren().clear());
        layerGroup.getChildren().clear();
        clearSelection();
        for (Node n : copy) {
            for (Consumer<Node> listener : new java.util.ArrayList<>(removalListeners)) {
                listener.accept(n);
            }
        }
    }

    public void clearTextLayers() {
        removeLayerIf(node -> node instanceof TextLayer);
    }

    // Exclusive selection (clears others)
    public void selectNode(Node node) {
        if (node != null) {
            // RECURSIVE GROUP LOOKUP (Find the top-most group ancestor)
            Node temp = node;
            Node topmostGroup = node;
            while (temp.getParent() != null) {
                temp = temp.getParent();
                if ("USER_GROUP".equals(temp.getId()) || temp instanceof GroupLayerV2 || temp instanceof GroupLayer) {
                    topmostGroup = temp;
                }
            }
            node = topmostGroup;
        }

        if (selectedNodes.size() == 1 && selectedNodes.contains(node))
            return; // Already selected, skip redundant work
        deselectAllInternal();

        if (node != null) {
            addToSelectionInternal(node);
        }
        notifySelection();
    }

    // Toggle selection (Ctrl+Click)
    public void toggleSelection(Node node) {
        if (node == null)
            return;

        // RECURSIVE GROUP LOOKUP (Find the top-most group ancestor)
        Node temp = node;
        Node topmostGroup = node;
        while (temp.getParent() != null) {
            temp = temp.getParent();
            if ("USER_GROUP".equals(temp.getId()) || temp instanceof GroupLayerV2 || temp instanceof GroupLayer) {
                topmostGroup = temp;
            }
        }
        node = topmostGroup;

        if (selectedNodes.contains(node)) {
            removeFromSelectionInternal(node);
        } else {
            addToSelectionInternal(node);
        }
        notifySelection();
    }

    public void addToSelection(Node node) {
        if (node != null) {
            // RECURSIVE GROUP LOOKUP (Find the top-most group ancestor)
            Node temp = node;
            Node topmostGroup = node;
            while (temp.getParent() != null) {
                temp = temp.getParent();
                if ("USER_GROUP".equals(temp.getId()) || temp instanceof GroupLayerV2 || temp instanceof GroupLayer) {
                    topmostGroup = temp;
                }
            }
            node = topmostGroup;

            if (!selectedNodes.contains(node)) {
                addToSelectionInternal(node);
                notifySelection();
            }
        }
    }

    private void addToSelectionInternal(Node node) {
        selectedNodes.add(node);
        primarySelectedNode = node;
        setNodeSelected(node, true);
    }

    private void removeFromSelectionInternal(Node node) {
        selectedNodes.remove(node);
        if (primarySelectedNode == node) {
            primarySelectedNode = selectedNodes.isEmpty() ? null : selectedNodes.iterator().next();
        }
        setNodeSelected(node, false);
    }

    private void deselectAllInternal() {
        for (Node n : new java.util.ArrayList<>(selectedNodes)) {
            setNodeSelected(n, false);
        }
        selectedNodes.clear();
        primarySelectedNode = null;
    }

    public void clearSelection() {
        if (!selectedNodes.isEmpty()) {
            deselectAllInternal();
            notifySelection();
        }
    }

    private javafx.geometry.Point2D localToWorkspace(Node node, javafx.geometry.Point2D p) {
        javafx.geometry.Point2D current = p;
        Node next = node;
        while (next != null && next != layerGroup) {
            current = next.localToParent(current);
            next = next.getParent();
        }
        return current;
    }

    private javafx.geometry.Point2D localToWorkspace(Node node, double x, double y) {
        return localToWorkspace(node, new javafx.geometry.Point2D(x, y));
    }

    private javafx.geometry.Point2D workspaceToLocal(Node node, javafx.geometry.Point2D p) {
        javafx.geometry.Point2D current = p;
        // Collect chain
        java.util.List<Node> chain = new java.util.ArrayList<>();
        Node next = node;
        while (next != null && next != layerGroup) {
            chain.add(next);
            next = next.getParent();
        }
        // Apply inverse from top to bottom
        java.util.Collections.reverse(chain);
        for (Node n : chain) {
            current = n.parentToLocal(current);
        }
        return current;
    }

    private javafx.geometry.Bounds getSanitizedWorkspaceBounds(Node n) {
        double minX = 0, minY = 0, w = 0, h = 0;

        if (n instanceof ShapeLayer) {
            ShapeLayer sl = (ShapeLayer) n;
            minX = sl.getVisualMinX();
            minY = sl.getVisualMinY();
            w = sl.getLogicalWidth();
            h = sl.getLogicalHeight();
        } else if (n instanceof ImageLayer) {
            ImageLayer il = (ImageLayer) n;
            w = il.getLogicalWidth();
            h = il.getLogicalHeight();
        } else if (n instanceof TextLayer) {
            TextLayer tl = (TextLayer) n;
            w = tl.getLogicalWidth();
            h = tl.getLogicalHeight();
        } else if (n instanceof GroupLayer) {
            GroupLayer gl = (GroupLayer) n;
            minX = gl.getBoundsMinX();
            minY = gl.getBoundsMinY();
            w = gl.getLogicalWidth();
            h = gl.getLogicalHeight();
            // Group content corners must be projected through Group's internal transforms
            javafx.geometry.Point2D p1 = localToWorkspace(gl.getContentGroup(), minX, minY);
            javafx.geometry.Point2D p2 = localToWorkspace(gl.getContentGroup(), minX + w, minY);
            javafx.geometry.Point2D p3 = localToWorkspace(gl.getContentGroup(), minX, minY + h);
            javafx.geometry.Point2D p4 = localToWorkspace(gl.getContentGroup(), minX + w, minY + h);

            double sMinX = Math.min(Math.min(p1.getX(), p2.getX()), Math.min(p3.getX(), p4.getX()));
            double sMaxX = Math.max(Math.max(p1.getX(), p2.getX()), Math.max(p3.getX(), p4.getX()));
            double sMinY = Math.min(Math.min(p1.getY(), p2.getY()), Math.min(p3.getY(), p4.getY()));
            double sMaxY = Math.max(Math.max(p1.getY(), p2.getY()), Math.max(p3.getY(), p4.getY()));

            return new javafx.geometry.BoundingBox(sMinX, sMinY, sMaxX - sMinX, sMaxY - sMinY);
        } else {
            return n.getBoundsInParent(); // Best effort
        }

        // Project logical corners to workspace
        javafx.geometry.Point2D p1 = localToWorkspace(n, minX, minY);
        javafx.geometry.Point2D p2 = localToWorkspace(n, minX + w, minY);
        javafx.geometry.Point2D p3 = localToWorkspace(n, minX, minY + h);
        javafx.geometry.Point2D p4 = localToWorkspace(n, minX + w, minY + h);

        double sMinX = Math.min(Math.min(p1.getX(), p2.getX()), Math.min(p3.getX(), p4.getX()));
        double sMaxX = Math.max(Math.max(p1.getX(), p2.getX()), Math.max(p3.getX(), p4.getX()));
        double sMinY = Math.min(Math.min(p1.getY(), p2.getY()), Math.min(p3.getY(), p4.getY()));
        double sMaxY = Math.max(Math.max(p1.getY(), p2.getY()), Math.max(p3.getY(), p4.getY()));

        return new javafx.geometry.BoundingBox(sMinX, sMinY, sMaxX - sMinX, sMaxY - sMinY);
    }

    // --- NOTIFICATION CONTROL (Performance Fix) ---
    private boolean suppressNotifications = false;

    public void suspendNotifications() {
        this.suppressNotifications = true;
    }

    public void resumeNotifications() {
        this.suppressNotifications = false;
        // Optionally trigger a pending notification if dirty?
        // For now, caller is responsible for triggering final update.
    }

    private void notifySelection() {
        if (suppressNotifications)
            return;
        for (Consumer<Node> listener : new java.util.ArrayList<>(selectionListeners)) {
            listener.accept(primarySelectedNode);
        }
    }

    public void addSelectionListener(Consumer<Node> listener) {
        if (listener != null)
            this.selectionListeners.add(listener);
    }

    // Compat method
    public void setOnSelectionChanged(Consumer<Node> listener) {
        addSelectionListener(listener);
    }

    public Node getSelectedNode() {
        return primarySelectedNode;
    }

    public java.util.Set<Node> getSelectedNodes() {
        return new java.util.LinkedHashSet<>(selectedNodes);
    }

    public ImageLayer getSelectedImageLayer() {
        if (primarySelectedNode instanceof ImageLayer)
            return (ImageLayer) primarySelectedNode;
        return null;
    }

    // Alias for getSelectedImageLayer() to satisfy BrandingController consumer
    public ImageLayer getSelectedLayer() {
        return getSelectedImageLayer();
    }

    public TextLayer getSelectedTextLayer() {
        if (primarySelectedNode instanceof TextLayer)
            return (TextLayer) primarySelectedNode;
        return null;
    }

    public ShapeLayer getSelectedShapeLayer() {
        if (primarySelectedNode instanceof ShapeLayer)
            return (ShapeLayer) primarySelectedNode;
        return null;
    }

    // --- GROUPING LOGIC ---

    private javafx.geometry.Bounds getVisualBoundsInParent(Node n) {
        double lx, ly, lw, lh;
        if (n instanceof ShapeLayer) {
            ShapeLayer sl = (ShapeLayer) n;
            lx = sl.getVisualMinX(); ly = sl.getVisualMinY(); lw = sl.getLogicalWidth(); lh = sl.getLogicalHeight();
        } else if (n instanceof ImageLayer) {
            ImageLayer il = (ImageLayer) n;
            lx = 0; ly = 0; lw = il.getLogicalWidth(); lh = il.getLogicalHeight();
        } else if (n instanceof TextLayer) {
            TextLayer tl = (TextLayer) n;
            lx = -tl.getLogicalWidth() / 2.0; ly = -tl.getLogicalHeight() / 2.0; lw = tl.getLogicalWidth(); lh = tl.getLogicalHeight();
        } else if (n instanceof GroupLayerV2) {
            GroupLayerV2 g2 = (GroupLayerV2) n;
            javafx.geometry.Bounds cb = g2.calculateBounds();
            lx = cb.getMinX(); ly = cb.getMinY(); lw = cb.getWidth(); lh = cb.getHeight();
        } else if (n instanceof GroupLayer) {
            GroupLayer gl = (GroupLayer) n;
            lx = gl.getBoundsMinX(); ly = gl.getBoundsMinY(); lw = gl.getLogicalWidth(); lh = gl.getLogicalHeight();
        } else {
            return n.getBoundsInParent();
        }
        
        javafx.geometry.Point2D p1 = n.localToParent(lx, ly);
        javafx.geometry.Point2D p2 = n.localToParent(lx + lw, ly);
        javafx.geometry.Point2D p3 = n.localToParent(lx, ly + lh);
        javafx.geometry.Point2D p4 = n.localToParent(lx + lw, ly + lh);
        
        double minX = Math.min(Math.min(p1.getX(), p2.getX()), Math.min(p3.getX(), p4.getX()));
        double maxX = Math.max(Math.max(p1.getX(), p2.getX()), Math.max(p3.getX(), p4.getX()));
        double minY = Math.min(Math.min(p1.getY(), p2.getY()), Math.min(p3.getY(), p4.getY()));
        double maxY = Math.max(Math.max(p1.getY(), p2.getY()), Math.max(p3.getY(), p4.getY()));
        
        return new javafx.geometry.BoundingBox(minX, minY, maxX - minX, maxY - minY);
    }

    public void groupSelected() {
        if (isGrouping)
            return; // Prevent double execution

        if (selectedNodes.size() < 2)
            return;

        java.util.List<Node> toGroup = new java.util.ArrayList<>(selectedNodes);
        if (toGroup.isEmpty())
            return;

        Node first = toGroup.get(0);
        Group parentGroup = (Group) first.getParent();
        if (parentGroup == null)
            return;

        // Verify same parent
        for (Node n : toGroup) {
            if (n.getParent() != parentGroup) {
                System.out.println("Cannot group items from different containers.");
                return;
            }
        }

        // Sort toGroup by their original index in parentGroup to preserve layer hierarchy
        toGroup.sort(java.util.Comparator.comparingInt(parentGroup.getChildren()::indexOf));

        isGrouping = true; // Lock
        // PERF: Suspend updates to prevent UI freeze during batch remove/add
        suspendNotifications();
        try {

            if (USE_V2_GROUPS) {
                // Clear active selections FIRST to hide handles that distort bounds calculation
                clearSelection();

                java.util.List<org.example.pattern.NodeMemento> beforeStates = new java.util.ArrayList<>();
                for (Node n : toGroup) {
                    beforeStates.add(new org.example.pattern.NodeMemento(n));
                }

                // Calculate Bounds for positioning using exact visual bounds (ignores invisible handles)
                double minX = Double.MAX_VALUE;
                double minY = Double.MAX_VALUE;

                for (Node n : toGroup) {
                    javafx.geometry.Bounds b = getVisualBoundsInParent(n);
                    if (b.getMinX() < minX)
                        minX = b.getMinX();
                    if (b.getMinY() < minY)
                        minY = b.getMinY();
                }

                // Create GroupLayerV2
                org.example.component.GroupLayerV2 newGroup = new org.example.component.GroupLayerV2();
                System.out.println("✅ USANDO GROUPLAYER V2 - Nueva arquitectura");

                // Infer activeZone from the items being grouped
                String groupZone = null;
                if (!toGroup.isEmpty()) {
                    Node firstItem = toGroup.get(0);
                    if (firstItem instanceof org.example.component.GraphicLayer) {
                        groupZone = ((org.example.component.GraphicLayer) firstItem).getActiveZone();
                    } else if (firstItem instanceof org.example.component.GroupLayerV2) {
                        groupZone = ((org.example.component.GroupLayerV2) firstItem).getActiveZone();
                    } else if (firstItem instanceof org.example.component.GroupLayer) {
                        groupZone = ((org.example.component.GroupLayer) firstItem).getActiveZone();
                    }
                }
                if (groupZone != null) {
                    newGroup.setActiveZone(groupZone);
                }

                // Calculate Insertion Index
                int targetIndex = -1;
                for (Node n : toGroup) {
                    int idx = parentGroup.getChildren().indexOf(n);
                    if (idx > targetIndex)
                        targetIndex = idx;
                }
                if (targetIndex == -1 || targetIndex > parentGroup.getChildren().size())
                    targetIndex = parentGroup.getChildren().size();

                // Position Group at Top-Left of selection
                newGroup.setTranslateX(minX);
                newGroup.setTranslateY(minY);

                // CRITICAL FIX: Inject Visualizer for History Recording
                if (historyManager != null) {
                    newGroup.setVisualizer(historyManager.getVisualizer());
                }

                // Insert Group into Scene
                parentGroup.getChildren().add(targetIndex, newGroup);

                // Add items to Group (uses addChild which handles insertion order)
                for (Node n : toGroup) {
                    // Remove from parent
                    parentGroup.getChildren().remove(n);

                    // Adjust local coords to be relative to group position
                    n.setTranslateX(n.getTranslateX() - minX);
                    n.setTranslateY(n.getTranslateY() - minY);

                    // Add to group
                    newGroup.addChild(n);
                }

                // Update Bounds
                newGroup.recalculateBounds();
                newGroup.toFront();

                // Select the new group
                selectNode(newGroup);

                if (historyManager != null && !isPerformingHistoryAction) {
                    historyManager.addCommand(new GroupCommand(this, newGroup, toGroup, GroupCommand.ActionType.GROUP,
                            beforeStates, null, null));
                }

            } else {
                // ============================================
                // OLD V1 IMPLEMENTATION (WORKSPACE ALIGNMENT FIX)
                // ============================================

                // 1. CAPTURE MEMENTOS AND V1 ALIGNMENT DATA
                clearSelection(); // Hide handles

                java.util.List<org.example.pattern.NodeMemento> beforeStates = new java.util.ArrayList<>();
                java.util.Map<Node, javafx.geometry.Point2D> targetWorkspaceCenters = new java.util.HashMap<>();
                java.util.Map<Node, javafx.geometry.Point2D> localCenters = new java.util.HashMap<>();

                double minWorkX = Double.MAX_VALUE;
                double minWorkY = Double.MAX_VALUE;

                for (Node n : toGroup) {
                    beforeStates.add(new org.example.pattern.NodeMemento(n));
                    javafx.geometry.Point2D centerLocal = getStableCenter(n);
                    localCenters.put(n, centerLocal);

                    // Capture center in stable Workspace coordinates
                    targetWorkspaceCenters.put(n, localToWorkspace(n, centerLocal));

                    // Use sanitized workspace bounds for group placement
                    javafx.geometry.Bounds workBounds = getSanitizedWorkspaceBounds(n);
                    if (workBounds.getMinX() < minWorkX)
                        minWorkX = workBounds.getMinX();
                    if (workBounds.getMinY() < minWorkY)
                        minWorkY = workBounds.getMinY();
                }

                // 2. CREATE AND PLACE GROUP
                org.example.component.GroupLayer newGroup = new org.example.component.GroupLayer();
                newGroup.setId("USER_GROUP");
                if (historyManager != null) {
                    newGroup.setVisualizer(historyManager.getVisualizer());
                }

                // Place newGroup at exactly the Workspace Top-Left calculated
                newGroup.setTranslateX(minWorkX);
                newGroup.setTranslateY(minWorkY);

                // Find insertion index
                int insertIdx = parentGroup.getChildren().size();
                for (Node n : toGroup) {
                    int i = parentGroup.getChildren().indexOf(n);
                    if (i != -1 && i < insertIdx)
                        insertIdx = i;
                }
                parentGroup.getChildren().add(insertIdx, newGroup);

                // CRITICAL: Explicitly remove objects from their current parent
                // BEFORE adding them to the new group. This prevents the JavaFX 'Index -1'
                // Bounds caching crash caused by implicit removal during forced layouts.
                parentGroup.getChildren().removeAll(toGroup);

                // 3. MOVE ITEMS TO GROUP
                for (Node n : toGroup) {
                    n.setTranslateX(0);
                    n.setTranslateY(0);
                    newGroup.getContentGroup().getChildren().add(n);
                }

                // 4. ABSOLUTE REALIGNMENT (In Workspace coordinates)
                for (Node n : toGroup) {
                    javafx.geometry.Point2D targetWork = targetWorkspaceCenters.get(n);
                    javafx.geometry.Point2D stableCenter = localCenters.get(n);

                    if (targetWork != null && stableCenter != null) {
                        // Match current Workspace position to target Workspace position
                        javafx.geometry.Point2D targetInGroupLocal = workspaceToLocal(newGroup.getContentGroup(),
                                targetWork);
                        javafx.geometry.Point2D currentInGroupLocal = n.localToParent(stableCenter); // n is child of
                                                                                                     // contentGroup

                        double dx = targetInGroupLocal.getX() - currentInGroupLocal.getX();
                        double dy = targetInGroupLocal.getY() - currentInGroupLocal.getY();

                        n.setTranslateX(dx);
                        n.setTranslateY(dy);
                    }
                }

                // 5. NORMALIZE GROUP (Tight Fit)
                newGroup.recalculateBounds();
                newGroup.toFront();

                selectNode(newGroup);

                if (historyManager != null && !isPerformingHistoryAction) {
                    historyManager.addCommand(new GroupCommand(this, newGroup, toGroup, GroupCommand.ActionType.GROUP,
                            beforeStates, null, null));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            isGrouping = false; // Unlock
            resumeNotifications();
        }
    }

    public void ungroupSelected() {
        if (primarySelectedNode != null) {
            ungroup(primarySelectedNode);
        }
    }

    private javafx.geometry.Point2D getStableCenter(Node n) {
        if (n instanceof ShapeLayer)
            return ((ShapeLayer) n).getStableCenter();
        if (n instanceof ImageLayer)
            return ((ImageLayer) n).getStableCenter();
        if (n instanceof TextLayer)
            return ((TextLayer) n).getStableCenter();
        if (n instanceof GroupLayer)
            return ((GroupLayer) n).getStableCenter();
        if (n instanceof GroupLayerV2) {
            javafx.geometry.Bounds cb = ((GroupLayerV2) n).calculateBounds();
            return new javafx.geometry.Point2D((cb.getMinX() + cb.getMaxX()) / 2.0, (cb.getMinY() + cb.getMaxY()) / 2.0);
        }

        javafx.geometry.Bounds b = n.getBoundsInLocal();
        return new javafx.geometry.Point2D((b.getMinX() + b.getMaxX()) / 2.0, (b.getMinY() + b.getMaxY()) / 2.0);
    }

    public void ungroup(Node groupNode) {
        if (!(groupNode instanceof GroupLayer || groupNode instanceof GroupLayerV2))
            return;

        suspendNotifications();
        try {
            Group group = (Group) groupNode;
            Group parent = (Group) group.getParent();
            if (parent == null)
                return;

            java.util.List<Node> children = new java.util.ArrayList<>();

            // Support both V1 and V2
            if (groupNode instanceof org.example.component.GroupLayerV2) {
                children.addAll(((org.example.component.GroupLayerV2) groupNode).getUserLayers());
            } else if (groupNode instanceof org.example.component.GroupLayer) {
                children.addAll(((org.example.component.GroupLayer) groupNode).getUserLayers());
            }

            int groupIndex = parent.getChildren().indexOf(group);
            if (groupIndex == -1)
                groupIndex = parent.getChildren().size();

            // Clear Group Selection first
            clearSelection();

            // 1. CAPTURE MEMENTOS AND VISUAL CENTERS (PASS 1) - In Workspace coordinates
            java.util.List<org.example.pattern.NodeMemento> beforeStates = new java.util.ArrayList<>();
            org.example.pattern.NodeMemento groupBeforeState = new org.example.pattern.NodeMemento(groupNode);

            java.util.Map<Node, javafx.geometry.Point2D> targetWorkspaceCenters = new java.util.HashMap<>();
            java.util.Map<Node, javafx.geometry.Point2D> localCenters = new java.util.HashMap<>();
            java.util.Map<Node, javafx.scene.transform.Transform> combinedTransforms = new java.util.HashMap<>();

            for (Node c : children) {
                beforeStates.add(new org.example.pattern.NodeMemento(c));
                javafx.geometry.Point2D centerLocal = getStableCenter(c);
                localCenters.put(c, centerLocal);
                targetWorkspaceCenters.put(c, localToWorkspace(c, centerLocal));

                // Capture combined transform relative to the group's parent dynamically.
                // If the child is a GroupLayer or GroupLayerV2, its internal transforms are applied
                // to its contentGroup. We must start concatenation from contentGroup to include them.
                Node startNode;
                if (c instanceof org.example.component.AbstractGraphicLayer) {
                    startNode = ((org.example.component.AbstractGraphicLayer) c).getContentGroup();
                } else if (c instanceof org.example.component.GroupLayer) {
                    startNode = ((org.example.component.GroupLayer) c).getContentGroup();
                } else {
                    startNode = c;
                }

                javafx.scene.transform.Transform combined = startNode.getLocalToParentTransform();
                Node next = startNode.getParent();
                while (next != null && next != parent) {
                    combined = next.getLocalToParentTransform().createConcatenation(combined);
                    next = next.getParent();
                }
                combinedTransforms.put(c, combined);
            }

            // 2. REPARENT & APPLY PROPERTIES (PASS 2)
            int i = 0;
            for (Node c : children) {
                // Determine group's shear BEFORE reparenting (for shear restoration fallback)
                double shearX = 0;
                double shearY = 0;
                if (groupNode instanceof org.example.component.GroupLayerV2) {
                    shearX = ((org.example.component.GroupLayerV2) groupNode).getInternalShearX();
                    shearY = ((org.example.component.GroupLayerV2) groupNode).getInternalShearY();
                }

                // Remove from Group
                if (groupNode instanceof org.example.component.GroupLayerV2) {
                    ((org.example.component.GroupLayerV2) groupNode).removeChild(c);
                } else if (groupNode instanceof org.example.component.GroupLayer) {
                    ((org.example.component.GroupLayer) groupNode).getContentGroup().getChildren().remove(c);
                }

                // Add to Parent
                parent.getChildren().add(groupIndex + i, c);

                // RESET TRANSLATE - Start from clean slate
                c.setTranslateX(0);
                c.setTranslateY(0);

                // DECOMPOSE AND APPLY ROTATION AND SCALE
                javafx.scene.transform.Transform combined = combinedTransforms.get(c);
                if (combined != null) {
                    double a = combined.getMxx();
                    double b = combined.getMxy();
                    double c_val = combined.getMyx();
                    double d = combined.getMyy();

                    double det = a * d - b * c_val;
                    double sx = Math.sqrt(a * a + c_val * c_val);
                    double sy = Math.sqrt(b * b + d * d);

                    if (det < 0) {
                        sx = -sx;
                    }

                    double rotation = 0;
                    if (Math.abs(sx) > 1e-9) {
                        rotation = Math.toDegrees(Math.atan2(c_val / sx, a / sx));
                    }

                    setNodeRotationAndScale(c, rotation, sx, sy);
                }

                // RESTORE SHEAR (fallback if group was sheared)
                if (Math.abs(shearX) > 0.001 || Math.abs(shearY) > 0.001) {
                    if (c instanceof org.example.component.ShapeLayer) {
                        ((org.example.component.ShapeLayer) c).multiplyShear(shearX, shearY);
                    } else if (c instanceof org.example.component.ImageLayer) {
                        org.example.component.ImageLayer il = (org.example.component.ImageLayer) c;
                        il.getShearTransform().setX(il.getShearTransform().getX() + shearX);
                        il.getShearTransform().setY(il.getShearTransform().getY() + shearY);
                    } else if (c instanceof org.example.component.GroupLayerV2) {
                        ((org.example.component.GroupLayerV2) c).multiplyShear(shearX, shearY);
                    }
                }

                // 3. CORRECT POSITION (ABSOLUTE WORKSPACE REALIGNMENT)
                javafx.geometry.Point2D stableCenter = getStableCenter(c); // RECALCULATE post-scale
                javafx.geometry.Point2D targetWork = targetWorkspaceCenters.get(c);

                if (targetWork != null && stableCenter != null) {
                    c.applyCss();
                    if (c instanceof javafx.scene.Parent)
                        ((javafx.scene.Parent) c).layout();

                    // Find where it should be in the new parent space using workspace coordinates
                    javafx.geometry.Point2D targetInParentSpace = workspaceToLocal(parent, targetWork);
                    javafx.geometry.Point2D currentInParentSpace = c.localToParent(stableCenter);

                    if (targetInParentSpace != null && currentInParentSpace != null) {
                        double dx = targetInParentSpace.getX() - currentInParentSpace.getX();
                        double dy = targetInParentSpace.getY() - currentInParentSpace.getY();

                        c.setTranslateX(c.getTranslateX() + dx);
                        c.setTranslateY(c.getTranslateY() + dy);
                    }
                }
                
                if (!allLayers.contains(c)) {
                    allLayers.add(c);
                }

                if (parent.getParent() instanceof org.example.component.helper.SmartZoneContainer zoneContainer) {
                    String groupZone = null;
                    if (groupNode instanceof org.example.component.GroupLayerV2) {
                        groupZone = ((org.example.component.GroupLayerV2) groupNode).getActiveZone();
                    } else if (groupNode instanceof org.example.component.GroupLayer) {
                        groupZone = ((org.example.component.GroupLayer) groupNode).getActiveZone();
                    }
                    if (groupZone != null) {
                        if (c instanceof org.example.component.ShapeLayer) {
                            ((org.example.component.ShapeLayer) c).setActiveZone(groupZone);
                        } else if (c instanceof org.example.component.ImageLayer) {
                            ((org.example.component.ImageLayer) c).setActiveZone(groupZone);
                        } else if (c instanceof org.example.component.TextLayer) {
                            ((org.example.component.TextLayer) c).setActiveZone(groupZone);
                        } else if (c instanceof org.example.component.GroupLayerV2) {
                            ((org.example.component.GroupLayerV2) c).setActiveZone(groupZone);
                        }
                    }
                    zoneContainer.updateItemState(c);
                    if (historyManager != null && historyManager.getVisualizer() != null) {
                        historyManager.getVisualizer().getPowerClipManager().refreshZoneClip(zoneContainer.getZoneId());
                    }
                    // Do NOT auto-select children after ungroup inside PowerClip.
                    // User must click to select the desired child normally.
                } else {
                    addToSelectionInternal(c);
                }
                i++;
            }

            // Final cleanup
            groupNode.setVisible(false);
            parent.getChildren().remove(groupNode);

            notifySelection();

            // Record History
            if (historyManager != null && !isPerformingHistoryAction) {
                historyManager.addCommand(new GroupCommand(this, groupNode, new java.util.ArrayList<>(children),
                        GroupCommand.ActionType.UNGROUP, beforeStates, groupBeforeState, null));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            resumeNotifications();
        }
    }

    // --- Z-Order Management ---

    public void bringToFront(Node node) {
        if (node != null && node.getParent() instanceof Group) {
            int oldIndex = node.getParent().getChildrenUnmodifiable().indexOf(node);
            node.toFront();
            int newIndex = node.getParent().getChildrenUnmodifiable().indexOf(node);

            if (historyManager != null && !isPerformingHistoryAction && oldIndex != newIndex) {
                historyManager.addCommand(new org.example.pattern.ZOrderCommand(this, node, oldIndex, newIndex));
            }
        }
    }

    public void sendToBack(Node node) {
        if (node != null && node.getParent() instanceof Group) {
            int oldIndex = node.getParent().getChildrenUnmodifiable().indexOf(node);
            node.toBack();
            int newIndex = node.getParent().getChildrenUnmodifiable().indexOf(node);

            if (historyManager != null && !isPerformingHistoryAction && oldIndex != newIndex) {
                historyManager.addCommand(new org.example.pattern.ZOrderCommand(this, node, oldIndex, newIndex));
            }
        }
    }

    public void bringForward(Node node) {
        if (node != null && node.getParent() instanceof Group) {
            java.util.List<Node> children = ((Group) node.getParent()).getChildren();
            int oldIndex = children.indexOf(node);
            if (oldIndex < children.size() - 1) {
                children.remove(oldIndex);
                children.add(oldIndex + 1, node);

                if (historyManager != null && !isPerformingHistoryAction) {
                    historyManager
                            .addCommand(new org.example.pattern.ZOrderCommand(this, node, oldIndex, oldIndex + 1));
                }
            }
        }
    }

    public void sendBackward(Node node) {
        if (node != null && node.getParent() instanceof Group) {
            java.util.List<Node> children = ((Group) node.getParent()).getChildren();
            int oldIndex = children.indexOf(node);
            if (oldIndex > 0) {
                children.remove(oldIndex);
                children.add(oldIndex - 1, node);

                if (historyManager != null && !isPerformingHistoryAction) {
                    historyManager
                            .addCommand(new org.example.pattern.ZOrderCommand(this, node, oldIndex, oldIndex - 1));
                }
            }
        }
    }

    public ObservableList<Node> getLayers() {
        return allLayers;
    }

    private void setNodeSelected(Node node, boolean selected) {
        if (node instanceof ImageLayer) {
            ((ImageLayer) node).setSelected(selected);
        } else if (node instanceof TextLayer) {
            ((TextLayer) node).setSelected(selected);
        } else if (node instanceof ShapeLayer) {
            ((ShapeLayer) node).setSelected(selected);
        } else if (node instanceof org.example.component.GroupLayerV2) {
            // V2 Group Selection
            ((org.example.component.GroupLayerV2) node).setSelected(selected);
        } else if (node instanceof org.example.component.GroupLayer) {
            // V1 Group Selection
            ((org.example.component.GroupLayer) node).setSelected(selected);
        } else if ("NUMBER_ROOT".equals(node.getId())) {
             // Standard Number selection
             Object data = node.getUserData();
             if (data instanceof org.example.component.NumberComposition) {
                 ((org.example.component.NumberComposition) data).setSelected(selected);
             }
        } else if (node instanceof Group) {
            for (Node child : new java.util.ArrayList<>(((Group) node).getChildren())) {
                setNodeSelected(child, selected);
            }
        }
    }

    public java.util.List<TextLayer> getAllTextLayers() {
        java.util.List<TextLayer> list = new java.util.ArrayList<>();
        collectTextLayers(layerGroup, list);
        return list;
    }

    private void collectTextLayers(Group root, java.util.List<TextLayer> list) {
        for (Node n : root.getChildren()) {
            if (n instanceof TextLayer) {
                list.add((TextLayer) n);
            } else if (n instanceof Group) {
                collectTextLayers((Group) n, list);
            } else if (n instanceof org.example.component.helper.SmartZoneContainer) {
                collectTextLayers(((org.example.component.helper.SmartZoneContainer) n).getContentGroup(), list);
            }
        }
    }

    private void setNodeRotationAndScale(Node n, double rotation, double scaleX, double scaleY) {
        if (n instanceof org.example.component.ShapeLayer) {
            org.example.component.ShapeLayer sl = (org.example.component.ShapeLayer) n;
            sl.setInternalRotation(rotation);
            sl.setInternalScaleX(scaleX);
            sl.setInternalScaleY(scaleY);
        } else if (n instanceof org.example.component.ImageLayer) {
            org.example.component.ImageLayer il = (org.example.component.ImageLayer) n;
            il.setInternalRotation(rotation);
            il.setInternalScaleX(scaleX);
            il.setInternalScaleY(scaleY);
        } else if (n instanceof org.example.component.TextLayer) {
            org.example.component.TextLayer tl = (org.example.component.TextLayer) n;
            tl.setInternalRotation(rotation);
            tl.setInternalScaleX(scaleX);
            tl.setInternalScaleY(scaleY);
        } else if (n instanceof org.example.component.GroupLayerV2) {
            org.example.component.GroupLayerV2 gl = (org.example.component.GroupLayerV2) n;
            gl.setInternalRotation(rotation);
            gl.setInternalScaleX(scaleX);
            gl.setInternalScaleY(scaleY);
        } else if (n instanceof org.example.component.GroupLayer) {
            org.example.component.GroupLayer gl = (org.example.component.GroupLayer) n;
            gl.setInternalRotation(rotation);
            gl.setInternalScaleX(scaleX);
            gl.setInternalScaleY(scaleY);
        } else {
            n.setRotate(rotation);
            n.setScaleX(scaleX);
            n.setScaleY(scaleY);
        }
    }
}
