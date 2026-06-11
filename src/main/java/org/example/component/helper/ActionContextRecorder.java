package org.example.component.helper;

import javafx.scene.Node;
import org.example.component.ImageLayer;
import org.example.component.ShapeLayer;
import org.example.component.TextLayer;
import org.example.component.GroupLayer;
import org.example.component.GroupLayerV2;
import org.example.pattern.NodeMemento;
import org.example.pattern.TransformCommand;
import org.example.pattern.ICommand;

import java.util.*;

/**
 * Central utility for recording action context to ensure consistent Undo/Redo snapshots.
 * 
 * PROBLEM SOLVED:
 * Previously, recordUndoState() was called manually in various handlers, but the corresponding
 * TransformCommand was created in onMouseReleased. IF the handler logic changed between press
 * and release (e.g., a sub-command triggered a selection change), the undo state would mismatch.
 * 
 * SOLUTION:
 * ActionContextRecorder provides a robust way to capture and register commands:
 * 
 *   BEFORE action: call captureFrom(node)
 *   AFTER  action: call registerCommand(node, "Action Name")
 * 
 * It also handles single-node and multi-node batching automatically.
 * 
 * USAGE PATTERN:
 *   ActionContextRecorder recorder = new ActionContextRecorder(historyManager);
 *   recorder.captureFrom(shapeLayer);
 *   // ... perform mutation ...
 *   recorder.registerCommand(shapeLayer, "Mover Capa");
 * 
 * OR for batch operations (moving multiple selected layers):
 *   ActionContextRecorder recorder = new ActionContextRecorder(historyManager);
 *   List<Node> selected = ...;
 *   recorder.captureFrom(selected);
 *   // ... perform mutations on all selected ...
 *   recorder.registerBatchCommand("Mover Seleccion", selected);
 */
public class ActionContextRecorder {

    private final PrendaHistoryManager historyManager;
    private final Map<Node, NodeMemento> beforeStates = new IdentityHashMap<>();
    private boolean inBatchCapture = false;

    public ActionContextRecorder(PrendaHistoryManager historyManager) {
        this.historyManager = historyManager;
    }

    // --- Single Node Capture ---

    public void captureFrom(Node node) {
        if (node == null) return;
        beforeStates.put(node, new NodeMemento(node));
    }

    // --- Multi Node Capture ---

    public void captureFrom(Collection<? extends Node> nodes) {
        if (nodes == null) return;
        for (Node n : nodes) {
            if (n != null) captureFrom(n);
        }
    }

    public void captureFrom(Node... nodes) {
        if (nodes == null) return;
        for (Node n : nodes) {
            if (n != null) captureFrom(n);
        }
    }

    // --- Command Registration ---

    /**
     * Register a TransformCommand for a single node.
     * If no before-state was captured, it silently does nothing (failsafe).
     */
    public void registerCommand(Node node, String actionName) {
        if (node == null || historyManager == null) return;

        NodeMemento before = beforeStates.remove(node);
        if (before == null) {
            // No before state captured. This is a programming error in the caller.
            System.err.println("ActionContextRecorder WARNING: No before-state found for " + node +
                    " when registering '" + actionName + "'. Did you call captureFrom()?");
            return;
        }

        NodeMemento after = new NodeMemento(node);

        // Safety: if before and after are identical, nothing changed. Skip adding to history.
        if (isMementoEqual(before, after)) {
            return;
        }

        String zone = getContextZone(node);
        TransformCommand cmd = new TransformCommand(node, before, after, zone);
        historyManager.addCommand(cmd);
    }

    /**
     * Register a batch command for all captured nodes in this session.
     * Clears the capture after registering.
     */
    public void registerBatchCommand(String actionName, Collection<? extends Node> nodes) {
        if (nodes == null || nodes.isEmpty() || historyManager == null) {
            beforeStates.clear();
            return;
        }

        org.example.pattern.CompositeCommand composite = null;

        for (Node n : nodes) {
            NodeMemento before = beforeStates.remove(n);
            if (before == null) continue;

            NodeMemento after = new NodeMemento(n);
            if (isMementoEqual(before, after)) continue;

            if (composite == null) {
                composite = new org.example.pattern.CompositeCommand(actionName);
            }
            composite.addCommand(new TransformCommand(n, before, after, getContextZone(n)));
        }

        beforeStates.clear(); // Ensure cleanup

        if (composite != null && !composite.isEmpty()) {
            historyManager.addCommand(composite);
        }
    }

    // --- Convenience: Auto-transaction with Runnable ---

    /**
     * Executes the given action and automatically captures before/after state.
     * This is the SAFEST way to ensure an action is undoable:
     * 
     *   recorder.execute("Mi Accion", node, () -> {
     *       node.setTranslateX(100);
     *   });
     * 
     * The lambda is executed, and if mutation occurred, a TransformCommand is added.
     */
    public void execute(String actionName, Node node, Runnable action) {
        if (node == null) return;
        captureFrom(node);
        try {
            action.run();
        } finally {
            registerCommand(node, actionName);
        }
    }

    /**
     * Batch version: execute an action on a collection and register a CompositeCommand.
     */
    public void executeBatch(String actionName, Collection<? extends Node> nodes, Runnable action) {
        if (nodes == null || nodes.isEmpty()) return;
        captureFrom(nodes);
        try {
            action.run();
        } finally {
            registerBatchCommand(actionName, nodes);
        }
    }

    // --- Utility ---

    public void clear() {
        beforeStates.clear();
    }

    public boolean hasCaptureFor(Node node) {
        return beforeStates.containsKey(node);
    }

    // --- Helpers ---

    private String getContextZone(Node node) {
        if (node instanceof ShapeLayer) return ((ShapeLayer) node).getActiveZone();
        if (node instanceof ImageLayer) return ((ImageLayer) node).getActiveZone();
        if (node instanceof TextLayer) return ((TextLayer) node).getActiveZone();
        return null;
    }

    private boolean isMementoEqual(NodeMemento a, NodeMemento b) {
        if (a == null || b == null) return false;
        return a.isEquivalentTo(b);
    }
}
