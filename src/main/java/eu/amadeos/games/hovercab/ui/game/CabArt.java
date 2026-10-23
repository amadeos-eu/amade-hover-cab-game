package eu.amadeos.games.hovercab.ui.game;

import eu.amadeos.games.hovercab.domain.Cab;
import eu.amadeos.games.hovercab.domain.CabShape;
import eu.amadeos.games.hovercab.domain.Field;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/**
 * The cab as it is drawn: yellow, with a checkered band, a cabin with the driver and maybe a passenger, the
 * sign on the roof, the gear, and the flames of the jets that fire. Drawn in the cab's own box of
 * {@link CabShape}, mirrored when it faces left.
 */
final class CabArt {
    static final Color YELLOW = new Color(0xF2C230);
    static final Color EDGE = new Color(0x5A4410);

    private CabArt() {
    }

    // --8<-- [start:cab]
    /** The cab with its top left corner at {@code x}, {@code y}; {@code flicker} 0 to 1 makes the flames dance. */
    static void paint(Graphics2D g, Cab cab, double x, double y, boolean passenger, double flicker) {
        AffineTransform saved = g.getTransform();
        g.translate(x, y);
        if (cab.facingLeft()) g.transform(new AffineTransform(-1, 0, 0, 1, Field.CAB_WIDTH, 0));
        flames(g, cab, flicker);
        body(g, cab.gear(), passenger);
        g.setTransform(saved);
    }
    // --8<-- [end:cab]

    /** The jets: under the belly, on the roof, and at the side that pushes (seen from the cab: back or nose). */
    private static void flames(Graphics2D g, Cab cab, double flicker) {
        double f = 0.8 + 0.4 * flicker;
        if (cab.jetUp()) {
            double length = 13 * f;
            g.setPaint(new GradientPaint(16, 12, new Color(0xFFF6C0), 16, (float) (12 + length), new Color(255, 106, 32, 0)));
            Path2D p = new Path2D.Double();
            p.moveTo(11.5, 12);
            p.lineTo(20.5, 12);
            p.lineTo(17.5, 12 + length);
            p.lineTo(16, 14 + length);
            p.lineTo(14.5, 12 + length);
            p.closePath();
            g.fill(p);
            g.setColor(new Color(255, 255, 255, 220));
            g.fill(new Ellipse2D.Double(13.5, 12, 5, 5));
        }
        if (cab.jetDown()) {
            double length = 6 * f;
            g.setPaint(new GradientPaint(16, 0, new Color(0xFFF6C0), 16, (float) -length, new Color(255, 106, 32, 0)));
            g.fill(new Rectangle2D.Double(14, -length, 4, length));
        }
        int side = cab.jetSide() * (cab.facingLeft() ? -1 : 1);     // in the cab's own mirrored box
        if (side != 0) {
            double length = 9 * f;
            double from = side > 0 ? 1 : 31;                         // pushing forward: the rear jet fires
            double to = side > 0 ? from - length : from + length;
            g.setPaint(new GradientPaint((float) from, 8, new Color(0xFFD070), (float) to, 8, new Color(255, 106, 32, 0)));
            Path2D p = new Path2D.Double();
            p.moveTo(from, 6);
            p.lineTo(to, 8);
            p.lineTo(from, 10);
            p.closePath();
            g.fill(p);
        }
    }

    // --8<-- [start:body]
    static void body(Graphics2D g, double gear, boolean passenger) {
        // the gear: two legs with feet
        g.setColor(new Color(0x3A3A44));
        for (Shape leg : CabShape.legs(gear)) g.fill(leg);
        if (gear > 0.5) {
            double foot = Field.HULL_HEIGHT + (Field.CAB_HEIGHT - Field.HULL_HEIGHT) * gear - 1;
            g.fill(new Rectangle2D.Double(Field.LEG_LEFT - 1, foot, 5, 1.2));
            g.fill(new Rectangle2D.Double(Field.LEG_RIGHT - 4, foot, 5, 1.2));
        }
        Path2D hull = CabShape.hull();
        g.setColor(YELLOW);
        g.fill(hull);
        g.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 90), 0, 13, new Color(0, 0, 0, 50)));
        g.fill(hull);
        // the checkered band
        for (int i = 0; i < 14; i++) {
            g.setColor(i % 2 == 0 ? new Color(0x1A1A1A) : new Color(0xFFF0C0));
            g.fill(new Rectangle2D.Double(2 + i * 2, 8, 2, 1.5));
            g.setColor(i % 2 == 1 ? new Color(0x1A1A1A) : new Color(0xFFF0C0));
            g.fill(new Rectangle2D.Double(2 + i * 2, 9.5, 2, 1.5));
        }
        g.setColor(EDGE);
        g.setStroke(new BasicStroke(0.8f));
        g.draw(hull);
        g.setStroke(new BasicStroke(1));
        // the windows: the passenger in the back, the driver in the front
        g.setColor(new Color(0x9AD8FF));
        Path2D back = new Path2D.Double();
        back.moveTo(8, 4);
        back.lineTo(11.5, 1);
        back.lineTo(16, 1);
        back.lineTo(16, 4);
        back.closePath();
        g.fill(back);
        Path2D front = new Path2D.Double();
        front.moveTo(17.5, 4);
        front.lineTo(17.5, 1);
        front.lineTo(21.5, 1);
        front.lineTo(25, 4);
        front.closePath();
        g.fill(front);
        if (passenger) {
            g.setColor(new Color(0x3A2A20));
            g.fill(new Ellipse2D.Double(12, 1.2, 3, 3));
        }
        g.setColor(new Color(0x2A2A3A));
        g.fill(new Ellipse2D.Double(19.2, 1.2, 2.8, 2.8));
        // the sign on the roof, lit while the cab is free
        g.setColor(passenger ? new Color(0xB8B0A0) : new Color(0xFFF8E0));
        g.fill(new RoundRectangle2D.Double(12, -3, 8, 3, 1, 1));
        g.setColor(EDGE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 1).deriveFont(2.6f));
        g.drawString("CAB", 16 - g.getFontMetrics().stringWidth("CAB") / 2f, -0.7f);
        // the head lamp
        g.setColor(new Color(0xFFFBE0));
        g.fill(new Ellipse2D.Double(29, 6.5, 2.5, 2));
    }
    // --8<-- [end:body]
}
