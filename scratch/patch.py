import os

fpath = r"c:\Users\palan\Desktop\PALANT-PEDIDOS\src\main\java\org\example\component\helper\BezierInteractionService.java"
with open(fpath, "r", encoding="utf-8") as f:
    code = f.read()

# 1. State Variables
code = code.replace(
    "private BezierNode selectedBezierNode;",
    "private final java.util.Set<BezierNode> selectedBezierNodes = new java.util.LinkedHashSet<>();\n    private Rectangle marqueeRect;\n    private Point2D marqueeStartScene;\n    private boolean isMarqueeDragging = false;"
)

code = code.replace(
    "selectedBezierNode = null;",
    "selectedBezierNodes.clear();"
)

# 2. Rect Fills
code = code.replace(
    "rect.setFill(n == selectedBezierNode ? Color.RED : Color.web(\"#0078D7\"));",
    "rect.setFill(selectedBezierNodes.contains(n) ? Color.RED : Color.web(\"#0078D7\"));"
)

code = code.replace(
    "if (nodeRef != selectedBezierNode) rect.setFill(Color.web(\"#006CC2\"));",
    "if (!selectedBezierNodes.contains(nodeRef)) rect.setFill(Color.web(\"#006CC2\"));"
)

code = code.replace(
    "rect.setFill(nodeRef == selectedBezierNode ? Color.RED : Color.web(\"#0078D7\"));",
    "rect.setFill(selectedBezierNodes.contains(nodeRef) ? Color.RED : Color.web(\"#0078D7\"));"
)

# 3. Hitbox Anchor Press
old_anchor_press = """            hitBoxAnchor.setOnMousePressed(e -> {
                selectedBezierNode = nodeRef;
                currentDragNode = nodeRef;
                currentDragType = "ANCHOR";
                if (editingLayer != null && editingLayer.getBezierNodes() != null) {
                    nodesBeforeEdit = new ArrayList<>();
                    for (BezierNode bn : editingLayer.getBezierNodes()) {
                        nodesBeforeEdit.add(bn.copy());
                    }
                }
            });"""
new_anchor_press = """            hitBoxAnchor.setOnMousePressed(e -> {
                if (!e.isShiftDown()) {
                    if (!selectedBezierNodes.contains(nodeRef)) {
                        selectedBezierNodes.clear();
                        selectedBezierNodes.add(nodeRef);
                    }
                } else {
                    if (selectedBezierNodes.contains(nodeRef)) {
                        selectedBezierNodes.remove(nodeRef);
                    } else {
                        selectedBezierNodes.add(nodeRef);
                    }
                }
                updateNodeEditHandles();
                currentDragNode = nodeRef;
                currentDragType = "ANCHOR";
                if (editingLayer != null && editingLayer.getBezierNodes() != null) {
                    nodesBeforeEdit = new ArrayList<>();
                    for (BezierNode bn : editingLayer.getBezierNodes()) {
                        nodesBeforeEdit.add(bn.copy());
                    }
                }
            });"""
code = code.replace(old_anchor_press, new_anchor_press)

# 4. Context Menu Delete
code = code.replace(
    "delItem.setOnAction(ev -> { selectedBezierNode = n; deleteSelectedNode(); });",
    "delItem.setOnAction(ev -> { if (!selectedBezierNodes.contains(n)) { selectedBezierNodes.clear(); selectedBezierNodes.add(n); } deleteSelectedNode(); });"
)

# 5. Control Point Hitbox Press
old_control_press = """        hitBox.setOnMousePressed(e -> {
            currentDragNode = n;
            selectedBezierNode = n;
            currentDragType = type;
            if (editingLayer != null && editingLayer.getBezierNodes() != null) {
                nodesBeforeEdit = new ArrayList<>();
                for (BezierNode bn : editingLayer.getBezierNodes()) {
                    nodesBeforeEdit.add(bn.copy());
                }
            }
            updateNodeEditHandles();
            e.consume();
        });"""
new_control_press = """        hitBox.setOnMousePressed(e -> {
            currentDragNode = n;
            if (!e.isShiftDown() && !selectedBezierNodes.contains(n)) {
                selectedBezierNodes.clear();
                selectedBezierNodes.add(n);
            } else if (e.isShiftDown()) {
                if (selectedBezierNodes.contains(n)) selectedBezierNodes.remove(n);
                else selectedBezierNodes.add(n);
            }
            currentDragType = type;
            if (editingLayer != null && editingLayer.getBezierNodes() != null) {
                nodesBeforeEdit = new ArrayList<>();
                for (BezierNode bn : editingLayer.getBezierNodes()) {
                    nodesBeforeEdit.add(bn.copy());
                }
            }
            updateNodeEditHandles();
            e.consume();
        });"""
code = code.replace(old_control_press, new_control_press)

# 6. Delete Node Logic
old_delete = """    private void deleteSelectedNode() {
        BezierNode toDelete = selectedBezierNode != null ? selectedBezierNode : currentDragNode;
        if (!isNodeEditing() || toDelete == null || bezierNodes == null || editingLayer == null) return;
        if (bezierNodes.size() <= 2) return;

        List<BezierNode> nodesBefore = new ArrayList<>();
        for (BezierNode bn : bezierNodes) nodesBefore.add(bn.copy());

        bezierNodes.remove(toDelete);
        currentDragNode = null;
        selectedBezierNode = null;
        updateNodeEditHandles();
        refreshLayerPath(editingLayer, bezierNodes);

        if (visualizer.getHistoryManager() != null) {
            List<BezierNode> currentNodes = new ArrayList<>();
            for (BezierNode bn : editingLayer.getBezierNodes()) currentNodes.add(bn.copy());
            visualizer.getHistoryManager().addCommand(new org.example.pattern.BezierEditCommand(editingLayer, nodesBefore, currentNodes, editingLayer.getActiveZone()));
        }
    }"""
new_delete = """    private void deleteSelectedNode() {
        if (!isNodeEditing() || bezierNodes == null || editingLayer == null) return;
        java.util.Set<BezierNode> toDelete = new java.util.HashSet<>(selectedBezierNodes);
        if (toDelete.isEmpty() && currentDragNode != null) {
            toDelete.add(currentDragNode);
        }
        if (toDelete.isEmpty() || bezierNodes.size() - toDelete.size() < 2) return;

        List<BezierNode> nodesBefore = new ArrayList<>();
        for (BezierNode bn : bezierNodes) nodesBefore.add(bn.copy());

        bezierNodes.removeAll(toDelete);
        currentDragNode = null;
        selectedBezierNodes.clear();
        updateNodeEditHandles();
        refreshLayerPath(editingLayer, bezierNodes);

        if (visualizer.getHistoryManager() != null) {
            List<BezierNode> currentNodes = new ArrayList<>();
            for (BezierNode bn : editingLayer.getBezierNodes()) currentNodes.add(bn.copy());
            visualizer.getHistoryManager().addCommand(new org.example.pattern.BezierEditCommand(editingLayer, nodesBefore, currentNodes, editingLayer.getActiveZone()));
        }
    }"""
code = code.replace(old_delete, new_delete)

# 7. Drag Handlers
old_drag = """        nodeEditDragHandler = e -> {
            if (!isNodeEditing() || currentDragNode == null) return;
            e.consume();

            Point2D pScene = new Point2D(e.getSceneX(), e.getSceneY());
            Point2D pLayer = editingLayer.sceneToLocal(pScene);
            String type = currentDragType != null ? currentDragType : "ANCHOR";"""
new_drag = """        nodeEditDragHandler = e -> {
            if (!isNodeEditing()) return;
            if (isMarqueeDragging && marqueeRect != null && marqueeStartScene != null) {
                double currentX = e.getSceneX();
                double currentY = e.getSceneY();
                Point2D startLocal = visualizer.getContentGroup().sceneToLocal(marqueeStartScene);
                Point2D currentLocal = visualizer.getContentGroup().sceneToLocal(currentX, currentY);
                marqueeRect.setX(Math.min(startLocal.getX(), currentLocal.getX()));
                marqueeRect.setY(Math.min(startLocal.getY(), currentLocal.getY()));
                marqueeRect.setWidth(Math.abs(currentLocal.getX() - startLocal.getX()));
                marqueeRect.setHeight(Math.abs(currentLocal.getY() - startLocal.getY()));
                e.consume();
                return;
            }
            if (currentDragNode == null) return;
            e.consume();

            Point2D pScene = new Point2D(e.getSceneX(), e.getSceneY());
            Point2D pLayer = editingLayer.sceneToLocal(pScene);
            String type = currentDragType != null ? currentDragType : "ANCHOR";"""
code = code.replace(old_drag, new_drag)

old_drag_multi = """            } else {
                double dx = pLayer.getX() - currentDragNode.anchor.getX();
                double dy = pLayer.getY() - currentDragNode.anchor.getY();
                currentDragNode.anchor = pLayer;
                currentDragNode.control1 = new Point2D(currentDragNode.control1.getX() + dx, currentDragNode.control1.getY() + dy);
                currentDragNode.control2 = new Point2D(currentDragNode.control2.getX() + dx, currentDragNode.control2.getY() + dy);
            }"""
new_drag_multi = """            } else {
                double dx = pLayer.getX() - currentDragNode.anchor.getX();
                double dy = pLayer.getY() - currentDragNode.anchor.getY();
                java.util.Set<BezierNode> toMove = new java.util.HashSet<>();
                if (selectedBezierNodes.contains(currentDragNode)) {
                    toMove.addAll(selectedBezierNodes);
                } else {
                    toMove.add(currentDragNode);
                }
                for (BezierNode n : toMove) {
                    n.anchor = new Point2D(n.anchor.getX() + dx, n.anchor.getY() + dy);
                    n.control1 = new Point2D(n.control1.getX() + dx, n.control1.getY() + dy);
                    n.control2 = new Point2D(n.control2.getX() + dx, n.control2.getY() + dy);
                }
            }"""
code = code.replace(old_drag_multi, new_drag_multi)

# 8. Release Handler
old_release = """        nodeEditReleaseHandler = e -> {
            if (isNodeEditing()) {
                // Finalize bounds and normalize coordinates on release
                if (editingLayer != null) {
                    recalculateLayerBounds(editingLayer, true); 
                    editingLayer.updateVisuals();
                }
                recordUndo();
                currentDragNode = null;
            }
        };"""
new_release = """        nodeEditReleaseHandler = e -> {
            if (isNodeEditing()) {
                if (isMarqueeDragging) {
                    isMarqueeDragging = false;
                    if (marqueeRect != null) {
                        marqueeRect.setVisible(false);
                        javafx.geometry.Bounds selectionBounds = marqueeRect.getBoundsInParent();
                        if (!e.isShiftDown()) selectedBezierNodes.clear();
                        for (Node child : handleGroup.getChildren()) {
                            if (child instanceof Rectangle && child != marqueeRect && ((Rectangle) child).getFill() == Color.TRANSPARENT) {
                                Object data = child.getUserData();
                                if (data instanceof Object[]) {
                                    Object[] tag = (Object[]) data;
                                    if ("ANCHOR".equals(tag[1])) {
                                        if (selectionBounds.intersects(child.getBoundsInParent())) {
                                            selectedBezierNodes.add((BezierNode) tag[0]);
                                        }
                                    }
                                }
                            }
                        }
                        updateNodeEditHandles();
                    }
                } else if (currentDragNode != null) {
                    if (editingLayer != null) {
                        recalculateLayerBounds(editingLayer, true); 
                        editingLayer.updateVisuals();
                    }
                    recordUndo();
                    currentDragNode = null;
                }
            }
        };"""
code = code.replace(old_release, new_release)

# 9. Insert/Marquee Press Handler
old_insert = """        nodeInsertHandler = e -> {
            if (isNodeEditing()) {
                boolean isDoubleClick = e.getClickCount() == 2 && e.isPrimaryButtonDown();
                boolean isRightClick = e.getButton() == javafx.scene.input.MouseButton.SECONDARY;
                if (!isDoubleClick && !isRightClick) return;
                if (e.getTarget() instanceof Circle || e.getTarget() instanceof Rectangle) return;

                Point2D pScene = new Point2D(e.getSceneX(), e.getSceneY());
                Point2D pLayer = editingLayer.sceneToLocal(pScene);
                tryInsertNode(pLayer.getX(), pLayer.getY());
                e.consume();
            }
        };"""
new_insert = """        nodeInsertHandler = e -> {
            if (isNodeEditing()) {
                boolean isDoubleClick = e.getClickCount() == 2 && e.isPrimaryButtonDown();
                boolean isRightClick = e.getButton() == javafx.scene.input.MouseButton.SECONDARY;
                boolean isSingleClick = e.getClickCount() == 1 && e.isPrimaryButtonDown();
                
                if (e.getTarget() instanceof Circle || e.getTarget() instanceof Rectangle) return;

                if (isDoubleClick) {
                    Point2D pScene = new Point2D(e.getSceneX(), e.getSceneY());
                    Point2D pLayer = editingLayer.sceneToLocal(pScene);
                    tryInsertNode(pLayer.getX(), pLayer.getY());
                    e.consume();
                } else if (isSingleClick) {
                    marqueeStartScene = new Point2D(e.getSceneX(), e.getSceneY());
                    isMarqueeDragging = true;
                    if (marqueeRect == null) {
                        marqueeRect = new Rectangle();
                        marqueeRect.setFill(Color.web("#3498db", 0.15));
                        marqueeRect.setStroke(Color.web("#2980b9"));
                        marqueeRect.setStrokeWidth(1.0);
                        marqueeRect.getStrokeDashArray().addAll(5.0, 5.0);
                        marqueeRect.setMouseTransparent(true);
                    }
                    if (!handleGroup.getChildren().contains(marqueeRect)) {
                        handleGroup.getChildren().add(marqueeRect);
                    }
                    Point2D localStart = visualizer.getContentGroup().sceneToLocal(marqueeStartScene);
                    marqueeRect.setX(localStart.getX());
                    marqueeRect.setY(localStart.getY());
                    marqueeRect.setWidth(0);
                    marqueeRect.setHeight(0);
                    marqueeRect.setVisible(true);
                    if (!e.isShiftDown()) {
                        selectedBezierNodes.clear();
                        updateNodeEditHandles();
                    }
                    e.consume();
                }
            }
        };"""
code = code.replace(old_insert, new_insert)

# Add Marquee rect back when refreshing handles
code = code.replace(
    "            handleGroup.getChildren().add(hitBoxAnchor);\n\n            int idx = i;",
    "            handleGroup.getChildren().add(hitBoxAnchor);\n\n            int idx = i;"
) # Wait, handleGroup is cleared in updateNodeEditHandles, so marqueeRect is lost!

code = code.replace(
    "        applyAntiScaleToHandles();\n    }",
    "        if (marqueeRect != null && isMarqueeDragging) {\n            if (!handleGroup.getChildren().contains(marqueeRect)) handleGroup.getChildren().add(marqueeRect);\n            marqueeRect.toFront();\n        }\n        applyAntiScaleToHandles();\n    }"
)

code = code.replace(
    "                    r.setFill(selectedBezierNodes.contains(n) ? Color.RED : Color.web(\"#0078D7\"));",
    "                    r.setFill(selectedBezierNodes.contains(n) ? Color.RED : Color.web(\"#0078D7\"));\n                }\n            } else if (child == marqueeRect) {\n                // ignore"
)

with open(fpath, "w", encoding="utf-8") as f:
    f.write(code)

print("Patch applied")
