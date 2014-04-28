package uk.ac.ebi.pride.gui.util;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

/**
 * @author Rui Wang
 * @version $Id$
 */
public final class UIUtil {

    public static final Color TRANSPARENT_COLOR = new Color(0, 0, 0, 0);

    private UIUtil() {
    }

    public static boolean isUnderNimbusLookAndFeel() {
        return UIManager.getLookAndFeel().getName().contains("Nimbus");
    }

    /**
     * Draw a line
     *
     * @param g  given graphics
     * @param x1 starting x
     * @param y1 starting y
     * @param x2 stop x
     * @param y2 stop y
     */
    public static void drawLine(Graphics g, int x1, int y1, int x2, int y2) {
        g.drawLine(x1, y1, x2, y2);
    }

    public static void drawRoundRect(Graphics g, double x1d, double y1d, double x2d, double y2d, Color color) {
        final Color oldColor = g.getColor();
        g.setColor(color);

        int x1 = (int) Math.round(x1d);
        int x2 = (int) Math.round(x2d);
        int y1 = (int) Math.round(y1d);
        int y2 = (int) Math.round(y2d);

        drawLine(g, x1 + 1, y1, x2 - 1, y1);
        drawLine(g, x1 + 1, y2, x2 - 1, y2);

        drawLine(g, x1, y1 + 1, x1, y2 - 1);
        drawLine(g, x2, y1 + 1, x2, y2 - 1);

        g.setColor(oldColor);
    }

    public static void drawPlainRect(Graphics g, int x1, int y1, int x2, int y2) {
        drawLine(g, x1, y1, x2 - 1, y1);
        drawLine(g, x2, y1, x2, y2 - 1);
        drawLine(g, x1 + 1, y2, x2, y2);
        drawLine(g, x1, y1 + 1, x1, y2);
    }

    public static Shape getArrowShape(Line2D line, Point2D intersectionPoint) {
        final double deltaY = line.getP2().getY() - line.getP1().getY();
        final double length = Math.sqrt(Math.pow(deltaY, 2) + Math.pow(line.getP2().getX() - line.getP1().getX(), 2));

        double theta = Math.asin(deltaY / length);

        if (line.getP1().getX() > line.getP2().getX()) {
            theta = Math.PI - theta;
        }

        int arrowSize = 9;
        Shape arrowPolygon = new Polygon(new int[]{0, arrowSize, 0, 0}, new int[]{0, arrowSize / 2, arrowSize, 0}, 4);
        AffineTransform rotate = AffineTransform.getRotateInstance(theta, arrowSize, arrowSize / 2);
        Shape polygon = rotate.createTransformedShape(arrowPolygon);

        AffineTransform move = AffineTransform.getTranslateInstance(intersectionPoint.getX() - arrowSize, intersectionPoint.getY() - arrowSize / 2);
        polygon = move.createTransformedShape(polygon);
        return polygon;
    }

    public static Rectangle getScreenRectangle(int x, int y) {
        return getScreenRectangle(new Point(x, y));
    }

    public static Rectangle getScreenRectangle(Point p) {
        final GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice[] devices = env.getScreenDevices();
        double distance = -1;
        GraphicsConfiguration targetGC = null;
        GraphicsConfiguration bestConfig = null;

        for (GraphicsDevice device : devices) {
            final GraphicsConfiguration config = device.getDefaultConfiguration();
            final Rectangle rect = config.getBounds();

            final Insets insets = getScreenInsets(config);
            if (insets != null) {
                rect.x += insets.left;
                rect.width -= (insets.left + insets.right);
                rect.y += insets.top;
                rect.height -= (insets.top + insets.bottom);
            }

            if (rect.contains(p)) {
                targetGC = config;
                break;
            } else {
                final double d = findNearestPointOnBorder(rect, p).distance(p.x, p.y);
                if (bestConfig == null || distance > d) {
                    distance = d;
                    bestConfig = config;
                }
            }
        }

        if (targetGC == null && devices.length > 0 && bestConfig != null) {
            targetGC = bestConfig;
            //targetGC = env.getDefaultScreenDevice().getDefaultConfiguration();
        }
        if (targetGC == null) {
            throw new IllegalStateException("It's impossible to determine target graphics environment for point (" + p.x + "," + p.y + ")");
        }

        // determine real client area of target graphics configuration
        final Insets insets = getScreenInsets(targetGC);
        final Rectangle targetRect = targetGC.getBounds();
        targetRect.x += insets.left;
        targetRect.y += insets.top;
        targetRect.width -= insets.left + insets.right;
        targetRect.height -= insets.top + insets.bottom;

        return targetRect;
    }

    public static Insets getScreenInsets(final GraphicsConfiguration gc) {
        return Toolkit.getDefaultToolkit().getScreenInsets(gc);
    }

    public static Point findNearestPointOnBorder(Rectangle rect, Point p) {
        final int x0 = rect.x;
        final int y0 = rect.y;
        final int x1 = x0 + rect.width;
        final int y1 = y0 + rect.height;
        double distance = -1;
        Point best = null;
        final Point[] variants = {new Point(p.x, y0), new Point(p.x, y1), new Point(x0, p.y), new Point(x1, p.y)};
        for (Point variant : variants) {
            final double d = variant.distance(p.x, p.y);
            if (best == null || distance > d) {
                best = variant;
                distance = d;
            }
        }
        assert best != null;
        return best;
    }

    public static void moveRectangleToFitTheScreen(Rectangle aRectangle) {
        int screenX = aRectangle.x + aRectangle.width / 2;
        int screenY = aRectangle.y + aRectangle.height / 2;
        Rectangle screen = getScreenRectangle(screenX, screenY);

        moveToFit(aRectangle, screen, null);
    }

    public static void moveToFit(final Rectangle rectangle, final Rectangle container, Insets padding) {
        Insets insets = padding != null ? padding : new Insets(0, 0, 0, 0);

        Rectangle move = new Rectangle(rectangle.x - insets.left, rectangle.y - insets.top, rectangle.width + insets.left + insets.right,
                rectangle.height + insets.top + insets.bottom);

        if (move.getMaxX() > container.getMaxX()) {
            move.x = (int) container.getMaxX() - move.width;
        }


        if (move.getMinX() < container.getMinX()) {
            move.x = (int) container.getMinX();
        }

        if (move.getMaxY() > container.getMaxY()) {
            move.y = (int) container.getMaxY() - move.height;
        }

        if (move.getMinY() < container.getMinY()) {
            move.y = (int) container.getMinY();
        }

        rectangle.x = move.x + insets.left;
        rectangle.y = move.y + insets.right;
        rectangle.width = move.width - insets.left - insets.right;
        rectangle.height = move.height - insets.top - insets.bottom;
    }

}
