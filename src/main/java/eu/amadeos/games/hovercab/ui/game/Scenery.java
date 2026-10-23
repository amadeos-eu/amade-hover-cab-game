package eu.amadeos.games.hovercab.ui.game;

import eu.amadeos.gameengine.sprite.Sprite;
import eu.amadeos.games.hovercab.domain.Field;
import eu.amadeos.games.hovercab.domain.Mover;
import eu.amadeos.games.hovercab.domain.Pad;
import eu.amadeos.games.hovercab.domain.Shift;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Arc2D;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Random;

/**
 * The scenery of a shift as one picture: the backdrop of its theme, the sea, the ceiling with the gate, the
 * blocks in their looks, the signs, the decorations and the pads that do not move. It never changes while
 * the shift is flown, so it is a {@link Sprite}: drawn once at the size of the screen and then only copied.
 * Lifts, moving blocks and the gate's lights change, and {@link #lift}, {@link #mover} and {@link #gate}
 * draw them in every frame.
 */
final class Scenery {
    static final Color STRIPE = new Color(0xF2C230);
    static final Color FUEL_GREEN = new Color(0x2AA86A);

    private Scenery() {
    }

    // --8<-- [start:sprite]
    /** The picture of a shift's scenery, opaque: the backdrop covers every pixel. */
    static Sprite of(Shift shift) {
        return Sprite.drawn(Field.WIDTH, Field.HEIGHT, (g, w, h) -> paint(g, shift)).opaque();
    }
    // --8<-- [end:sprite]

    static void paint(Graphics2D g, Shift shift) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        long seed = shift.number() * 7919L;
        Backdrop.paint(g, shift.theme(), seed);
        if (shift.hasSea()) sea(g, shift);
        Random r = new Random(seed + 1);
        for (Shift.Solid solid : shift.solids()) {
            if (solid.kind() == Shift.Solid.Kind.SIGN) sign(g, solid);
            else if (solid.kind() == Shift.Solid.Kind.BEAM) beam(g, solid.numbers());
            else material(g, solid.shape(), solid.look(), r);
        }
        for (Shift.Deco deco : shift.decos()) deco(g, deco, r);
        for (Pad pad : shift.pads()) if (!pad.isLift()) pad(g, pad, pad.y());
        ceiling(g, shift);
    }

    // ---- the sea and the ceiling ---------------------------------------------------------------------------

    private static void sea(Graphics2D g, Shift shift) {
        double top = shift.sea();
        Color[] colours = Backdrop.sea(shift.theme());
        Backdrop.gradient(g, 0, top, Field.WIDTH, Field.HEIGHT - top, colours[0], colours[1]);
        Random r = new Random(shift.number());
        g.setColor(new Color(255, 255, 255, 90));
        g.fill(new Rectangle2D.Double(0, top, Field.WIDTH, 1));
        for (int i = 0; i < 90; i++) {
            double y = top + 3 + r.nextDouble() * (Field.HEIGHT - top - 3), x = r.nextDouble() * Field.WIDTH;
            g.setColor(r.nextInt(3) == 0 ? new Color(255, 210, 122, 70) : new Color(138, 160, 224, 70));
            g.fill(new Rectangle2D.Double(x, y, 4 + r.nextDouble() * 14, 1));
        }
    }

    // --8<-- [start:ceiling]
    /** The girders along the top, with a gap for the gate and its two posts. */
    private static void ceiling(Graphics2D g, Shift shift) {
        double top = Field.TOP, height = Field.CEILING - Field.TOP;
        double[][] spans = {{0, shift.gateFrom()}, {shift.gateTo(), Field.WIDTH}};
        g.setColor(new Color(0x161924));
        g.fill(new Rectangle2D.Double(0, 0, Field.WIDTH, top));
        for (double[] span : spans) {
            if (span[1] <= span[0]) continue;
            g.setColor(new Color(0x3C4256));
            g.fill(new Rectangle2D.Double(span[0], top, span[1] - span[0], height));
            g.setColor(new Color(0x5A6480));
            g.fill(new Rectangle2D.Double(span[0], top + height - 2, span[1] - span[0], 2));
            g.setColor(new Color(0x262A3A));
            g.setStroke(new BasicStroke(1.2f));
            for (double x = span[0]; x < span[1] - 8; x += 12) {
                g.draw(new Line2D.Double(x, top + 1, x + 6, top + height - 3));
                g.draw(new Line2D.Double(x + 6, top + height - 3, x + 12, top + 1));
            }
        }
        g.setStroke(new BasicStroke(1));
        for (double x : new double[]{shift.gateFrom() - 6, shift.gateTo()}) {
            g.setColor(new Color(0x2A2E40));
            g.fill(new Rectangle2D.Double(x, top, 6, height + 8));
        }
    }
    // --8<-- [end:ceiling]

    /** The gate's lights and, while it is shut, the beam across it; green and open when the last fare wants up. */
    static void gate(Graphics2D g, Shift shift, boolean open, double time) {
        Color light = open ? new Color(0x5AFF8A) : new Color(0xFF3A3A);
        boolean on = !open || (time * 2) % 1 < 0.6;
        for (double x : new double[]{shift.gateFrom() - 5, shift.gateTo() + 1}) {
            if (on) {
                g.setColor(new Color(light.getRed(), light.getGreen(), light.getBlue(), 70));
                g.fill(new Ellipse2D.Double(x - 4, Field.CEILING + 1, 12, 12));
            }
            g.setColor(on ? light : light.darker().darker());
            g.fill(new Ellipse2D.Double(x, Field.CEILING + 5, 4, 4));
        }
        if (!open) {
            double flicker = 0.75 + 0.25 * Math.sin(time * 23);
            g.setColor(new Color(255, 90, 80, (int) (160 * flicker)));
            g.fill(new Rectangle2D.Double(shift.gateFrom(), Field.TOP + 6, shift.gateTo() - shift.gateFrom(), 2));
            g.setColor(new Color(255, 90, 80, (int) (50 * flicker)));
            g.fill(new Rectangle2D.Double(shift.gateFrom(), Field.TOP + 3, shift.gateTo() - shift.gateFrom(), 8));
        } else {
            for (int i = 0; i < 3; i++) {                // arrows pointing up
                double y = Field.CEILING - 2 - ((time * 30 + i * 6) % 18);
                double x = (shift.gateFrom() + shift.gateTo()) / 2;
                g.setColor(new Color(90, 255, 140, 160));
                g.fill(new Polygon(new int[]{(int) x - 5, (int) x, (int) x + 5}, new int[]{(int) y + 4, (int) y, (int) y + 4}, 3));
            }
        }
    }

    // ---- the looks of the solid things -----------------------------------------------------------------

    // --8<-- [start:material]
    /** Fills a solid shape in its look: a base colour, a texture clipped to the shape, light above and shadow below. */
    static void material(Graphics2D g, Shape shape, String look, Random r) {
        Shape clip = g.getClip();
        g.clip(shape);
        Rectangle2D b = shape.getBounds2D();
        switch (look) {
            case "brick" -> bricks(g, b, 0x8A4A3C, 0x5A2E28, r);
            case "glass" -> glass(g, b, r);
            case "concrete" -> concrete(g, b, r);
            case "steel" -> steel(g, b);
            case "rock" -> rock(g, b, 0x6A5040, r);
            case "sand" -> sandstone(g, b, r);
            case "ice" -> ice(g, b, r);
            case "hull" -> hull(g, b);
            case "wood" -> wood(g, b, r);
            default -> {
                g.setColor(new Color(0x2E3244));
                g.fill(b);
            }
        }
        g.setClip(clip);
        // a lit edge along the top of the shape, and a dark one below
        Stroke stroke = g.getStroke();
        g.setStroke(new BasicStroke(1.2f));
        g.setColor(new Color(255, 255, 255, 50));
        g.draw(shape);
        g.setStroke(stroke);
    }
    // --8<-- [end:material]

    private static Color vary(int rgb, Random r, int amount) {
        Color c = new Color(rgb);
        int v = r.nextInt(2 * amount + 1) - amount;
        return new Color(clamp(c.getRed() + v), clamp(c.getGreen() + v), clamp(c.getBlue() + v));
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private static void bricks(Graphics2D g, Rectangle2D b, int base, int mortar, Random r) {
        g.setColor(new Color(mortar));
        g.fill(b);
        int row = 0;
        for (double y = b.getY(); y < b.getMaxY(); y += 5, row++) {
            for (double x = b.getX() - (row % 2) * 5; x < b.getMaxX(); x += 10) {
                g.setColor(vary(base, r, 9));
                g.fill(new Rectangle2D.Double(x + 1, y + 1, 9, 4));
            }
        }
        windows(g, b, r, 0x1C1830);
        g.setColor(new Color(0, 0, 0, 60));
        g.fill(new Rectangle2D.Double(b.getMaxX() - 8, b.getY(), 8, b.getHeight()));
    }

    /** Rows of windows in a building's face, most of them lit. */
    private static void windows(Graphics2D g, Rectangle2D b, Random r, int frame) {
        if (b.getWidth() < 40 || b.getHeight() < 40) return;
        int across = (int) ((b.getWidth() - 12) / 14);
        double left = b.getX() + (b.getWidth() - across * 14 + 6) / 2;
        for (double y = b.getY() + 14; y + 14 <= b.getMaxY() - 6; y += 18) {
            for (int i = 0; i < across; i++) {
                double x = left + i * 14;
                boolean lit = r.nextInt(3) != 0;
                g.setColor(new Color(frame));
                g.fill(new Rectangle2D.Double(x - 1, y - 1, 10, 14));
                g.setColor(lit ? new Color(r.nextInt(4) == 0 ? 0xA8E0FF : 0xFFCF70) : new Color(0x30304A));
                g.fill(new Rectangle2D.Double(x, y, 8, 12));
                if (lit) {
                    g.setColor(new Color(255, 255, 255, 60));
                    g.fill(new Rectangle2D.Double(x, y, 8, 3));
                }
            }
        }
    }

    private static void glass(Graphics2D g, Rectangle2D b, Random r) {
        g.setPaint(new GradientPaint((float) b.getX(), 0, new Color(0x40507A), (float) b.getMaxX(), 0, new Color(0x222A48)));
        g.fill(b);
        for (double x = b.getX() + 6; x < b.getMaxX(); x += 12) {
            g.setColor(new Color(0x1A2038));
            g.fill(new Rectangle2D.Double(x + 9, b.getY(), 3, b.getHeight()));
            for (double y = b.getY() + 10; y < b.getMaxY() - 6; y += 10) {
                int pick = r.nextInt(6);
                g.setColor(pick == 0 ? new Color(154, 216, 255, 170) : pick < 3 ? new Color(255, 210, 122, 150) : new Color(0x2C3658));
                g.fill(new Rectangle2D.Double(x, y, 9, 6));
            }
        }
        g.setPaint(new GradientPaint(0, (float) b.getY(), new Color(240, 154, 96, 0), 0, (float) b.getMaxY(), new Color(240, 154, 96, 60)));
        g.fill(b);
    }

    private static void concrete(Graphics2D g, Rectangle2D b, Random r) {
        g.setPaint(new GradientPaint((float) b.getX(), 0, new Color(0x9AA2B6), (float) b.getMaxX(), 0, new Color(0x6A7286)));
        g.fill(b);
        g.setColor(new Color(0x4A5064));
        for (double y = b.getY() + 12; y < b.getMaxY(); y += 14) g.fill(new Rectangle2D.Double(b.getX(), y, b.getWidth(), 1));
        for (int i = 0; i < b.getWidth() * b.getHeight() / 40; i++) {
            g.setColor(new Color(0, 0, 0, 25));
            g.fill(new Rectangle2D.Double(b.getX() + r.nextDouble() * b.getWidth(), b.getY() + r.nextDouble() * b.getHeight(), 1, 1));
        }
        windows(g, b, r, 0x2A2E40);
    }

    private static void steel(Graphics2D g, Rectangle2D b) {
        g.setPaint(new GradientPaint(0, (float) b.getY(), new Color(0x5A6480), 0, (float) b.getMaxY(), new Color(0x2E3446)));
        g.fill(b);
        g.setColor(new Color(0x262A3A));
        g.setStroke(new BasicStroke(1.2f));
        double step = Math.max(8, Math.min(16, b.getHeight()));
        for (double x = b.getX(); x < b.getMaxX(); x += step) {
            g.draw(new Line2D.Double(x, b.getY(), x + step, b.getMaxY()));
            g.draw(new Line2D.Double(x + step, b.getY(), x, b.getMaxY()));
        }
        g.setStroke(new BasicStroke(1));
        g.setColor(new Color(0x9AA4C0));
        for (double x = b.getX() + 3; x < b.getMaxX(); x += 8) {
            g.fill(new Rectangle2D.Double(x, b.getY() + 1, 1, 1));
            g.fill(new Rectangle2D.Double(x, b.getMaxY() - 2, 1, 1));
        }
    }

    private static void rock(Graphics2D g, Rectangle2D b, int base, Random r) {
        g.setColor(new Color(base));
        g.fill(b);
        for (int i = 0; i < b.getWidth() * b.getHeight() / 30; i++) {
            double x = b.getX() + r.nextDouble() * b.getWidth(), y = b.getY() + r.nextDouble() * b.getHeight();
            g.setColor(vary(base, r, 22));
            g.fill(new Ellipse2D.Double(x, y, 3 + r.nextDouble() * 7, 2 + r.nextDouble() * 4));
        }
        for (int i = 0; i < b.getWidth() * b.getHeight() / 900; i++) {
            g.setColor(new Color(255, 230, 190, 40));
            double x = b.getX() + r.nextDouble() * b.getWidth(), y = b.getY() + r.nextDouble() * b.getHeight();
            g.draw(new Line2D.Double(x, y, x + 4 + r.nextDouble() * 6, y + r.nextDouble() * 3));
        }
    }

    private static void sandstone(Graphics2D g, Rectangle2D b, Random r) {
        int[] layers = {0xC88A5A, 0xB87A4E, 0xD89A68, 0xA86A44};
        int i = 0;
        for (double y = b.getY(); y < b.getMaxY(); i++) {
            double h = 5 + r.nextDouble() * 9;
            g.setColor(new Color(layers[i % layers.length]));
            g.fill(new Rectangle2D.Double(b.getX(), y, b.getWidth(), h + 1));
            y += h;
        }
        for (int k = 0; k < b.getWidth() * b.getHeight() / 60; k++) {
            g.setColor(new Color(90, 50, 30, 50));
            g.fill(new Rectangle2D.Double(b.getX() + r.nextDouble() * b.getWidth(), b.getY() + r.nextDouble() * b.getHeight(), 2, 1));
        }
    }

    private static void ice(Graphics2D g, Rectangle2D b, Random r) {
        g.setPaint(new GradientPaint(0, (float) b.getY(), new Color(0xE8F6FF), 0, (float) b.getMaxY(), new Color(0x8CC0E0)));
        g.fill(b);
        g.setColor(new Color(255, 255, 255, 140));
        for (int i = 0; i < b.getWidth() * b.getHeight() / 500 + 2; i++) {
            double x = b.getX() + r.nextDouble() * b.getWidth(), y = b.getY() + r.nextDouble() * b.getHeight();
            g.draw(new Line2D.Double(x, y, x + 6 + r.nextDouble() * 10, y + 4 + r.nextDouble() * 8));
        }
        g.setColor(new Color(60, 120, 170, 60));
        for (int i = 0; i < b.getWidth() * b.getHeight() / 800 + 1; i++) {
            double x = b.getX() + r.nextDouble() * b.getWidth(), y = b.getY() + r.nextDouble() * b.getHeight();
            g.draw(new Line2D.Double(x, y, x - 4 - r.nextDouble() * 8, y + 6 + r.nextDouble() * 8));
        }
    }

    private static void hull(Graphics2D g, Rectangle2D b) {
        g.setPaint(new GradientPaint(0, (float) b.getY(), new Color(0xD8DCE6), 0, (float) b.getMaxY(), new Color(0x8C92A4)));
        g.fill(b);
        g.setColor(new Color(0x6A7084));
        for (double x = b.getX(); x < b.getMaxX(); x += 24) g.fill(new Rectangle2D.Double(x, b.getY(), 1, b.getHeight()));
        for (double y = b.getY(); y < b.getMaxY(); y += 18) g.fill(new Rectangle2D.Double(b.getX(), y, b.getWidth(), 1));
        g.setColor(new Color(0xE8783A));
        if (b.getHeight() > 20) g.fill(new Rectangle2D.Double(b.getX(), b.getY() + 6, b.getWidth(), 3));
        g.setColor(new Color(0x6AC8FF));
        for (double x = b.getX() + 10; x + 6 < b.getMaxX(); x += 24) {
            for (double y = b.getY() + 16; y + 6 < b.getMaxY(); y += 18) g.fill(new Ellipse2D.Double(x, y, 5, 5));
        }
    }

    private static void wood(Graphics2D g, Rectangle2D b, Random r) {
        g.setColor(new Color(0x4A3A30));
        g.fill(b);
        for (double y = b.getY(); y < b.getMaxY(); y += 4) {
            g.setColor(vary(0x6A4E38, r, 12));
            g.fill(new Rectangle2D.Double(b.getX(), y, b.getWidth(), 3));
        }
    }

    private static void beam(Graphics2D g, double[] n) {
        g.setColor(new Color(0x3A4058));
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(n[0], n[1], n[2], n[3]));
        g.setColor(new Color(0x6A7490));
        g.setStroke(new BasicStroke(1));
        g.draw(new Line2D.Double(n[0], n[1] - 1, n[2], n[3] - 1));
    }

    // --8<-- [start:sign]
    /** A neon sign on two chains from the ceiling: dark board, a glowing frame and its word. */
    private static void sign(Graphics2D g, Shift.Solid solid) {
        double[] n = solid.numbers();
        double x = n[0], y = n[1], w = n[2], h = n[3];
        Color neon = switch (solid.look()) {
            case "cyan" -> new Color(0x4FE8FF);
            case "green" -> new Color(0x6AFF6A);
            case "yellow" -> new Color(0xFFE04F);
            case "orange" -> new Color(0xFF9A3A);
            default -> new Color(0xFF4FB0);
        };
        g.setColor(new Color(0x8A94B0));
        for (double cx : new double[]{x + 10, x + w - 10}) {
            for (double cy = Field.CEILING; cy < y; cy += 4) g.draw(new Ellipse2D.Double(cx - 1, cy, 2, 3));
        }
        g.setColor(new Color(neon.getRed(), neon.getGreen(), neon.getBlue(), 60));
        g.fill(new RoundRectangle2D.Double(x - 4, y - 4, w + 8, h + 8, 10, 10));
        g.setColor(new Color(0x1C1830));
        g.fill(new RoundRectangle2D.Double(x, y, w, h, 6, 6));
        g.setColor(neon);
        g.setStroke(new BasicStroke(1.2f));
        g.draw(new RoundRectangle2D.Double(x + 2, y + 2, w - 4, h - 4, 4, 4));
        g.setStroke(new BasicStroke(1));
        float size = (float) Math.min(h * 0.45, (w - 10) / Math.max(1, solid.text().length()) * 1.6);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 1).deriveFont(size));
        double tx = x + (w - g.getFontMetrics().stringWidth(solid.text())) / 2, ty = y + h / 2 + size * 0.36;
        g.setColor(new Color(neon.getRed(), neon.getGreen(), neon.getBlue(), 90));
        g.drawString(solid.text(), (float) tx + 0.6f, (float) ty + 0.6f);
        g.setColor(neon.brighter());
        g.drawString(solid.text(), (float) tx, (float) ty);
    }
    // --8<-- [end:sign]

    // ---- decorations -------------------------------------------------------------------------------------

    /** Something that is only drawn: its {@code x} is its middle, its {@code y} where it stands. */
    private static void deco(Graphics2D g, Shift.Deco deco, Random r) {
        double x = deco.x(), y = deco.y();
        switch (deco.kind()) {
            case "tree" -> {
                g.setColor(new Color(0x5A3A24));
                g.fill(new Rectangle2D.Double(x - 1.5, y - 12, 3, 12));
                for (int i = 0; i < 4; i++) {
                    g.setColor(i % 2 == 0 ? new Color(0x3A8A3A) : new Color(0x4AA04A));
                    g.fill(new Ellipse2D.Double(x - 9 + i * 3 - r.nextDouble() * 2, y - 28 + (i % 2) * 4, 12, 12));
                }
            }
            case "pine" -> {
                g.setColor(new Color(0x4A3020));
                g.fill(new Rectangle2D.Double(x - 1.5, y - 6, 3, 6));
                for (int i = 0; i < 3; i++) {
                    g.setColor(new Color(0x2A6A4A));
                    double w = 18 - i * 4, top = y - 10 - i * 8;
                    g.fill(new Polygon(new int[]{(int) (x - w / 2), (int) x, (int) (x + w / 2)}, new int[]{(int) (top + 10), (int) (top - 4), (int) (top + 10)}, 3));
                    g.setColor(new Color(255, 255, 255, 180));
                    g.fill(new Polygon(new int[]{(int) (x - 3), (int) x, (int) (x + 3)}, new int[]{(int) top, (int) (top - 4), (int) top}, 3));
                }
            }
            case "tank" -> {
                g.setColor(new Color(0x6A4A34));
                g.fill(new Rectangle2D.Double(x, y - 30, 14, 30));
                g.setColor(new Color(0x4A3426));
                for (double yy = y - 26; yy < y; yy += 5) g.fill(new Rectangle2D.Double(x, yy, 14, 1));
                g.fill(new Polygon(new int[]{(int) x - 2, (int) x + 7, (int) x + 16}, new int[]{(int) y - 29, (int) y - 38, (int) y - 29}, 3));
            }
            case "antenna" -> {
                g.setColor(new Color(0x8A94B0));
                g.fill(new Rectangle2D.Double(x - 1, y - 46, 2, 46));
                g.fill(new Rectangle2D.Double(x - 7, y - 28, 14, 1));
                g.setColor(new Color(255, 58, 58, 60));
                g.fill(new Ellipse2D.Double(x - 6, y - 54, 12, 12));
                g.setColor(new Color(0xFF3A3A));
                g.fill(new Ellipse2D.Double(x - 2, y - 50, 4, 4));
            }
            case "lamp" -> {
                g.setColor(new Color(0x3A3E50));
                g.fill(new Rectangle2D.Double(x - 1, y - 26, 2, 26));
                g.fill(new Rectangle2D.Double(x - 1, y - 26, 8, 2));
                g.setColor(new Color(255, 230, 160, 60));
                g.fill(new Ellipse2D.Double(x, y - 30, 14, 12));
                g.setColor(new Color(0xFFF0B0));
                g.fill(new Ellipse2D.Double(x + 4, y - 25, 4, 3));
            }
            case "crane" -> {
                g.setColor(new Color(0xE8A030));
                g.fill(new Rectangle2D.Double(x - 2, y - 80, 4, 80));
                g.fill(new Rectangle2D.Double(x - 30, y - 80, 70, 3));
                g.setColor(new Color(0x2A2E40));
                g.draw(new Line2D.Double(x + 30, y - 77, x + 30, y - 50));
                g.fill(new Rectangle2D.Double(x + 26, y - 50, 8, 6));
            }
            case "chimney" -> {
                g.setColor(new Color(0x7A3A2E));
                g.fill(new Rectangle2D.Double(x - 5, y - 18, 10, 18));
                g.setColor(new Color(0x4A2A22));
                g.fill(new Rectangle2D.Double(x - 6, y - 19, 12, 3));
                for (int s = 0; s < 4; s++) {
                    g.setColor(new Color(200, 200, 210, 70 - s * 15));
                    g.fill(new Ellipse2D.Double(x - 4 + s * 4, y - 28 - s * 8, 8 + s * 3, 6 + s * 2));
                }
            }
            case "dish" -> {
                g.setColor(new Color(0xC8CCD8));
                g.fill(new Arc2D.Double(x - 12, y - 24, 24, 24, 200, 180, Arc2D.CHORD));
                g.setColor(new Color(0x8A90A4));
                g.fill(new Rectangle2D.Double(x - 1, y - 12, 2, 12));
            }
            case "pipe" -> {
                g.setColor(new Color(0x6A6E7A));
                g.setStroke(new BasicStroke(4));
                g.draw(new CubicCurve2D.Double(x, y, x, y - 30, x + 30, y - 30, x + 30, y - 60));
                g.setStroke(new BasicStroke(1));
            }
            case "cactus" -> {
                g.setColor(new Color(0x3A7A44));
                g.fill(new RoundRectangle2D.Double(x - 3, y - 26, 6, 26, 6, 6));
                g.fill(new RoundRectangle2D.Double(x - 10, y - 18, 5, 10, 5, 5));
                g.fill(new Rectangle2D.Double(x - 8, y - 11, 6, 3));
                g.fill(new RoundRectangle2D.Double(x + 5, y - 22, 5, 12, 5, 5));
                g.fill(new Rectangle2D.Double(x + 2, y - 13, 6, 3));
            }
            case "crystal" -> {
                Color[] c = {new Color(0x6AE0F0), new Color(0x3AA8C8), new Color(0xB0F4FF)};
                for (int i = 0; i < 3; i++) {
                    double cx = x - 6 + i * 6, h = 10 + (i == 1 ? 8 : r.nextInt(4));
                    g.setColor(c[i]);
                    g.fill(new Polygon(new int[]{(int) cx - 3, (int) cx, (int) cx + 3}, new int[]{(int) y, (int) (y - h), (int) y}, 3));
                }
                g.setColor(new Color(110, 230, 255, 40));
                g.fill(new Ellipse2D.Double(x - 14, y - 22, 28, 26));
            }
            case "flag" -> {
                g.setColor(new Color(0x8A94B0));
                g.fill(new Rectangle2D.Double(x, y - 30, 1.5, 30));
                g.setColor(new Color(0xE84A4A));
                g.fill(new Polygon(new int[]{(int) x + 2, (int) x + 16, (int) x + 2}, new int[]{(int) y - 30, (int) y - 25, (int) y - 20}, 3));
            }
            default -> {
            }
        }
    }

    // ---- pads, lifts and moving blocks -------------------------------------------------------------------

    // --8<-- [start:pad]
    /** A pad: a deck with a striped edge, landing lights, and a sign with its number (FUEL for the fuel pad). */
    static void pad(Graphics2D g, Pad pad, double top) {
        double x = pad.x(), w = pad.width();
        boolean fuel = pad.fuel();
        g.setColor(new Color(0x30343E));
        g.fill(new Rectangle2D.Double(x, top, w, Field.DECK));
        g.setColor(new Color(0x5A606E));
        g.fill(new Rectangle2D.Double(x, top + Field.DECK - 1, w, 1));
        for (double i = 0; i < w; i += 6) {
            g.setColor(fuel ? FUEL_GREEN : STRIPE);
            g.fill(new Rectangle2D.Double(x + i, top, Math.min(3, w - i), 2.5));
        }
        for (double i = 3; i < w; i += 10) {
            g.setColor(fuel ? new Color(106, 255, 176, 70) : new Color(255, 240, 160, 70));
            g.fill(new Ellipse2D.Double(x + i - 1.5, top - 2.5, 4, 4));
            g.setColor(fuel ? new Color(0x6AFFB0) : new Color(0xFFF0A0));
            g.fill(new Rectangle2D.Double(x + i, top - 1, 1, 1));
        }
        double sx = x + 1, sy = top - 21, sw = fuel ? 22 : 11;
        g.setColor(new Color(0x5A6070));
        g.fill(new Rectangle2D.Double(sx + 4, sy + 12, 2, 9));
        g.setColor(fuel ? new Color(0x1A6A44) : new Color(0x1C2A5A));
        g.fill(new RoundRectangle2D.Double(sx, sy, sw, 12, 3, 3));
        g.setColor(new Color(255, 255, 255, 200));
        g.draw(new RoundRectangle2D.Double(sx + 0.5, sy + 0.5, sw - 1, 11, 3, 3));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 1).deriveFont(fuel ? 6.5f : 9f));
        String label = fuel ? "FUEL" : Integer.toString(pad.number());
        g.setColor(Color.WHITE);
        g.drawString(label, (float) (sx + (sw - g.getFontMetrics().stringWidth(label)) / 2), (float) (sy + (fuel ? 8.5 : 9.5)));
        if (fuel) pump(g, pad.right() - 13, top);
    }
    // --8<-- [end:pad]

    private static void pump(Graphics2D g, double x, double top) {
        g.setColor(FUEL_GREEN);
        g.fill(new RoundRectangle2D.Double(x, top - 22, 12, 22, 3, 3));
        g.setColor(new Color(0xD8FFE8));
        g.fill(new Rectangle2D.Double(x + 2, top - 19, 8, 6));
        g.setColor(new Color(0x1A6A44));
        g.fill(new Rectangle2D.Double(x + 3, top - 18, 6, 2));
        g.setColor(new Color(0x202020));
        g.setStroke(new BasicStroke(1.4f));
        g.draw(new CubicCurve2D.Double(x, top - 10, x - 6, top - 8, x - 6, top - 2, x - 3, top - 1));
        g.setStroke(new BasicStroke(1));
    }

    /** A lift where it is now: its cables up to the ceiling, its deck and its sign. */
    static void lift(Graphics2D g, Pad pad, double top) {
        g.setColor(new Color(0x8A94B0));
        for (double x : new double[]{pad.x() + 3, pad.right() - 4}) {
            g.fill(new Rectangle2D.Double(x, Field.CEILING, 1, top - Field.CEILING));
        }
        g.setColor(new Color(0x3A4058));
        g.fill(new Rectangle2D.Double(pad.x() - 2, top + Field.DECK, pad.width() + 4, 3));
        pad(g, pad, top);
    }

    static void mover(Graphics2D g, Mover mover, double time) {
        switch (mover.look()) {
            case "van" -> {
                van(g, mover.x(time), mover.y(time), mover.width(), mover.height());
                return;
            }
            case "drone" -> {
                drone(g, mover.x(time), mover.y(time), mover.width(), mover.height());
                return;
            }
            default -> {
            }
        }
        Shape box = new Rectangle2D.Double(mover.x(time), mover.y(time), mover.width(), mover.height());
        material(g, box, mover.look(), new Random(17));
        g.setColor(new Color(0xF2C230));
        Path2D stripes = new Path2D.Double();
        Rectangle2D b = box.getBounds2D();
        for (double x = b.getX() - b.getHeight(); x < b.getMaxX(); x += 8) {
            stripes.moveTo(x, b.getMaxY());
            stripes.lineTo(x + 4, b.getMaxY());
            stripes.lineTo(x + 7, b.getMaxY() - 3);
            stripes.lineTo(x + 3, b.getMaxY() - 3);
            stripes.closePath();
        }
        Shape clip = g.getClip();
        g.clip(box);
        g.fill(stripes);
        g.setClip(clip);
    }

    /** A flying delivery van: a box with a cab at each end, windows, a stripe and the glow of its hover pads. */
    private static void van(Graphics2D g, double x, double y, double w, double h) {
        g.setPaint(new GradientPaint((float) x, (float) y, new Color(0xF4F4F0), (float) x, (float) (y + h), new Color(0xB8BCC4)));
        g.fill(new RoundRectangle2D.Double(x, y, w, h - 2, 5, 5));
        g.setColor(new Color(0x2A6AD8));
        g.fill(new Rectangle2D.Double(x + 1, y + h * 0.55, w - 2, 2));
        g.setColor(new Color(0x24324A));
        g.fill(new RoundRectangle2D.Double(x + 2, y + 2, 6, h * 0.35, 2, 2));
        g.fill(new RoundRectangle2D.Double(x + w - 8, y + 2, 6, h * 0.35, 2, 2));
        g.setColor(new Color(0xFFE08A));
        g.fill(new Rectangle2D.Double(x + w - 1.5, y + h * 0.45, 1.5, 2));
        g.setColor(new Color(0xE84A3A));
        g.fill(new Rectangle2D.Double(x, y + h * 0.45, 1.5, 2));
        g.setColor(new Color(110, 200, 255, 180));
        g.fill(new Ellipse2D.Double(x + 5, y + h - 2.5, 8, 2.5));
        g.fill(new Ellipse2D.Double(x + w - 13, y + h - 2.5, 8, 2.5));
    }

    /** A delivery drone: a small body, two arms and the blurred discs of its rotors. */
    private static void drone(Graphics2D g, double x, double y, double w, double h) {
        g.setColor(new Color(0x3A3E48));
        g.fill(new Rectangle2D.Double(x + 1, y + 2.5, w - 2, 1.5));
        g.setColor(new Color(200, 210, 220, 140));
        g.fill(new Ellipse2D.Double(x, y, w * 0.4, 3));
        g.fill(new Ellipse2D.Double(x + w * 0.6, y, w * 0.4, 3));
        g.setColor(new Color(0xE8E8EC));
        g.fill(new RoundRectangle2D.Double(x + w * 0.25, y + 3, w * 0.5, h - 6, 3, 3));
        g.setColor(new Color(0xE84A3A));
        g.fill(new Ellipse2D.Double(x + w / 2 - 1, y + 4, 2, 2));
        g.setColor(new Color(0xC89A50));
        g.fill(new Rectangle2D.Double(x + w * 0.3, y + h - 3, w * 0.4, 3));
    }
}
