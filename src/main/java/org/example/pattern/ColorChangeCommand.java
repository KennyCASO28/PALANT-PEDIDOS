package org.example.pattern;

import javafx.scene.paint.Color;
import java.util.function.Consumer;

public class ColorChangeCommand implements ICommand {
    private final String name;
    private final Color oldColor;
    private final Color newColor;
    private final Consumer<Color> setter;
    private final Runnable customRedoLogic; // Optional side effects

    public ColorChangeCommand(String name, Color oldColor, Color newColor, Consumer<Color> setter) {
        this(name, oldColor, newColor, setter, null);
    }

    public ColorChangeCommand(String name, Color oldColor, Color newColor, Consumer<Color> setter,
            Runnable customRedoLogic) {
        this.name = name;
        this.oldColor = oldColor;
        this.newColor = newColor;
        this.setter = setter;
        this.customRedoLogic = customRedoLogic;
    }

    @Override
    public void execute() {
        setter.accept(newColor);
        if (customRedoLogic != null)
            customRedoLogic.run();
    }

    @Override
    public void undo() {
        setter.accept(oldColor);
    }

    @Override
    public void redo() {
        execute();
    }

    @Override
    public String getName() {
        return "Color Change: " + name;
    }
}

