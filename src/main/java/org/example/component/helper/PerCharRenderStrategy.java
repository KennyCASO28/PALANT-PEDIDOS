package org.example.component.helper;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;
import org.example.component.RenderContext;
import org.example.component.TextRenderStrategy;
import org.example.component.TextLayer;
import org.example.model.TrajectoryPath;

/**
 * Per-character rendering strategy (the heavy but flexible approach).
 * Used for curved trajectories (arc, wave, circle) where each character
 * must be positioned and rotated independently.
 */
public class PerCharRenderStrategy implements TextRenderStrategy {

    @Override
    public void render(TextLayer textLayer, Group textGroup, RenderContext ctx) {
        // Clear existing content (assumed to be handled by caller before render)
        // This implementation focuses on creating character stacks.
        // The caller (TextLayer.renderText) handles clearing.

        String text = ctx.getTextContent();
        if (text == null || text.isEmpty()) return;

        char[] chars = text.toCharArray();
        double[] charWidths = new double[chars.length];
        Font font = ctx.getFont();
        double totalTextWidth = 0;

        for (int i = 0; i < chars.length; i++) {
            Text t = new Text(String.valueOf(chars[i]));
            t.setFont(font);
            charWidths[i] = t.getLayoutBounds().getWidth();
            totalTextWidth += charWidths[i];
        }

        double logicalWidth = ctx.getLogicalWidth();
        double logicalHeight = ctx.getLogicalHeight();
        double spacing = ctx.getSpacing();

        // Scale characters to fit logical width (no spacing in denominator to avoid feedback loop)
        double scaleX = logicalWidth / Math.max(1, totalTextWidth);
        double scaleY = logicalHeight / Math.max(1, font.getSize());

        // Sync trajectory to logical bounds if it's a primitive
        TrajectoryPath trajectory = ctx.getTrajectory();
        if (trajectory != null) {
            if (trajectory.getType() == org.example.model.TrajectoryPath.Type.STRAIGHT) {
                trajectory.getControlPoints().set(0, new javafx.geometry.Point2D(-logicalWidth / 2.0, 0));
                trajectory.getControlPoints().set(1, new javafx.geometry.Point2D(logicalWidth / 2.0, 0));
            } else if (trajectory.getType() == org.example.model.TrajectoryPath.Type.CIRCLE) {
                scaleX = scaleY; // Use uniform scaling to preserve aspect ratio
                double radius = Math.min(logicalWidth, logicalHeight) / 2.0;
                trajectory.getControlPoints().set(0, new javafx.geometry.Point2D(0, 0));
                trajectory.getControlPoints().set(1, new javafx.geometry.Point2D(radius, 0));
                
                double pathLenCircle = 2 * Math.PI * radius;
                double spanWithUniformScale = totalTextWidth * scaleX + (chars.length - 1) * spacing;
                
                if (spanWithUniformScale > pathLenCircle * 0.95) {
                    // Uniformly squeeze it so it fits the circumference without overlapping
                    double squeeze = (pathLenCircle * 0.95) / spanWithUniformScale;
                    scaleX *= squeeze;
                    scaleY *= squeeze;
                }
            }
        }

        // Total span the text occupies in scaled coordinates (chars + spacing)
        double totalSpan = totalTextWidth * scaleX + (chars.length - 1) * spacing;
        double pathLen = trajectory != null ? trajectory.getTotalLength() : totalSpan;

        // Center the text block along the path
        double startOffset = 0;
        if (totalSpan < pathLen) {
            startOffset = (pathLen - totalSpan) / 2.0;
        }

        double currentPos = startOffset;

        for (int i = 0; i < chars.length; i++) {
            Group charStack = createCharStack(chars[i], scaleX, scaleY, ctx);
            double cw = charWidths[i] * scaleX;
            double charCenter = currentPos + cw / 2.0;
            double t = (pathLen > 0) ? charCenter / pathLen : 0.5;
            javafx.geometry.Point2D pos;

            if (trajectory != null && trajectory.getType() == org.example.model.TrajectoryPath.Type.CIRCLE) {
                double tOval = (t + 0.75) % 1.0;
                double angleRad = tOval * 2 * Math.PI;
                pos = new javafx.geometry.Point2D(
                        Math.cos(angleRad) * (logicalWidth / 2.0),
                        Math.sin(angleRad) * (logicalHeight / 2.0)
                );
            } else {
                pos = trajectory != null ? trajectory.getPointAt(t) : new javafx.geometry.Point2D(0, 0);
            }

            charStack.setTranslateX(pos.getX());
            charStack.setTranslateY(pos.getY());

            if (trajectory != null && trajectory.isAutoRotate()) {
                double angle;
                if (trajectory.getType() == org.example.model.TrajectoryPath.Type.CIRCLE) {
                    angle = Math.toDegrees(Math.atan2(pos.getY(), pos.getX())) + 90;
                } else {
                    double dt = 0.01;
                    double tNext = Math.min(1.0, t + dt);
                    if (tNext == t) tNext = Math.max(0, t - dt);
                    javafx.geometry.Point2D p1 = trajectory.getPointAt(tNext);
                    javafx.geometry.Point2D p0 = pos;
                    angle = Math.toDegrees(Math.atan2(p1.getY() - p0.getY(), p1.getX() - p0.getX()));
                    if (tNext < t) angle += 180;
                }
                charStack.setRotate(angle);
            }
            textGroup.getChildren().add(charStack);
            currentPos += cw + spacing;
        }
    }

    private Group createCharStack(char c, double sx, double sy, RenderContext ctx) {
        javafx.scene.text.Font font = ctx.getFont();
        javafx.scene.paint.Color textColor = ctx.getTextColor();
        javafx.scene.paint.Color strokeColor = ctx.getStrokeColor();
        double strokeWidth = ctx.getStrokeWidth();

        Text fill = new Text(String.valueOf(c));
        fill.setFont(font);
        fill.setFill(textColor);
        fill.setTextOrigin(javafx.geometry.VPos.CENTER);
        fill.getTransforms().add(new Scale(sx, sy, 0, 0));

        Group g = new Group();
        if (strokeWidth > 0) {
            Text outer = new Text(String.valueOf(c));
            outer.setFont(font);
            outer.setFill(null);
            outer.setStroke(strokeColor);
            outer.setStrokeType(javafx.scene.shape.StrokeType.OUTSIDE);
            outer.setStrokeWidth(strokeWidth);
            outer.setTextOrigin(javafx.geometry.VPos.CENTER);
            outer.getTransforms().add(new Scale(sx, sy, 0, 0));
            g.getChildren().add(outer);
        }
        g.getChildren().add(fill);
        return g;
    }

    @Override
    public double calculateNaturalWidth(TextLayer textLayer, RenderContext ctx) {
        String text = ctx.getTextContent();
        if (text == null || text.isEmpty()) return 0;
        Font font = ctx.getFont();
        double totalWidth = 0;
        for (char c : text.toCharArray()) {
            Text t = new Text(String.valueOf(c));
            t.setFont(font);
            totalWidth += t.getLayoutBounds().getWidth();
        }
        return totalWidth;
    }

    @Override
    public double calculateNaturalHeight(TextLayer textLayer, RenderContext ctx) {
        Font font = ctx.getFont();
        if (font == null) return 0;
        Text t = new Text("Mg"); // Use a string with descenders and ascenders for height
        t.setFont(font);
        return t.getLayoutBounds().getHeight();
    }

    @Override
    public boolean isOptimized() {
        return false;
    }
}
