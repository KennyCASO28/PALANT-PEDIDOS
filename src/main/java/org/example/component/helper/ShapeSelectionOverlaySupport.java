package org.example.component.helper;

import javafx.scene.Cursor;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Shear;
import org.example.model.ShapeType;

public final class ShapeSelectionOverlaySupport {

    private ShapeSelectionOverlaySupport() {
    }

    public static OverlayNodes createOverlayNodes(Shear shearTransform, javafx.scene.Group handlesGroup) {
        javafx.scene.shape.Polygon border = new javafx.scene.shape.Polygon();
        border.setStroke(Color.web("#0047AB"));
        border.setStrokeWidth(1);
        border.setFill(null);
        border.setMouseTransparent(true);

        StackPane topLeft = createResizeHandle(Cursor.NW_RESIZE, shearTransform);
        StackPane topRight = createResizeHandle(Cursor.NE_RESIZE, shearTransform);
        StackPane bottomLeft = createResizeHandle(Cursor.SW_RESIZE, shearTransform);
        StackPane bottomRight = createResizeHandle(Cursor.SE_RESIZE, shearTransform);
        StackPane topCenter = createResizeHandle(Cursor.N_RESIZE, shearTransform);
        StackPane bottomCenter = createResizeHandle(Cursor.S_RESIZE, shearTransform);
        StackPane leftCenter = createResizeHandle(Cursor.W_RESIZE, shearTransform);
        StackPane rightCenter = createResizeHandle(Cursor.E_RESIZE, shearTransform);

        StackPane rotTopLeft = createRotationHandle(shearTransform);
        StackPane rotTopRight = createRotationHandle(shearTransform);
        StackPane rotBottomLeft = createRotationHandle(shearTransform);
        StackPane rotBottomRight = createRotationHandle(shearTransform);

        StackPane shearTop = createShearHandle(Cursor.H_RESIZE, true, shearTransform);
        StackPane shearBottom = createShearHandle(Cursor.H_RESIZE, true, shearTransform);
        StackPane shearLeft = createShearHandle(Cursor.V_RESIZE, false, shearTransform);
        StackPane shearRight = createShearHandle(Cursor.V_RESIZE, false, shearTransform);

        javafx.scene.Group pivotHandle = org.example.utils.UIFactory.crearPivotHandle();
        org.example.utils.GeometryUtility.applyAntiShear(pivotHandle, shearTransform, 5, 5);

        StackPane arcTopLeft = createArcHandle(shearTransform);
        StackPane arcTopRight = createArcHandle(shearTransform);
        StackPane arcBottomLeft = createArcHandle(shearTransform);
        StackPane arcBottomRight = createArcHandle(shearTransform);

        OverlayNodes nodes = new OverlayNodes(
                border, topLeft, topRight, bottomLeft, bottomRight,
                topCenter, bottomCenter, leftCenter, rightCenter,
                rotTopLeft, rotTopRight, rotBottomLeft, rotBottomRight,
                shearTop, shearBottom, shearLeft, shearRight,
                pivotHandle, arcTopLeft, arcTopRight, arcBottomLeft, arcBottomRight);

        handlesGroup.getChildren().addAll(
                border, topLeft, topRight, bottomLeft, bottomRight,
                topCenter, bottomCenter, leftCenter, rightCenter,
                rotTopLeft, rotTopRight, rotBottomLeft, rotBottomRight,
                shearTop, shearBottom, shearLeft, shearRight,
                pivotHandle, arcTopLeft, arcTopRight, arcBottomLeft, arcBottomRight);

        return nodes;
    }

    public static StackPane createResizeHandle(Cursor cursor, Shear shearTransform) {
        StackPane handle = org.example.utils.UIFactory.crearSquareHandle(null, 4, "#0047AB", "#ffffff", cursor);
        org.example.utils.GeometryUtility.applyAntiShear(handle, shearTransform, 2, 2);
        return handle;
    }

    public static StackPane createRotationHandle(Shear shearTransform) {
        StackPane handle = org.example.utils.UIFactory.crearIconHandle("mdi2r-rotate-right", 16, "#e8a020",
                Cursor.OPEN_HAND);
        org.example.utils.GeometryUtility.applyAntiShear(handle, shearTransform, 8, 8);
        return handle;
    }

    public static StackPane createShearHandle(Cursor cursor, boolean isHorizontal, Shear shearTransform) {
        String iconName = isHorizontal ? "mdi2a-arrow-left-right" : "mdi2a-arrow-up-down";
        StackPane handle = org.example.utils.UIFactory.crearIconHandle(iconName, 16, "#16a085", cursor);
        handle.setVisible(false);
        org.example.utils.GeometryUtility.applyAntiShear(handle, shearTransform, 8, 8);
        return handle;
    }

    public static StackPane createArcHandle(Shear shearTransform) {
        StackPane handle = new StackPane();
        handle.setPrefSize(16, 16);
        // Slightly larger than a dot (6x6) for better visibility as requested
        Rectangle rect = new Rectangle(6, 6, Color.BLACK);
        rect.setStroke(Color.WHITE);
        rect.setStrokeWidth(1.2);
        rect.setArcWidth(2); // Slightly soft corners for the handle
        rect.setArcHeight(2);

        handle.getChildren().add(rect);
        handle.setCursor(Cursor.HAND);
        handle.setPickOnBounds(true);
        // Anti-Shear to keep it clean
        org.example.utils.GeometryUtility.applyAntiShear(handle, shearTransform, 3, 3);
        return handle;
    }

    public static void updateVisuals(OverlayNodes nodes, VisualState state) {
        double width = state.width();
        double height = state.height();
        double visualMinX = state.visualMinX();
        double visualMinY = state.visualMinY();

        double shX = state.shearTransform().getX();
        double shY = state.shearTransform().getY();
        double px = state.shearTransform().getPivotX();
        double py = state.shearTransform().getPivotY();

        javafx.geometry.Point2D pTL = shearPoint(visualMinX, visualMinY, shX, shY, px, py);
        javafx.geometry.Point2D pTR = shearPoint(visualMinX + width, visualMinY, shX, shY, px, py);
        javafx.geometry.Point2D pBL = shearPoint(visualMinX, visualMinY + height, shX, shY, px, py);
        javafx.geometry.Point2D pBR = shearPoint(visualMinX + width, visualMinY + height, shX, shY, px, py);

        nodes.border().getPoints().setAll(
                pTL.getX(), pTL.getY(),
                pTR.getX(), pTR.getY(),
                pBR.getX(), pBR.getY(),
                pBL.getX(), pBL.getY());

        // selectionHandleOffset must be 6.0 because UIFactory.crearSquareHandle uses a
        // 12x12 hitArea
        double selectionHandleOffset = 6.0;
        positionSelectionHandles(nodes, pTL, pTR, pBL, pBR, selectionHandleOffset);

        // transformHandleOffset must be 8.0 because createRotationHandle uses size 16
        double transformHandleOffset = 8.0;
        positionTransformHandles(nodes, pTL, pTR, pBL, pBR, transformHandleOffset);

        nodes.border().setStroke(Color.web("#0047AB"));
        if (state.isRotationMode()) {
            setSelectionHandlesVisible(nodes, false);
            setRotationHandlesVisible(nodes, true);
            setArcHandlesVisible(nodes, false);

            double pivotX = state.customPivotX() != -1 ? state.customPivotX() : width / 2.0;
            double pivotY = state.customPivotY() != -1 ? state.customPivotY() : height / 2.0;

            // pivotHandle is placed at un-sheared coords right now
            nodes.pivotHandle().setLayoutX(visualMinX + pivotX - 5);
            nodes.pivotHandle().setLayoutY(visualMinY + pivotY - 5);
        } else if (state.isArcEditingMode() || state.isNodeEditing()) {
            setSelectionHandlesVisible(nodes, false);
            setRotationHandlesVisible(nodes, false);

            if (state.type() == ShapeType.RECTANGLE) {
                setArcHandlesVisible(nodes, true);
                double arcWidth = state.arcWidth() / 2.0;
                double arcHandleOffset = 8.0; // Half of prefSize(16, 16)

                nodes.arcTopLeft().setLayoutX(visualMinX + arcWidth - arcHandleOffset);
                nodes.arcTopLeft().setLayoutY(visualMinY + arcWidth - arcHandleOffset);

                nodes.arcTopRight().setLayoutX(visualMinX + width - arcWidth - arcHandleOffset);
                nodes.arcTopRight().setLayoutY(visualMinY + arcWidth - arcHandleOffset);

                nodes.arcBottomLeft().setLayoutX(visualMinX + arcWidth - arcHandleOffset);
                nodes.arcBottomLeft().setLayoutY(visualMinY + height - arcWidth - arcHandleOffset);

                nodes.arcBottomRight().setLayoutX(visualMinX + width - arcWidth - arcHandleOffset);
                nodes.arcBottomRight().setLayoutY(visualMinY + height - arcWidth - arcHandleOffset);
            } else {
                setArcHandlesVisible(nodes, false);
            }
        } else {
            // Normal resize mode
            setSelectionHandlesVisible(nodes, true);
            setRotationHandlesVisible(nodes, false);
            setArcHandlesVisible(nodes, false);
        }

        applyAntiScale(nodes, state.scaleTransform().getX(), state.scaleTransform().getY(), state.viewportScale());

        updatePivotTransforms(
                state.rotateTransform(),
                state.scaleTransform(),
                state.shearTransform(),
                visualMinX,
                visualMinY,
                width,
                height,
                state.customPivotX(),
                state.customPivotY());
    }

    private static javafx.geometry.Point2D shearPoint(double x, double y, double shX, double shY, double px,
            double py) {
        double newX = x + shX * (y - py);
        double newY = y + shY * (x - px);
        return new javafx.geometry.Point2D(newX, newY);
    }

    private static void positionSelectionHandles(
            OverlayNodes nodes,
            javafx.geometry.Point2D pTL,
            javafx.geometry.Point2D pTR,
            javafx.geometry.Point2D pBL,
            javafx.geometry.Point2D pBR,
            double handleOffset) {
        nodes.topLeft().setLayoutX(pTL.getX() - handleOffset);
        nodes.topLeft().setLayoutY(pTL.getY() - handleOffset);
        nodes.topRight().setLayoutX(pTR.getX() - handleOffset);
        nodes.topRight().setLayoutY(pTR.getY() - handleOffset);
        nodes.bottomLeft().setLayoutX(pBL.getX() - handleOffset);
        nodes.bottomLeft().setLayoutY(pBL.getY() - handleOffset);
        nodes.bottomRight().setLayoutX(pBR.getX() - handleOffset);
        nodes.bottomRight().setLayoutY(pBR.getY() - handleOffset);

        nodes.topCenter().setLayoutX((pTL.getX() + pTR.getX()) / 2 - handleOffset);
        nodes.topCenter().setLayoutY((pTL.getY() + pTR.getY()) / 2 - handleOffset);
        nodes.bottomCenter().setLayoutX((pBL.getX() + pBR.getX()) / 2 - handleOffset);
        nodes.bottomCenter().setLayoutY((pBL.getY() + pBR.getY()) / 2 - handleOffset);
        nodes.leftCenter().setLayoutX((pTL.getX() + pBL.getX()) / 2 - handleOffset);
        nodes.leftCenter().setLayoutY((pTL.getY() + pBL.getY()) / 2 - handleOffset);
        nodes.rightCenter().setLayoutX((pTR.getX() + pBR.getX()) / 2 - handleOffset);
        nodes.rightCenter().setLayoutY((pTR.getY() + pBR.getY()) / 2 - handleOffset);
    }

    private static void positionTransformHandles(
            OverlayNodes nodes,
            javafx.geometry.Point2D pTL,
            javafx.geometry.Point2D pTR,
            javafx.geometry.Point2D pBL,
            javafx.geometry.Point2D pBR,
            double handleOffset) {
        nodes.rotTopLeft().setLayoutX(pTL.getX() - handleOffset);
        nodes.rotTopLeft().setLayoutY(pTL.getY() - handleOffset);
        nodes.rotTopRight().setLayoutX(pTR.getX() - handleOffset);
        nodes.rotTopRight().setLayoutY(pTR.getY() - handleOffset);
        nodes.rotBottomLeft().setLayoutX(pBL.getX() - handleOffset);
        nodes.rotBottomLeft().setLayoutY(pBL.getY() - handleOffset);
        nodes.rotBottomRight().setLayoutX(pBR.getX() - handleOffset);
        nodes.rotBottomRight().setLayoutY(pBR.getY() - handleOffset);

        nodes.shearTop().setLayoutX((pTL.getX() + pTR.getX()) / 2 - handleOffset);
        nodes.shearTop().setLayoutY((pTL.getY() + pTR.getY()) / 2 - handleOffset);
        nodes.shearBottom().setLayoutX((pBL.getX() + pBR.getX()) / 2 - handleOffset);
        nodes.shearBottom().setLayoutY((pBL.getY() + pBR.getY()) / 2 - handleOffset);
        nodes.shearLeft().setLayoutX((pTL.getX() + pBL.getX()) / 2 - handleOffset);
        nodes.shearLeft().setLayoutY((pTL.getY() + pBL.getY()) / 2 - handleOffset);
        nodes.shearRight().setLayoutX((pTR.getX() + pBR.getX()) / 2 - handleOffset);
        nodes.shearRight().setLayoutY((pTR.getY() + pBR.getY()) / 2 - handleOffset);
    }

    private static void setSelectionHandlesVisible(OverlayNodes nodes, boolean visible) {
        nodes.topLeft().setVisible(visible);
        nodes.topRight().setVisible(visible);
        nodes.bottomLeft().setVisible(visible);
        nodes.bottomRight().setVisible(visible);
        nodes.topCenter().setVisible(visible);
        nodes.bottomCenter().setVisible(visible);
        nodes.leftCenter().setVisible(visible);
        nodes.rightCenter().setVisible(visible);
    }

    private static void setRotationHandlesVisible(OverlayNodes nodes, boolean visible) {
        nodes.rotTopLeft().setVisible(visible);
        nodes.rotTopRight().setVisible(visible);
        nodes.rotBottomLeft().setVisible(visible);
        nodes.rotBottomRight().setVisible(visible);
        nodes.shearTop().setVisible(visible);
        nodes.shearBottom().setVisible(visible);
        nodes.shearLeft().setVisible(visible);
        nodes.shearRight().setVisible(visible);
        nodes.pivotHandle().setVisible(visible);
    }

    private static void setArcHandlesVisible(OverlayNodes nodes, boolean visible) {
        nodes.arcTopLeft().setVisible(visible);
        nodes.arcTopRight().setVisible(visible);
        nodes.arcBottomLeft().setVisible(visible);
        nodes.arcBottomRight().setVisible(visible);
    }

    private static void applyAntiShear(OverlayNodes nodes, Shear shearTransform) {
        org.example.utils.GeometryUtility.applyAntiShear(nodes.topLeft(), shearTransform, 2, 2);
        org.example.utils.GeometryUtility.applyAntiShear(nodes.topRight(), shearTransform, 2, 2);
        org.example.utils.GeometryUtility.applyAntiShear(nodes.bottomLeft(), shearTransform, 2, 2);
        org.example.utils.GeometryUtility.applyAntiShear(nodes.bottomRight(), shearTransform, 2, 2);
        org.example.utils.GeometryUtility.applyAntiShear(nodes.topCenter(), shearTransform, 2, 2);
        org.example.utils.GeometryUtility.applyAntiShear(nodes.bottomCenter(), shearTransform, 2, 2);
        org.example.utils.GeometryUtility.applyAntiShear(nodes.leftCenter(), shearTransform, 2, 2);
        org.example.utils.GeometryUtility.applyAntiShear(nodes.rightCenter(), shearTransform, 2, 2);

        org.example.utils.GeometryUtility.applyAntiShear(nodes.rotTopLeft(), shearTransform, 5, 5);
        org.example.utils.GeometryUtility.applyAntiShear(nodes.rotTopRight(), shearTransform, 5, 5);
        org.example.utils.GeometryUtility.applyAntiShear(nodes.rotBottomLeft(), shearTransform, 5, 5);
        org.example.utils.GeometryUtility.applyAntiShear(nodes.rotBottomRight(), shearTransform, 5, 5);

        org.example.utils.GeometryUtility.applyAntiShear(nodes.shearTop(), shearTransform, 8, 8);
        org.example.utils.GeometryUtility.applyAntiShear(nodes.shearBottom(), shearTransform, 8, 8);
        org.example.utils.GeometryUtility.applyAntiShear(nodes.shearLeft(), shearTransform, 8, 8);
        org.example.utils.GeometryUtility.applyAntiShear(nodes.shearRight(), shearTransform, 8, 8);

        org.example.utils.GeometryUtility.applyAntiShear(nodes.pivotHandle(), shearTransform, 5, 5);

        org.example.utils.GeometryUtility.applyAntiShear(nodes.arcTopLeft(), shearTransform, 2, 2);
        org.example.utils.GeometryUtility.applyAntiShear(nodes.arcTopRight(), shearTransform, 2, 2);
        org.example.utils.GeometryUtility.applyAntiShear(nodes.arcBottomLeft(), shearTransform, 2, 2);
        org.example.utils.GeometryUtility.applyAntiShear(nodes.arcBottomRight(), shearTransform, 2, 2);
    }

    private static void applyAntiScale(OverlayNodes nodes, double scaleX, double scaleY, double viewportScale) {
        double totalX = Math.max(0.001, Math.abs(scaleX) * viewportScale);
        double totalY = Math.max(0.001, Math.abs(scaleY) * viewportScale);
        double invX = 1.0 / totalX;
        double invY = 1.0 / totalY;

        nodes.border().setStrokeWidth(1.0 / Math.max(totalX, totalY));

        nodes.topLeft().setScaleX(invX);
        nodes.topLeft().setScaleY(invY);
        nodes.topRight().setScaleX(invX);
        nodes.topRight().setScaleY(invY);
        nodes.bottomLeft().setScaleX(invX);
        nodes.bottomLeft().setScaleY(invY);
        nodes.bottomRight().setScaleX(invX);
        nodes.bottomRight().setScaleY(invY);
        nodes.topCenter().setScaleX(invX);
        nodes.topCenter().setScaleY(invY);
        nodes.bottomCenter().setScaleX(invX);
        nodes.bottomCenter().setScaleY(invY);
        nodes.leftCenter().setScaleX(invX);
        nodes.leftCenter().setScaleY(invY);
        nodes.rightCenter().setScaleX(invX);
        nodes.rightCenter().setScaleY(invY);

        nodes.rotTopLeft().setScaleX(invX);
        nodes.rotTopLeft().setScaleY(invY);
        nodes.rotTopRight().setScaleX(invX);
        nodes.rotTopRight().setScaleY(invY);
        nodes.rotBottomLeft().setScaleX(invX);
        nodes.rotBottomLeft().setScaleY(invY);
        nodes.rotBottomRight().setScaleX(invX);
        nodes.rotBottomRight().setScaleY(invY);

        nodes.shearTop().setScaleX(invX);
        nodes.shearTop().setScaleY(invY);
        nodes.shearBottom().setScaleX(invX);
        nodes.shearBottom().setScaleY(invY);
        nodes.shearLeft().setScaleX(invX);
        nodes.shearLeft().setScaleY(invY);
        nodes.shearRight().setScaleX(invX);
        nodes.shearRight().setScaleY(invY);

        nodes.pivotHandle().setScaleX(invX);
        nodes.pivotHandle().setScaleY(invY);

        nodes.arcTopLeft().setScaleX(invX);
        nodes.arcTopLeft().setScaleY(invY);
        nodes.arcTopRight().setScaleX(invX);
        nodes.arcTopRight().setScaleY(invY);
        nodes.arcBottomLeft().setScaleX(invX);
        nodes.arcBottomLeft().setScaleY(invY);
        nodes.arcBottomRight().setScaleX(invX);
        nodes.arcBottomRight().setScaleY(invY);
    }

    private static void updatePivotTransforms(
            Rotate rotateTransform,
            Scale scaleTransform,
            Shear shearTransform,
            double visualMinX,
            double visualMinY,
            double width,
            double height,
            double customPivotX,
            double customPivotY) {
        double defaultPivotX = visualMinX + width / 2;
        double defaultPivotY = visualMinY + height / 2;
        double pivotX = customPivotX != -1 ? visualMinX + customPivotX : defaultPivotX;
        double pivotY = customPivotY != -1 ? visualMinY + customPivotY : defaultPivotY;

        // STABILITY FILTER: Prevent tiny floating point variations from causing
        // jitter/zooms
        if (Math.abs(rotateTransform.getPivotX() - pivotX) > 0.001
                || Math.abs(rotateTransform.getPivotY() - pivotY) > 0.001) {
            rotateTransform.setPivotX(pivotX);
            rotateTransform.setPivotY(pivotY);
        }

        // Shear and Scale pivots are less sensitive but should be updated if
        // significant
        if (Math.abs(shearTransform.getPivotX() - defaultPivotX) > 0.001
                || Math.abs(shearTransform.getPivotY() - defaultPivotY) > 0.001) {
            shearTransform.setPivotX(defaultPivotX);
            shearTransform.setPivotY(defaultPivotY);
            scaleTransform.setPivotX(defaultPivotX);
            scaleTransform.setPivotY(defaultPivotY);
        }
    }

    public static final class OverlayNodes {
        public final javafx.scene.shape.Polygon border;
        public final StackPane topLeft;
        public final StackPane topRight;
        public final StackPane bottomLeft;
        public final StackPane bottomRight;
        public final StackPane topCenter;
        public final StackPane bottomCenter;
        public final StackPane leftCenter;
        public final StackPane rightCenter;
        public final StackPane rotTopLeft;
        public final StackPane rotTopRight;
        public final StackPane rotBottomLeft;
        public final StackPane rotBottomRight;
        public final StackPane shearTop;
        public final StackPane shearBottom;
        public final StackPane shearLeft;
        public final StackPane shearRight;
        public final javafx.scene.Group pivotHandle;
        public final StackPane arcTopLeft;
        public final StackPane arcTopRight;
        public final StackPane arcBottomLeft;
        public final StackPane arcBottomRight;

        public OverlayNodes(
                javafx.scene.shape.Polygon border,
                StackPane topLeft,
                StackPane topRight,
                StackPane bottomLeft,
                StackPane bottomRight,
                StackPane topCenter,
                StackPane bottomCenter,
                StackPane leftCenter,
                StackPane rightCenter,
                StackPane rotTopLeft,
                StackPane rotTopRight,
                StackPane rotBottomLeft,
                StackPane rotBottomRight,
                StackPane shearTop,
                StackPane shearBottom,
                StackPane shearLeft,
                StackPane shearRight,
                javafx.scene.Group pivotHandle,
                StackPane arcTopLeft,
                StackPane arcTopRight,
                StackPane arcBottomLeft,
                StackPane arcBottomRight) {
            this.border = border;
            this.topLeft = topLeft;
            this.topRight = topRight;
            this.bottomLeft = bottomLeft;
            this.bottomRight = bottomRight;
            this.topCenter = topCenter;
            this.bottomCenter = bottomCenter;
            this.leftCenter = leftCenter;
            this.rightCenter = rightCenter;
            this.rotTopLeft = rotTopLeft;
            this.rotTopRight = rotTopRight;
            this.rotBottomLeft = rotBottomLeft;
            this.rotBottomRight = rotBottomRight;
            this.shearTop = shearTop;
            this.shearBottom = shearBottom;
            this.shearLeft = shearLeft;
            this.shearRight = shearRight;
            this.pivotHandle = pivotHandle;
            this.arcTopLeft = arcTopLeft;
            this.arcTopRight = arcTopRight;
            this.arcBottomLeft = arcBottomLeft;
            this.arcBottomRight = arcBottomRight;
        }

        public javafx.scene.shape.Polygon border() {
            return border;
        }

        public StackPane topLeft() {
            return topLeft;
        }

        public StackPane topRight() {
            return topRight;
        }

        public StackPane bottomLeft() {
            return bottomLeft;
        }

        public StackPane bottomRight() {
            return bottomRight;
        }

        public StackPane topCenter() {
            return topCenter;
        }

        public StackPane bottomCenter() {
            return bottomCenter;
        }

        public StackPane leftCenter() {
            return leftCenter;
        }

        public StackPane rightCenter() {
            return rightCenter;
        }

        public StackPane rotTopLeft() {
            return rotTopLeft;
        }

        public StackPane rotTopRight() {
            return rotTopRight;
        }

        public StackPane rotBottomLeft() {
            return rotBottomLeft;
        }

        public StackPane rotBottomRight() {
            return rotBottomRight;
        }

        public StackPane shearTop() {
            return shearTop;
        }

        public StackPane shearBottom() {
            return shearBottom;
        }

        public StackPane shearLeft() {
            return shearLeft;
        }

        public StackPane shearRight() {
            return shearRight;
        }

        public javafx.scene.Group pivotHandle() {
            return pivotHandle;
        }

        public StackPane arcTopLeft() {
            return arcTopLeft;
        }

        public StackPane arcTopRight() {
            return arcTopRight;
        }

        public StackPane arcBottomLeft() {
            return arcBottomLeft;
        }

        public StackPane arcBottomRight() {
            return arcBottomRight;
        }
    }

    public static final class VisualState {
        private final double width;
        private final double height;
        private final double visualMinX;
        private final double visualMinY;
        private final boolean isNodeEditing;
        private final boolean isRotationMode;
        private final boolean isArcEditingMode;
        private final boolean isLocked;
        private final ShapeType type;
        private final double arcWidth;
        private final double customPivotX;
        private final double customPivotY;
        private final Rotate rotateTransform;
        private final Scale scaleTransform;
        private final Shear shearTransform;
        private final double viewportScale;

        public VisualState(
                double width,
                double height,
                double visualMinX,
                double visualMinY,
                boolean isNodeEditing,
                boolean isRotationMode,
                boolean isArcEditingMode,
                boolean isLocked,
                ShapeType type,
                double arcWidth,
                double customPivotX,
                double customPivotY,
                Rotate rotateTransform,
                Scale scaleTransform,
                Shear shearTransform,
                double viewportScale) {
            this.width = width;
            this.height = height;
            this.visualMinX = visualMinX;
            this.visualMinY = visualMinY;
            this.isNodeEditing = isNodeEditing;
            this.isRotationMode = isRotationMode;
            this.isArcEditingMode = isArcEditingMode;
            this.isLocked = isLocked;
            this.type = type;
            this.arcWidth = arcWidth;
            this.customPivotX = customPivotX;
            this.customPivotY = customPivotY;
            this.rotateTransform = rotateTransform;
            this.scaleTransform = scaleTransform;
            this.shearTransform = shearTransform;
            this.viewportScale = viewportScale;
        }

        public double width() {
            return width;
        }

        public double height() {
            return height;
        }

        public double visualMinX() {
            return visualMinX;
        }

        public double visualMinY() {
            return visualMinY;
        }

        public boolean isNodeEditing() {
            return isNodeEditing;
        }

        public boolean isRotationMode() {
            return isRotationMode;
        }

        public boolean isArcEditingMode() {
            return isArcEditingMode;
        }

        public boolean isLocked() {
            return isLocked;
        }

        public ShapeType type() {
            return type;
        }

        public double arcWidth() {
            return arcWidth;
        }

        public double customPivotX() {
            return customPivotX;
        }

        public double customPivotY() {
            return customPivotY;
        }

        public Rotate rotateTransform() {
            return rotateTransform;
        }

        public Scale scaleTransform() {
            return scaleTransform;
        }

        public Shear shearTransform() {
            return shearTransform;
        }

        public double viewportScale() {
            return viewportScale;
        }
    }
}
