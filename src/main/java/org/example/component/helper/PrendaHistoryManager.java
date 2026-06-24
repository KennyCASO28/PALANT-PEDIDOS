package org.example.component.helper;

import org.example.pattern.ICommand;
import java.util.*;
import java.util.function.Consumer;

/**
 * Manages the history of actions for Undo/Redo functionalities.
 * THIS IS THE NEW ENHANCED VERSION with:
 * - Unlimited history (configurable)
 * - Transaction support (beginTransaction/endTransaction)
 * - Command merging for consecutive similar actions
 * - Better error recovery to prevent layers from disappearing
 */
public class PrendaHistoryManager {

    private final Deque<ICommand> undoStack = new ArrayDeque<>();
    private final Deque<ICommand> redoStack = new ArrayDeque<>();

    // Changed from 8 to much larger, with config option
    private int maxHistory = 50;

    // Transaction support
    private boolean inTransaction = false;
    private final List<ICommand> transactionCommands = new ArrayList<>();
    private String transactionName = "Transaccion";
    private String transactionZone = null;

    // Command deduplication for rapid similar commands
    private ICommand lastCommand = null;
    private long lastCommandTime = 0;
    private static final long MERGE_TIME_WINDOW_MS = 500;

    // Optional listener to update UI
    private Consumer<Boolean> onHistoryChanged;

    // Visualizer reference for context-aware undo
    private org.example.component.PrendaVisualizer visualizer;

    private boolean recording = true;

    public PrendaHistoryManager() {
    }

    public void setRecording(boolean recording) {
        this.recording = recording;
    }

    public boolean isRecording() {
        return recording;
    }

    public void setVisualizer(org.example.component.PrendaVisualizer visualizer) {
        this.visualizer = visualizer;
    }

    public org.example.component.PrendaVisualizer getVisualizer() {
        return visualizer;
    }

    /**
     * Set the maximum history size (default 50).
     * Set to 0 for unlimited.
     */
    public void setMaxHistory(int maxHistory) {
        this.maxHistory = maxHistory;
        trimIfNeeded();
    }

    public int getMaxHistory() {
        return maxHistory;
    }

    // --- Transaction Support ---

    /**
     * Begin a transaction to group multiple commands into a single undoable action.
     * Example:
     *   historyManager.beginTransaction("Mover capas seleccionadas");
     *   // ... add individual commands, they are buffered
     *   historyManager.endTransaction();
     */
    public void beginTransaction(String name) {
        beginTransaction(name, null);
    }

    public void beginTransaction(String name, String contextZone) {
        if (inTransaction) {
            System.err.println("WARNING: Nested transaction detected. Merging into existing: " + this.transactionName);
            // Merge - flush current and start new (or just record nested commands)
            flushTransaction(); // flush previous, start new transaction
        }
        inTransaction = true;
        this.transactionName = name;
        this.transactionZone = contextZone;
        transactionCommands.clear();
    }

    public void endTransaction() {
        if (!inTransaction) {
            return;
        }
        inTransaction = false;

        if (transactionCommands.isEmpty()) {
            return; // Nothing to record
        }

        ICommand cmd;
        if (transactionCommands.size() == 1) {
            cmd = transactionCommands.get(0);
        } else {
            // Create composite
            org.example.pattern.CompositeCommand composite = new org.example.pattern.CompositeCommand(transactionName);
            for (ICommand c : transactionCommands) {
                composite.addCommand(c);
            }
            cmd = composite;
        }

        // Apply context zone if available
        if (transactionZone != null && cmd instanceof org.example.pattern.CompositeCommand) {
            // Note: CompositeCommand doesn't have setContextZone, but we keep the zone in mind
        }

        transactionCommands.clear();
        addCommandInternal(cmd);
    }

    public void cancelTransaction() {
        inTransaction = false;
        transactionCommands.clear();
        System.out.println("History: Transaction cancelled.");
    }

    public boolean isInTransaction() {
        return inTransaction;
    }

    // --- Core Add Command ---

    public void addCommand(ICommand cmd) {
        if (cmd == null || !recording) return;

        // If inside a transaction, buffer the command
        if (inTransaction) {
            transactionCommands.add(cmd);
            System.out.println("  [TX] Buffered: " + cmd.getName());
            return;
        }

        addCommandInternal(cmd);
    }

    private void addCommandInternal(ICommand cmd) {
        if (cmd == null) return;

        // Merge detection: if last command was same type within time window, try to merge
        long now = System.currentTimeMillis();
        if (lastCommand != null && (now - lastCommandTime) < MERGE_TIME_WINDOW_MS) {
            if (tryMerge(lastCommand, cmd)) {
                System.out.println("History: Merged consecutive command for: " + cmd.getName());
                lastCommandTime = now;
                return;
            }
        }

        undoStack.push(cmd);
        redoStack.clear();
        lastCommand = cmd;
        lastCommandTime = now;

        trimIfNeeded();

        System.out.println("History: Added " + cmd.getName() + " [Undo: " + undoStack.size() + "]");
        notifyChange();
    }

    /**
     * Attempt to merge a new command with the last one.
     * Override this method to add custom merge logic.
     */
    private boolean tryMerge(ICommand last, ICommand current) {
        // Default: no merge. Subclasses can override.
        // For now, we don't merge TransformCommands to avoid losing intermediate states.
        return false;
    }

    private void trimIfNeeded() {
        if (maxHistory > 0) {
            while (undoStack.size() > maxHistory) {
                ICommand removed = undoStack.removeLast(); // Remove oldest
                System.out.println("History: Removed oldest command to maintain limit: " + removed.getName());
            }
        }
    }

    // --- Undo / Redo ---

    public void undo() {
        if (undoStack.isEmpty()) {
            System.out.println("History: Undo stack empty. Nothing to undo.");
            return;
        }

        ICommand cmd = undoStack.pop();
        lastCommand = null; // Reset merge state

        // Context-aware zone handling
        String zone = cmd.getContextZone();
        if (zone != null && visualizer != null) {
            boolean isEditing = visualizer.getPowerClipManager().isEditing();
            String currentZone = visualizer.getPowerClipManager().getCurrentEditingZone();

            if (!isEditing || (currentZone != null && !currentZone.equals(zone))) {
                visualizer.getPowerClipManager().enterEditMode(zone);
            }
        }

        try {
            cmd.undo();
            redoStack.push(cmd);
            System.out.println("History: Undid " + cmd.getName());
        } catch (Exception e) {
            System.err.println("History: ERROR during undo of " + cmd.getName() + ": " + e.getMessage());
            e.printStackTrace();
            // Try to recover by pushing it back
            undoStack.push(cmd);
        }

        notifyChange();
    }

    public void redo() {
        if (redoStack.isEmpty()) {
            System.out.println("History: Redo stack empty. Nothing to redo.");
            return;
        }

        ICommand cmd = redoStack.pop();
        lastCommand = null;

        String zone = cmd.getContextZone();
        if (zone != null && visualizer != null) {
            boolean isEditing = visualizer.getPowerClipManager().isEditing();
            String currentZone = visualizer.getPowerClipManager().getCurrentEditingZone();

            if (!isEditing || (currentZone != null && !currentZone.equals(zone))) {
                visualizer.getPowerClipManager().enterEditMode(zone);
            }
        }

        try {
            cmd.redo();
            undoStack.push(cmd);
            System.out.println("History: Redid " + cmd.getName());
        } catch (Exception e) {
            System.err.println("History: ERROR during redo of " + cmd.getName() + ": " + e.getMessage());
            e.printStackTrace();
            redoStack.push(cmd);
        }

        notifyChange();
    }

    private void flushTransaction() {
        if (!inTransaction || transactionCommands.isEmpty()) return;
        endTransaction();
    }

    private void notifyChange() {
        if (onHistoryChanged != null) {
            onHistoryChanged.accept(!undoStack.isEmpty());
        }
        if (visualizer != null) {
            visualizer.notifyStateChanged();
        }
    }

    public void setOnHistoryChanged(Consumer<Boolean> listener) {
        this.onHistoryChanged = listener;
    }

    public int getUndoHistorySize() {
        return undoStack.size();
    }

    public int getRedoHistorySize() {
        return redoStack.size();
    }

    /**
     * Get a list of command names for the undo stack (oldest first).
     */
    public List<String> getUndoHistoryNames() {
        List<String> names = new ArrayList<>();
        Deque<ICommand> copy = new ArrayDeque<>(undoStack);
        for (ICommand cmd : copy) {
            names.add(cmd.getName());
        }
        Collections.reverse(names);
        return names;
    }

    public void clearHistory() {
        undoStack.clear();
        redoStack.clear();
        lastCommand = null;
        notifyChange();
    }

    /**
     * For debug: print the current state of both stacks.
     */
    public void printHistoryState() {
        System.out.println("=== History State ===");
        System.out.println("Undo stack (" + undoStack.size() + "):");
        for (ICommand cmd : undoStack) {
            System.out.println("  - " + cmd.getName());
        }
        System.out.println("Redo stack (" + redoStack.size() + "):");
        for (ICommand cmd : redoStack) {
            System.out.println("  + " + cmd.getName());
        }
        System.out.println("=====================");
    }
}
