package org.example.controller.helper;

import javafx.scene.control.TableView;
import javafx.scene.layout.*;
import org.example.model.DetallePedido;

public class TableLayoutHelper {

    /**
     * Replaces the FXML-provided table with a programmatic clean table,
     * maintaining all original layout constraints (VBox, HBox, AnchorPane, GridPane).
     *
     * @param oldTable   The table defined in FXML.
     * @param cleanTable The dynamically generated table that will replace it.
     * @return true if successful, false otherwise.
     */
    public static boolean replaceTableInParent(TableView<DetallePedido> oldTable, TableView<DetallePedido> cleanTable) {
        if (oldTable == null || cleanTable == null) return false;

        try {
            Pane parent = (Pane) oldTable.getParent();
            if (parent != null) {
                int idx = parent.getChildren().indexOf(oldTable);

                System.out.println("DEBUG: Table Parent is " + parent.getClass().getSimpleName());
                System.out.println("DEBUG: Table Index in Parent is " + idx);

                // 1. Copy VBox/HBox Priorities
                if (parent instanceof VBox) {
                    VBox.setVgrow(cleanTable, VBox.getVgrow(oldTable));
                    if (VBox.getVgrow(cleanTable) == null)
                        VBox.setVgrow(cleanTable, Priority.ALWAYS);
                } else if (parent instanceof HBox) {
                    HBox.setHgrow(cleanTable, HBox.getHgrow(oldTable));
                    if (HBox.getHgrow(cleanTable) == null)
                        HBox.setHgrow(cleanTable, Priority.ALWAYS);
                }

                // 2. Copy AnchorPane Constraints
                if (parent instanceof AnchorPane) {
                    Double top = AnchorPane.getTopAnchor(oldTable);
                    Double bottom = AnchorPane.getBottomAnchor(oldTable);
                    Double left = AnchorPane.getLeftAnchor(oldTable);
                    Double right = AnchorPane.getRightAnchor(oldTable);

                    if (top != null) AnchorPane.setTopAnchor(cleanTable, top);
                    if (bottom != null) AnchorPane.setBottomAnchor(cleanTable, bottom);
                    if (left != null) AnchorPane.setLeftAnchor(cleanTable, left);
                    if (right != null) AnchorPane.setRightAnchor(cleanTable, right);
                }

                // 3. Copy GridPane Constraints
                if (parent instanceof GridPane) {
                    Integer row = GridPane.getRowIndex(oldTable);
                    Integer col = GridPane.getColumnIndex(oldTable);
                    Integer rowSpan = GridPane.getRowSpan(oldTable);
                    Integer colSpan = GridPane.getColumnSpan(oldTable);

                    if (row != null) GridPane.setRowIndex(cleanTable, row);
                    if (col != null) GridPane.setColumnIndex(cleanTable, col);
                    if (rowSpan != null) GridPane.setRowSpan(cleanTable, rowSpan);
                    if (colSpan != null) GridPane.setColumnSpan(cleanTable, colSpan);

                    Priority hGrow = GridPane.getHgrow(oldTable);
                    Priority vGrow = GridPane.getVgrow(oldTable);
                    if (hGrow != null) GridPane.setHgrow(cleanTable, hGrow);
                    if (vGrow != null) GridPane.setVgrow(cleanTable, vGrow);
                }

                // 4. Copy Size Constraints
                cleanTable.setMaxWidth(oldTable.getMaxWidth());
                cleanTable.setMaxHeight(oldTable.getMaxHeight());
                cleanTable.setPrefWidth(oldTable.getPrefWidth());
                cleanTable.setPrefHeight(oldTable.getPrefHeight());

                // Replace in view
                parent.getChildren().set(idx, cleanTable);
                System.out.println("DEBUG: SWAP COMPLETED SUCCESSFULLY - OLD TABLE REMOVED");
                return true;
            }
        } catch (Exception e) {
            System.err.println("Error replacing table: " + e.getMessage());
        }
        return false;
    }
}
