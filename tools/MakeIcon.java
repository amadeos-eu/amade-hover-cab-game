import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Draws the program icon of Hover Cab: the yellow cab with its checkered band, gear out and the main jet
 * burning, over a landing pad, against a dusk sky. Drawn as shapes on a 48 × 48 grid, so it is sharp at
 * every size.
 *
 * <pre>
 * java tools/MakeIcon.java 48 src/main/resources/icons/amade-hover-cab-game.png
 * java tools/MakeIcon.java 256 build/icon-256.png
 * python3 -c "from PIL import Image; Image.open('build/icon-256.png').save('packaging/windows/amade-hover-cab-game.ico',
 *     sizes=[(16,16),(24,24),(32,32),(48,48),(64,64),(128,128),(256,256)])"
 * </pre>
 *
 * Run it in the repository's folder. The files are part of the repository; run this again only to change
 * the icon.
 */
public class MakeIcon {
    public static void main(String[] args) throws IOException {
        int size = Integer.parseInt(args[0]);
        File file = new File(args[1]);
        if (file.getParentFile() != null) file.getParentFile().mkdirs();
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.scale(size / 48.0, size / 48.0);
        paint(g);
        g.dispose();
        ImageIO.write(image, "png", file);
    }

    static void paint(Graphics2D g) {
        // The sky, a rounded square.
        RoundRectangle2D frame = new RoundRectangle2D.Double(1, 1, 46, 46, 10, 10);
        g.setPaint(new GradientPaint(0, 1, new Color(0x2A2A5A), 0, 47, new Color(0xD86A4A)));
        g.fill(frame);
        g.setClip(frame);
        g.setColor(new Color(255, 240, 200));
        g.fill(new Ellipse2D.Double(34, 6, 6, 6));
        // The pad: a deck with yellow lights on a dark tower.
        g.setColor(new Color(0x30343E));
        g.fill(new Rectangle2D.Double(10, 40, 28, 8));
        g.setColor(new Color(0x8A8E98));
        g.fill(new Rectangle2D.Double(8, 38, 32, 3));
        g.setColor(new Color(0xF2C230));
        for (int i = 0; i < 6; i++) g.fill(new Rectangle2D.Double(9.5 + i * 5.2, 38.4, 2, 1.2));
        g.setClip(null);
        // The flame of the main jet.
        Path2D flame = new Path2D.Double();
        flame.moveTo(20.5, 27);
        flame.lineTo(27.5, 27);
        flame.lineTo(24, 36.5);
        flame.closePath();
        g.setPaint(new GradientPaint(0, 27, new Color(0xFFF0A0), 0, 36, new Color(0xFF6A2A)));
        g.fill(flame);
        // The gear.
        g.setColor(new Color(0x2A2A2A));
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new java.awt.geom.Line2D.Double(14, 26, 11.5, 31.5));
        g.draw(new java.awt.geom.Line2D.Double(34, 26, 36.5, 31.5));
        g.draw(new java.awt.geom.Line2D.Double(9.5, 31.5, 13.5, 31.5));
        g.draw(new java.awt.geom.Line2D.Double(34.5, 31.5, 38.5, 31.5));
        // The hull: a rounded body with a cabin on top.
        Path2D hull = new Path2D.Double();
        hull.moveTo(5, 24);
        hull.curveTo(5, 19, 8, 18, 13, 17.5);
        hull.lineTo(16, 11.5);
        hull.curveTo(17, 10, 18, 9.5, 20, 9.5);
        hull.lineTo(30, 9.5);
        hull.curveTo(32, 9.5, 33, 10, 34, 11.5);
        hull.lineTo(37, 17.5);
        hull.curveTo(41, 18, 43, 19, 43, 24);
        hull.curveTo(43, 26.5, 41, 27.5, 38, 27.5);
        hull.lineTo(10, 27.5);
        hull.curveTo(7, 27.5, 5, 26.5, 5, 24);
        hull.closePath();
        g.setPaint(new GradientPaint(0, 9, new Color(0xFFE070), 0, 28, new Color(0xE8A818)));
        g.fill(hull);
        g.setColor(new Color(0x6A4A10));
        g.setStroke(new BasicStroke(0.9f));
        g.draw(hull);
        // The windows of the cabin.
        g.setColor(new Color(0x24324A));
        Path2D front = new Path2D.Double();
        front.moveTo(26, 11.5);
        front.lineTo(31, 11.5);
        front.lineTo(34, 17);
        front.lineTo(26, 17);
        front.closePath();
        g.fill(front);
        Path2D back = new Path2D.Double();
        back.moveTo(18.5, 11.5);
        back.lineTo(24, 11.5);
        back.lineTo(24, 17);
        back.lineTo(16, 17);
        back.closePath();
        g.fill(back);
        g.setColor(new Color(160, 200, 255, 150));
        g.fill(new Rectangle2D.Double(27, 12.2, 2, 1.2));
        // The checkered band.
        for (int i = 0; i < 12; i++) {
            g.setColor(i % 2 == 0 ? new Color(0x1A1A1A) : new Color(0xFFF6D0));
            g.fill(new Rectangle2D.Double(7 + i * 2.85, 20, 2.85, 1.8));
            g.setColor(i % 2 == 1 ? new Color(0x1A1A1A) : new Color(0xFFF6D0));
            g.fill(new Rectangle2D.Double(7 + i * 2.85, 21.8, 2.85, 1.8));
        }
        // The roof light.
        g.setColor(new Color(0xFF5A3A));
        g.fill(new RoundRectangle2D.Double(22, 7, 5, 2.8, 1.5, 1.5));
    }
}
