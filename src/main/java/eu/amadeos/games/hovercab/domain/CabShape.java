package eu.amadeos.games.hovercab.domain;

import eu.amadeos.gameengine.collision.PixelMask;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * The outline of the cab, facing right, in its own box of 32 × 16 units: the hull with the cabin on top,
 * and the two legs of the gear under it. The painter draws these shapes, and the same shapes are the cab's
 * pixel masks: what is seen is what collides.
 */
public final class CabShape {
    /** Masks facing right and left, with the gear in and out. */
    private static final PixelMask[][] MASKS = new PixelMask[2][2];

    private CabShape() {
    }

    // --8<-- [start:hull]
    /** The hull: a long low body, the cabin a step up in the middle, the nose to the right. */
    public static Path2D hull() {
        Path2D p = new Path2D.Double();
        p.moveTo(1, 11);
        p.lineTo(1, 5);
        p.lineTo(6, 4);
        p.lineTo(11, 0);
        p.lineTo(22, 0);
        p.lineTo(27, 5);
        p.lineTo(31, 6);
        p.lineTo(31, 11);
        p.quadTo(31, 13, 29, 13);
        p.lineTo(3, 13);
        p.quadTo(1, 13, 1, 11);
        p.closePath();
        return p;
    }
    // --8<-- [end:hull]

    /** The two legs, {@code out} 0 (in) to 1 (out); they stand on the pad at the bottom of the box. */
    public static Shape[] legs(double out) {
        double length = (Field.CAB_HEIGHT - Field.HULL_HEIGHT) * out;
        return new Shape[]{
                new Rectangle2D.Double(Field.LEG_LEFT, Field.HULL_HEIGHT, 3, length),
                new Rectangle2D.Double(Field.LEG_RIGHT - 3, Field.HULL_HEIGHT, 3, length)};
    }

    /** Mirrors a shape of the cab for a cab that faces left. */
    public static Shape facingLeft(Shape shape) {
        AffineTransform flip = new AffineTransform(-1, 0, 0, 1, Field.CAB_WIDTH, 0);
        return flip.createTransformedShape(shape);
    }

    /** The cab's pixels: the hull, and the legs if the gear is all the way out. */
    public static PixelMask mask(boolean left, boolean gearOut) {
        int i = left ? 1 : 0, j = gearOut ? 1 : 0;
        synchronized (MASKS) {
            if (MASKS[i][j] == null) {
                MASKS[i][j] = PixelMask.painted((int) Field.CAB_WIDTH, (int) Field.CAB_HEIGHT, g -> {
                    if (left) g.transform(new AffineTransform(-1, 0, 0, 1, Field.CAB_WIDTH, 0));
                    g.fill(hull());
                    if (gearOut) for (Shape leg : legs(1)) g.fill(leg);
                });
            }
            return MASKS[i][j];
        }
    }
}
