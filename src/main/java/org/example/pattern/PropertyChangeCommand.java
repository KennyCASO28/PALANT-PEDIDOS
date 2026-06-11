package org.example.pattern;

import java.util.function.Consumer;

/**
 * A generic command for undoing/redoing simple property changes.
 * 
 * @param <T> The type of the property being changed.
 */
public class PropertyChangeCommand<T> implements ICommand {
    private final String name;
    private final T oldValue;
    private final T newValue;
    private final Consumer<T> setter;
    private final Runnable customAction; // Optional side effects

    public PropertyChangeCommand(String name, T oldValue, T newValue, Consumer<T> setter) {
        this(name, oldValue, newValue, setter, null);
    }

    public PropertyChangeCommand(String name, T oldValue, T newValue, Consumer<T> setter, Runnable customAction) {
        this.name = name;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.setter = setter;
        this.customAction = customAction;
    }

    @Override
    public void execute() {
        setter.accept(newValue);
        if (customAction != null) {
            customAction.run();
        }
    }

    @Override
    public void undo() {
        setter.accept(oldValue);
        if (customAction != null) {
            customAction.run();
        }
    }

    @Override
    public void redo() {
        execute();
    }

    @Override
    public String getName() {
        return name;
    }
}

