package eu.amadeos.games.hovercab.ui.game;

import eu.amadeos.gameengine.audio.Speech;
import eu.amadeos.games.hovercab.domain.Rider;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/**
 * The passengers: six kinds of people, each with a look and a voice of their own, so that the one who calls
 * "Hey, cab!" in a deep voice is the man in the hat. Which kind a passenger is follows from their place in
 * the shift's list and the shift's number.
 */
final class People {
    /** A woman with a suitcase, a man in a hat, a boy in a cap, an old man with a stick, a woman with a ponytail, a builder. */
    static final int KINDS = 6;

    // --8<-- [start:voices]
    /** The voice of each kind. */
    static final Speech.Voice[] VOICES = {
            Speech.Voice.WOMAN,
            Speech.Voice.MAN,
            Speech.Voice.CHILD,
            Speech.Voice.OLD_MAN,
            Speech.Voice.WOMAN.withPitch(235),
            Speech.Voice.MAN.withPitch(92)};
    // --8<-- [end:voices]

    private static final double SCALE = 1.3;

    private People() {
    }

    static int kind(Rider rider, int shift) {
        return kind(rider.index(), shift);
    }

    /** The kind of the shift's {@code index}th passenger. */
    static int kind(int index, int shift) {
        return (index + shift) % KINDS;
    }

    /** A person standing, waving or walking, the feet at {@code x}, {@code feet}. */
    static void paint(Graphics2D g, int kind, double x, double feet, boolean facingLeft, boolean waving,
                      boolean walking, double time) {
        AffineTransform saved = g.getTransform();
        g.translate(x, feet);
        double scale = kind == 2 ? SCALE * 0.75 : SCALE;
        g.scale(facingLeft ? -scale : scale, scale);
        g.setColor(new Color(0, 0, 0, 70));
        g.fill(new Ellipse2D.Double(-4, -0.8, 8, 1.6));
        Color coat = switch (kind) {
            case 0 -> new Color(0x2A8A9A);
            case 1 -> new Color(0x3A3E58);
            case 2 -> new Color(0xD84A3A);
            case 3 -> new Color(0x7A6A50);
            case 4 -> new Color(0x8A4AB0);
            default -> new Color(0xF08A2A);
        };
        Color skin = switch (kind) {
            case 1, 5 -> new Color(0xB07850);
            case 3 -> new Color(0xE8C0A0);
            default -> new Color(0xE0A888);
        };
        // legs: a step while walking
        double swing = walking ? Math.sin(time * 14) * 1.4 : 0;
        g.setColor(kind == 5 ? new Color(0x3A4A6A) : new Color(0x2A2A3A));
        g.fill(new Rectangle2D.Double(-1.6 + swing, -5, 1.2, 5));
        g.fill(new Rectangle2D.Double(0.4 - swing, -5, 1.2, 5));
        // body
        g.setColor(coat);
        Path2D body = new Path2D.Double();
        body.moveTo(-2.6, -5);
        body.lineTo(2.6, -5);
        body.lineTo(1.8, -11);
        body.lineTo(-1.8, -11);
        body.closePath();
        g.fill(body);
        // head
        g.setColor(skin);
        g.fill(new Ellipse2D.Double(-1.8, -14.4, 3.6, 3.6));
        hair(g, kind);
        // arms: one waves while waiting
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(coat);
        if (waving) {
            double wave = Math.sin(time * 9) * 0.8;
            g.draw(new Line2D.Double(1.2, -10.4, 3.2 + wave, -14));
            g.setColor(skin);
            g.fill(new Ellipse2D.Double(2.5 + wave, -15.6, 1.6, 1.6));
        } else {
            g.draw(new Line2D.Double(1.2, -10.4, 1.8 - swing * 0.6, -6.5));
        }
        g.setColor(coat);
        g.draw(new Line2D.Double(-1.2, -10.4, -1.8 + swing * 0.6, -6.5));
        g.setStroke(new BasicStroke(1));
        things(g, kind);
        g.setTransform(saved);
    }

    private static void hair(Graphics2D g, int kind) {
        switch (kind) {
            case 0 -> {
                g.setColor(new Color(0x5A2E1A));
                g.fill(new Arc2D.Double(-2.1, -14.7, 4.2, 3.4, 0, 180, Arc2D.PIE));
                g.fill(new Rectangle2D.Double(-2, -13, 1, 3));
            }
            case 1 -> {
                g.setColor(new Color(0x1E1E26));
                g.fill(new Rectangle2D.Double(-2.6, -13.6, 5.2, 0.8));
                g.fill(new Rectangle2D.Double(-1.7, -16, 3.4, 2.6));
            }
            case 2 -> {
                g.setColor(new Color(0x2A5AD8));
                g.fill(new Arc2D.Double(-2, -14.8, 4, 3.4, 0, 180, Arc2D.PIE));
                g.fill(new Rectangle2D.Double(0, -13.4, 3, 0.8));
            }
            case 3 -> {
                g.setColor(new Color(0xD8D8D8));
                g.fill(new Arc2D.Double(-2, -14.4, 4, 2.6, 0, 180, Arc2D.PIE));
            }
            case 4 -> {
                g.setColor(new Color(0xE8C060));
                g.fill(new Arc2D.Double(-2.1, -14.8, 4.2, 3.4, 0, 180, Arc2D.PIE));
                g.fill(new Ellipse2D.Double(-3.6, -13.6, 2, 3.4));
            }
            default -> {
                g.setColor(new Color(0xF2C230));
                g.fill(new Arc2D.Double(-2.4, -15.2, 4.8, 4, 0, 180, Arc2D.PIE));
                g.fill(new Rectangle2D.Double(-2.6, -13.4, 5.2, 0.6));
            }
        }
    }

    /** What each kind carries: a suitcase, a briefcase, nothing, a stick, a handbag, a toolbox. */
    private static void things(Graphics2D g, int kind) {
        switch (kind) {
            case 0 -> {
                g.setColor(new Color(0xB04A3A));
                g.fill(new RoundRectangle2D.Double(-7.5, -5, 5, 4, 1, 1));
                g.setColor(new Color(0x6A2A20));
                g.fill(new Rectangle2D.Double(-5.5, -6, 1, 1));
            }
            case 1 -> {
                g.setColor(new Color(0x5A3A20));
                g.fill(new Rectangle2D.Double(-4.5, -7, 3, 2.4));
            }
            case 3 -> {
                g.setColor(new Color(0x6A4A2A));
                g.draw(new Line2D.Double(-2.4, -7, -3.4, 0));
            }
            case 4 -> {
                g.setColor(new Color(0xE84A8A));
                g.fill(new RoundRectangle2D.Double(-4.2, -8, 2.4, 2.4, 1, 1));
            }
            case 5 -> {
                g.setColor(new Color(0xC83A2A));
                g.fill(new Rectangle2D.Double(-6, -4, 4, 2.6));
            }
            default -> {
            }
        }
    }
}
