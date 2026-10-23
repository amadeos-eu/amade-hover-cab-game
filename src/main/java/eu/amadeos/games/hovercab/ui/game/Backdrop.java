package eu.amadeos.games.hovercab.ui.game;

import eu.amadeos.games.hovercab.domain.Field;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Random;

/**
 * The far away of a shift, by its theme: the sky and what stands under it, a city, mountains, the walls of a
 * mine, a planet. Drawn once into the shift's picture of the scenery; nothing of it is solid.
 */
final class Backdrop {
    private Backdrop() {
    }

    static void paint(Graphics2D g, String theme, long seed) {
        Random r = new Random(seed);
        switch (theme) {
            case "day" -> day(g, r);
            case "dusk" -> dusk(g, r);
            case "night" -> night(g, r);
            case "cave" -> cave(g, r);
            case "canyon" -> canyon(g, r);
            case "snow" -> snow(g, r);
            case "space" -> space(g, r);
            case "factory" -> factory(g, r);
            case "storm" -> storm(g, r);
            default -> throw new IllegalArgumentException("no theme " + theme);
        }
    }

    /** The colour of the sea under each theme's sky, top and bottom. */
    static Color[] sea(String theme) {
        return switch (theme) {
            case "day" -> new Color[]{new Color(0x3C8CC8), new Color(0x1C4C88)};
            case "dusk" -> new Color[]{new Color(0x3A2F62), new Color(0x141834)};
            case "snow" -> new Color[]{new Color(0x5A8AAA), new Color(0x2A4A6A)};
            case "storm" -> new Color[]{new Color(0x2A3A48), new Color(0x101820)};
            case "canyon" -> new Color[]{new Color(0x4A8A9A), new Color(0x1E4A5A)};
            default -> new Color[]{new Color(0x1C2448), new Color(0x0A0E20)};
        };
    }

    private static void sky(Graphics2D g, float[] stops, int... colours) {
        Color[] c = new Color[colours.length];
        for (int i = 0; i < c.length; i++) c[i] = new Color(colours[i]);
        g.setPaint(new LinearGradientPaint(0, (float) Field.TOP, 0, (float) Field.HEIGHT, stops, c));
        g.fill(new Rectangle2D.Double(0, 0, Field.WIDTH, Field.HEIGHT));
    }

    private static void stars(Graphics2D g, Random r, int count, double down) {
        for (int i = 0; i < count; i++) {
            double x = r.nextDouble() * Field.WIDTH, y = Field.TOP + r.nextDouble() * down;
            int a = 60 + r.nextInt(190);
            g.setColor(new Color(255, 255, 255, Math.max(30, a - (int) ((y - Field.TOP) / down * 120))));
            double s = r.nextInt(6) == 0 ? 1.5 : 1;
            g.fill(new Rectangle2D.Double(x, y, s, s));
        }
    }

    private static void glow(Graphics2D g, double x, double y, double radius, Color colour) {
        Color clear = new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), 0);
        g.setPaint(new RadialGradientPaint(new Point2D.Double(x, y), (float) radius, new float[]{0f, 1f},
                new Color[]{colour, clear}));
        g.fill(new Ellipse2D.Double(x - radius, y - radius, 2 * radius, 2 * radius));
    }

    /** Two rows of far buildings with lit windows; {@code lit} 0 to 1 is how many are. */
    private static void skyline(Graphics2D g, Random r, int back, int front, double lit, int window, double baseY) {
        int[][] rows = {{back, (int) baseY - 70, 60}, {front, (int) baseY - 44, 40}};
        for (int[] row : rows) {
            double x = -4;
            while (x < Field.WIDTH) {
                int w = 14 + r.nextInt(26), h = 20 + r.nextInt(row[2]);
                double top = row[1] + 40 - h;
                g.setColor(new Color(row[0]));
                g.fill(new Rectangle2D.Double(x, top, w, Field.HEIGHT - top));
                if (r.nextInt(4) == 0) g.fill(new Rectangle2D.Double(x + w / 2.0 - 0.5, top - 8, 1, 8));
                for (double wy = top + 4; wy < Field.HEIGHT - 4; wy += 5) {
                    for (double wx = x + 3; wx < x + w - 3; wx += 4) {
                        if (r.nextDouble() < lit * (row[0] == back ? 0.6 : 1)) {
                            Color c = new Color(r.nextInt(3) == 0 ? 0x9AD8FF : window);
                            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), row[0] == back ? 110 : 190));
                            g.fill(new Rectangle2D.Double(wx, wy, 2, 2));
                        }
                    }
                }
                x += w + r.nextInt(4);
            }
        }
    }

    private static void cloud(Graphics2D g, double x, double y, double w, Color colour) {
        g.setColor(colour);
        for (int i = 0; i < 5; i++) {
            double cx = x + w * (0.1 + 0.2 * i), size = w * (i == 2 ? 0.45 : 0.3);
            g.fill(new Ellipse2D.Double(cx - size / 2, y - size / 2 + (i % 2) * 3, size, size * 0.7));
        }
        g.fill(new Rectangle2D.Double(x + w * 0.1, y, w * 0.8, w * 0.12));
    }

    // ---- the themes ------------------------------------------------------------------------------------

    private static void day(Graphics2D g, Random r) {
        sky(g, new float[]{0f, 0.6f, 1f}, 0x3E8EE0, 0x8CC8F4, 0xD8F0FF);
        glow(g, 90, 80, 60, new Color(255, 250, 220, 120));
        g.setColor(new Color(0xFFF6D0));
        g.fill(new Ellipse2D.Double(78, 68, 24, 24));
        for (int i = 0; i < 6; i++) {
            cloud(g, r.nextDouble() * 600, 60 + r.nextDouble() * 120, 50 + r.nextDouble() * 60, new Color(255, 255, 255, 200));
        }
        skyline(g, r, 0xA8C4DC, 0x88A8C4, 0.0, 0xFFFFFF, 320);
    }

    private static void dusk(Graphics2D g, Random r) {
        sky(g, new float[]{0f, 0.45f, 0.8f, 1f}, 0x141A3C, 0x34306A, 0xA4507A, 0xF09A60);
        stars(g, r, 70, 140);
        glow(g, 430, 72, 40, new Color(255, 244, 216, 70));
        g.setColor(new Color(0xFBEFD2));
        g.fill(new Ellipse2D.Double(421, 63, 18, 18));
        g.setColor(new Color(0x34306A));
        g.fill(new Ellipse2D.Double(427, 60, 16, 16));
        skyline(g, r, 0x4A3A72, 0x2C2550, 0.18, 0xFFD27A, 330);
    }

    private static void night(Graphics2D g, Random r) {
        sky(g, new float[]{0f, 0.7f, 1f}, 0x070A1C, 0x161C40, 0x2A2850);
        stars(g, r, 160, 260);
        glow(g, 520, 80, 34, new Color(220, 230, 255, 60));
        g.setColor(new Color(0xE8ECF8));
        g.fill(new Ellipse2D.Double(510, 70, 20, 20));
        skyline(g, r, 0x1E2246, 0x12142C, 0.3, 0xFFC860, 320);
    }

    private static void cave(Graphics2D g, Random r) {
        sky(g, new float[]{0f, 1f}, 0x2A1E18, 0x120C0A);
        // the far wall of the mine: rough rock, a few timbers and lamps
        for (int i = 0; i < 900; i++) {
            double x = r.nextDouble() * Field.WIDTH, y = r.nextDouble() * Field.HEIGHT;
            g.setColor(new Color(60 + r.nextInt(30), 44 + r.nextInt(20), 34 + r.nextInt(14), 90));
            g.fill(new Ellipse2D.Double(x, y, 3 + r.nextDouble() * 9, 2 + r.nextDouble() * 5));
        }
        for (int i = 0; i < 5; i++) {
            double x = 40 + i * 130 + r.nextDouble() * 40;
            g.setColor(new Color(0x3A2818));
            g.fill(new Rectangle2D.Double(x, 120, 5, 280));
            g.fill(new Rectangle2D.Double(x - 30, 118, 65, 5));
            glow(g, x + 2, 132, 26, new Color(255, 200, 110, 70));
            g.setColor(new Color(0xFFD890));
            g.fill(new Ellipse2D.Double(x, 128, 5, 5));
        }
        for (int i = 0; i < 14; i++) {      // crystals glinting in the dark
            double x = r.nextDouble() * Field.WIDTH, y = 60 + r.nextDouble() * 300;
            g.setColor(new Color(120, 220, 230, 80));
            g.fill(new Polygon(new int[]{(int) x, (int) x + 2, (int) x + 4}, new int[]{(int) y + 6, (int) y, (int) y + 6}, 3));
        }
    }

    private static void canyon(Graphics2D g, Random r) {
        sky(g, new float[]{0f, 0.6f, 1f}, 0x4E78C0, 0xE8A870, 0xF8D090);
        glow(g, 520, 120, 70, new Color(255, 240, 200, 110));
        g.setColor(new Color(0xFFF2D0));
        g.fill(new Ellipse2D.Double(506, 106, 28, 28));
        // mesas, three layers, nearer ones darker
        int[] colours = {0xD8A078, 0xB8785A, 0x8A5440};
        for (int layer = 0; layer < 3; layer++) {
            double base = 250 + layer * 40;
            Path2D p = new Path2D.Double();
            p.moveTo(0, Field.HEIGHT);
            double x = 0;
            p.lineTo(0, base);
            while (x < Field.WIDTH) {
                double w = 40 + r.nextDouble() * 90, h = r.nextDouble() < 0.4 ? 40 + r.nextDouble() * 60 : 5;
                p.lineTo(x + 8, base - h);
                p.lineTo(x + w - 8, base - h);
                p.lineTo(x + w, base);
                x += w;
            }
            p.lineTo(Field.WIDTH, Field.HEIGHT);
            p.closePath();
            g.setColor(new Color(colours[layer]));
            g.fill(p);
        }
    }

    private static void snow(Graphics2D g, Random r) {
        sky(g, new float[]{0f, 0.7f, 1f}, 0x7AA8D8, 0xC8DCF0, 0xF0F4FA);
        int[] colours = {0xB8CCE4, 0x96AED0};
        for (int layer = 0; layer < 2; layer++) {
            double base = 260 + layer * 50;
            Path2D p = new Path2D.Double();
            p.moveTo(0, Field.HEIGHT);
            p.lineTo(0, base);
            double x = 0;
            while (x < Field.WIDTH) {
                double w = 70 + r.nextDouble() * 110, h = 50 + r.nextDouble() * 110;
                p.lineTo(x + w / 2, base - h);
                p.lineTo(x + w, base);
                x += w;
            }
            p.lineTo(Field.WIDTH, Field.HEIGHT);
            p.closePath();
            g.setColor(new Color(colours[layer]));
            g.fill(p);
            g.setColor(new Color(255, 255, 255, 150));
            java.awt.Shape clip = g.getClip();
            g.clip(p);
            g.fill(new Rectangle2D.Double(0, 0, Field.WIDTH, base - 50 + layer * 10));
            g.setClip(clip);
        }
    }

    private static void space(Graphics2D g, Random r) {
        sky(g, new float[]{0f, 1f}, 0x02030A, 0x0A0C1E);
        glow(g, 470, 200, 160, new Color(110, 60, 160, 40));
        glow(g, 150, 120, 120, new Color(40, 90, 160, 40));
        stars(g, r, 260, 380);
        // a blue planet low on the left, lit from the right
        double px = 90, py = 360, pr = 110;
        g.setPaint(new RadialGradientPaint(new Point2D.Double(px + 40, py - 40), (float) pr * 1.3f, new float[]{0f, 0.6f, 1f},
                new Color[]{new Color(0x6AB0F0), new Color(0x2A60A8), new Color(0x081830)}));
        g.fill(new Ellipse2D.Double(px - pr, py - pr, 2 * pr, 2 * pr));
        g.setColor(new Color(255, 255, 255, 70));
        for (int i = 0; i < 6; i++) {
            g.fill(new Ellipse2D.Double(px - 60 + r.nextDouble() * 120, py - 90 + r.nextDouble() * 80, 30 + r.nextDouble() * 40, 6));
        }
        glow(g, px, py, pr + 14, new Color(120, 190, 255, 50));
    }

    private static void factory(Graphics2D g, Random r) {
        sky(g, new float[]{0f, 0.7f, 1f}, 0x4A4048, 0x9A7058, 0xC89060);
        for (int i = 0; i < 8; i++) {
            double x = r.nextDouble() * Field.WIDTH, w = 10 + r.nextDouble() * 10, top = 150 + r.nextDouble() * 90;
            g.setColor(new Color(0x4A3C3C));
            g.fill(new Rectangle2D.Double(x, top, w, Field.HEIGHT - top));
            g.setColor(new Color(0xB04A3A));
            g.fill(new Rectangle2D.Double(x, top + 6, w, 3));
            for (int s = 0; s < 6; s++) {                     // smoke
                g.setColor(new Color(90, 80, 80, 50 - s * 6));
                double size = 12 + s * 8;
                g.fill(new Ellipse2D.Double(x + w / 2 - size / 2 + s * 9, top - 10 - s * 14, size, size * 0.8));
            }
        }
        skyline(g, r, 0x6A5050, 0x4A3A3A, 0.05, 0xFFB060, 330);
    }

    private static void storm(Graphics2D g, Random r) {
        sky(g, new float[]{0f, 0.6f, 1f}, 0x1A2028, 0x34404C, 0x4A5660);
        for (int i = 0; i < 9; i++) {
            cloud(g, r.nextDouble() * 640 - 40, 50 + r.nextDouble() * 80, 90 + r.nextDouble() * 80, new Color(20, 26, 34, 140));
        }
        skyline(g, r, 0x2A3440, 0x1C242E, 0.12, 0xFFD27A, 330);
    }

    /** A box filled from one colour at the top to another at the bottom. */
    static void gradient(Graphics2D g, double x, double y, double w, double h, Color top, Color bottom) {
        g.setPaint(new GradientPaint((float) x, (float) y, top, (float) x, (float) (y + h), bottom));
        g.fill(new Rectangle2D.Double(x, y, w, h));
    }
}
