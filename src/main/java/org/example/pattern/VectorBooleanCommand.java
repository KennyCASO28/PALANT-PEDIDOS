package org.example.pattern;

import javafx.scene.Node;
import org.example.component.UserLayerManager;
import java.util.List;
import java.util.ArrayList;

public class VectorBooleanCommand implements ICommand {
    public enum ActionType {
        WELD, UNWELD
    }

    private final UserLayerManager manager;
    private final ActionType type;
    private final List<Node> sourceNodes;
    private final List<Node> resultNodes;
    private final String contextZone;

    private final List<NodeMemento> sourceBeforeState = new ArrayList<>();
    private final List<NodeMemento> resultAfterState = new ArrayList<>();

    public VectorBooleanCommand(UserLayerManager manager, List<Node> sourceNodes, List<Node> resultNodes, ActionType type, String contextZone, List<NodeMemento> preCapturedSourceStates) {
        this.manager = manager;
        this.type = type;
        this.sourceNodes = new ArrayList<>(sourceNodes);
        this.resultNodes = new ArrayList<>(resultNodes);
        this.contextZone = contextZone;

        // Capture BEFORE state for source nodes (the things that disappear)
        if (preCapturedSourceStates != null) {
            this.sourceBeforeState.addAll(preCapturedSourceStates);
        } else {
            for (Node n : this.sourceNodes) {
                this.sourceBeforeState.add(new NodeMemento(n));
            }
        }
        
        // Capture AFTER state for result nodes (the things that appear)
        for (Node n : this.resultNodes) {
            this.resultAfterState.add(new NodeMemento(n));
        }
    }

    @Override
    public void execute() {
        boolean wasHistory = manager.isPerformingHistoryAction();
        manager.setPerformingHistoryAction(true);
        manager.suspendNotifications();
        try {
            // Re-apply the operation: remove sources, restore results
            for (Node n : sourceNodes) {
                manager.removeLayer(n);
                if (n.getParent() instanceof javafx.scene.Group) {
                    ((javafx.scene.Group) n.getParent()).getChildren().remove(n);
                }
            }
            
            for (NodeMemento m : resultAfterState) {
                m.restore();
            }
            for (Node n : resultNodes) {
                manager.trackRestoredLayer(n);
            }
            
            manager.clearSelection();
            for (Node n : resultNodes) {
                manager.addToSelection(n);
            }
            
        } finally {
            manager.setPerformingHistoryAction(wasHistory);
            manager.resumeNotifications();
        }
    }

    @Override
    public void undo() {
        boolean wasHistory = manager.isPerformingHistoryAction();
        manager.setPerformingHistoryAction(true);
        manager.suspendNotifications();
        try {
            // Undo operation: remove results, restore sources
            for (Node n : resultNodes) {
                manager.removeLayer(n);
                if (n.getParent() instanceof javafx.scene.Group) {
                    ((javafx.scene.Group) n.getParent()).getChildren().remove(n);
                }
            }
            
            for (NodeMemento m : sourceBeforeState) {
                m.restore();
            }
            for (Node n : sourceNodes) {
                manager.trackRestoredLayer(n);
            }
            
            manager.clearSelection();
            for (Node n : sourceNodes) {
                manager.addToSelection(n);
            }
            
        } finally {
            manager.setPerformingHistoryAction(wasHistory);
            manager.resumeNotifications();
        }
    }

    @Override
    public void redo() {
        execute();
    }

    @Override
    public String getName() {
        return type == ActionType.WELD ? "Soldar Vectores" : "Desoldar Vectores";
    }

    @Override
    public String getContextZone() {
        return contextZone;
    }
}
