package org.example.pattern;

import javafx.scene.Node;
import org.example.component.UserLayerManager;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

public class LayerActionCommand implements ICommand {
    public enum ActionType {
        ADD, REMOVE
    }

    private final UserLayerManager manager;
    private final List<Node> layers;
    private final ActionType type;
    private final String contextZone;
    private final List<NodeMemento> beforeStates = new ArrayList<>();

    public LayerActionCommand(UserLayerManager manager, Node layer, ActionType type) {
        this(manager, Collections.singletonList(layer), type, null);
    }

    public LayerActionCommand(UserLayerManager manager, List<Node> layers, ActionType type, String contextZone) {
        this.manager = manager;
        this.layers = new ArrayList<>(layers);
        this.type = type;
        this.contextZone = contextZone;

        for (Node n : this.layers) {
            this.beforeStates.add(new NodeMemento(n));
        }
    }

    @Override
    public void execute() {
        boolean wasHistory = manager.isPerformingHistoryAction();
        manager.setPerformingHistoryAction(true);
        try {
            if (type == ActionType.ADD) {
                for (NodeMemento m : beforeStates) {
                    m.restore();
                }
                for (Node n : layers) {
                    manager.trackRestoredLayer(n);
                }
            } else {
                for (Node n : layers) {
                    manager.removeLayer(n);
                }
            }
        } finally {
            manager.setPerformingHistoryAction(wasHistory);
        }
    }

    @Override
    public void undo() {
        boolean wasHistory = manager.isPerformingHistoryAction();
        manager.setPerformingHistoryAction(true);
        try {
            if (type == ActionType.ADD) {
                for (Node n : layers) {
                    manager.removeLayer(n);
                }
            } else {
                // It was removed. Restore exact parent, position, and tracking.
                for (NodeMemento m : beforeStates) {
                    m.restore();
                }
                for (Node n : layers) {
                    manager.trackRestoredLayer(n);
                }
            }
        } finally {
            manager.setPerformingHistoryAction(wasHistory);
        }
    }

    @Override
    public void redo() {
        execute();
    }

    @Override
    public String getName() {
        String base = (type == ActionType.ADD ? "Añadir" : "Eliminar");
        return base + (layers.size() > 1 ? " Múltiples Objetos" : " Objeto");
    }

    @Override
    public String getContextZone() {
        return contextZone;
    }
}

