package org.example.utils;

import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
// import javafx.scene.control.Label; // REMOVED unused
import javafx.scene.layout.StackPane;
import javafx.scene.control.CheckBox;
import javafx.scene.control.cell.PropertyValueFactory;
// import javafx.util.Callback; // REMOVED unused
import javafx.geometry.Insets;

/**
 * Utility class to create TableColumns with strict alignment/styling.
 */
public class TableUtils {

    /**
     * Creates a standard text column.
     */
    public static <S, T> TableColumn<S, T> createColumn(String title, String property, double width, Pos alignment) {
        TableColumn<S, T> col = new TableColumn<>(title);
        if (property != null && !property.isEmpty()) {
            col.setCellValueFactory(new PropertyValueFactory<>(property));
        }
        col.setPrefWidth(width);

        applyAlignment(col, alignment);

        return col;
    }

    /**
     * Creates a centered CheckBox column.
     */
    /**
     * Creates a centered CheckBox column with bidirectional binding support.
     */
    public static <S> TableColumn<S, Boolean> createCheckBoxColumn(String title, String property, double width,
            java.util.function.BiConsumer<S, Boolean> setter) {
        TableColumn<S, Boolean> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>(property));
        col.setPrefWidth(width);

        applyAlignment(col, Pos.CENTER);

        col.setCellFactory(column -> new TableCell<S, Boolean>() {
            private final CheckBox cb = new CheckBox();
            private final StackPane container = new StackPane(cb);

            {
                // Force container to fill cell to guarantee centering
                container.setAlignment(Pos.CENTER);
                container.prefWidthProperty().bind(widthProperty());

                // Inherit alignment style
                getStyleClass().add("col-center");
                setAlignment(Pos.CENTER);
                setPadding(Insets.EMPTY);

                cb.setCursor(javafx.scene.Cursor.HAND);

                // Two-way binding logic
                cb.setOnAction(e -> {
                    if (getTableRow() != null) {
                        S item = getTableRow().getItem();
                        if (item != null && setter != null) {
                            setter.accept(item, cb.isSelected());
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    // Temporarily remove listener if we were using property listener
                    // But with onAction it's safer.
                    cb.setSelected(item);
                    setGraphic(container);
                }
            }
        });

        return col;
    }

    private static void applyAlignment(TableColumn<?, ?> col, Pos alignment) {
        // Strict Styling for Alignment
        if (alignment == Pos.CENTER) {
            col.getStyleClass().add("col-center");
        } else if (alignment == Pos.CENTER_RIGHT) {
            col.getStyleClass().add("col-right");
        } else {
            col.getStyleClass().add("col-left");
        }

        // Use a StackPane to enforce alignment on the Header (Nuclear Option)
        javafx.scene.control.Label lblHeader = new javafx.scene.control.Label(col.getText());
        lblHeader.setMaxWidth(Double.MAX_VALUE);
        lblHeader.setAlignment(Pos.CENTER);
        lblHeader.setStyle("-fx-padding: 0;"); // Remove padding from label itself

        StackPane headerContainer = new StackPane(lblHeader);
        headerContainer.setAlignment(Pos.CENTER); // Force center in container

        // Bind container width to column width to ensure it fills the space
        // This allows the StackPane to handle the centering logic independent of the
        // TableColumnHeader skin
        // We subtract a small amount to avoid scrollbar jitter or border overlap
        headerContainer.prefWidthProperty().bind(col.widthProperty().subtract(10));

        col.setGraphic(headerContainer);
        col.setText(""); // Clear text to rely on graphic
    }
}

