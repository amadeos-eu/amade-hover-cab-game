package eu.amadeos.games.hovercab.ui.game;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.physics.Body;
import eu.amadeos.games.hovercab.domain.Cab;
import eu.amadeos.games.hovercab.domain.Event;
import eu.amadeos.games.hovercab.domain.Field;
import eu.amadeos.games.hovercab.domain.Game;
import eu.amadeos.games.hovercab.domain.GameResult;
import eu.amadeos.games.hovercab.domain.Rider;
import eu.amadeos.games.hovercab.domain.ShiftResult;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The overlay: the top bar with the shift, the money, the tip, the fuel, the passengers and the spare cabs;
 * the speech bubbles and the money rising from a paid fare; the shift's name at its start; and the texts of
 * the title, the pause, the end of a shift and the end of the game. Drawn after the world, at the screen's
 * full size, so the letters are sharp in every render mode.
 */
final class Hud {
    static final Color CHALK = Color.WHITE;
    static final Color GOLD = new Color(0xF2C230);
    static final Color GREEN = new Color(0x8AFFB0);
    static final Color RED = new Color(0xFF5A3A);
    static final Color GREY = new Color(0x9AA0B8);
    static final Color VEIL = new Color(0, 0, 0, 150);
    static final Color SHADOW = new Color(0, 0, 0, 140);
    static final Color BUBBLE = new Color(0xFFFBEA);
    static final Color INK = new Color(0x2A2240);
    static final double BANNER_SECONDS = 2.6;

    private final Messages msg;
    private final Map<Float, Font> plain = new HashMap<>();
    private final Map<Float, Font> bold = new HashMap<>();

    Hud(Messages msg) {
        this.msg = msg;
    }

    /** Cents as dollars: 4850 is "$48.50". */
    static String dollars(long cents) {
        return String.format(Locale.ROOT, "$%d.%02d", cents / 100, cents % 100);
    }

    // --8<-- [start:status]
    /** The bar along the top. */
    void status(Graphics2D g, Game game) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, new Color(0x2A2E40), 0, (float) Field.TOP, new Color(0x161924)));
        g.fill(new Rectangle2D.Double(0, 0, Field.WIDTH, Field.TOP));
        g.setColor(GOLD);
        g.fill(new Rectangle2D.Double(0, Field.TOP - 1, Field.WIDTH, 1));
        int shift = game.shift().number();
        g.setFont(font(8.5f, true));
        g.setColor(GOLD);
        String label = msg.format("hud.shift", shift);
        g.drawString(label, 8, 14);
        double x = 8 + g.getFontMetrics().stringWidth(label) + 6;
        g.setFont(font(8.5f, false));
        g.setColor(CHALK);
        g.drawString(name(shift), (float) x, 14);

        label(g, msg.get("hud.fares"), 150);
        g.setFont(font(9, true));
        g.setColor(CHALK);
        g.drawString(dollars(game.career().cents()), 180, 14.5f);

        Rider rider = game.rider();
        boolean riding = rider != null && rider.state() == Rider.State.RIDING;
        label(g, msg.get("hud.tip"), 236);
        g.setFont(font(9, true));
        g.setColor(riding ? GREEN : GREY);
        int tip = riding ? rider.tip() : Field.TIP_START;
        g.drawString(dollars(tip), 254, 14.5f);
        g.setColor(new Color(0x3A3E50));
        g.fill(new Rectangle2D.Double(290, 9, 30, 4));
        g.setColor(riding ? GREEN : GREY);
        g.fill(new Rectangle2D.Double(290, 9, 30.0 * tip / Field.TIP_START, 4));

        Cab cab = game.cab();
        label(g, msg.get("hud.fuel"), 334);
        g.setColor(new Color(0x3A3E50));
        g.fill(new Rectangle2D.Double(358, 8, 70, 6));
        boolean low = cab.fuel() < Field.LOW_FUEL;
        if (!low || (game.time() * 3) % 1 < 0.6) {
            g.setPaint(new GradientPaint(358, 0, RED, 428, 0, GREEN));
            g.fill(new Rectangle2D.Double(358, 8, 70 * cab.fuel() / Field.TANK, 6));
        }
        g.setColor(new Color(0x161924));
        for (double t = 372; t < 428; t += 14) g.fill(new Rectangle2D.Double(t, 8, 1, 6));

        label(g, msg.get("hud.passengers"), 442);
        g.setFont(font(9, true));
        g.setColor(CHALK);
        String count = game.carried() + " / " + game.shift().fares().size();
        g.drawString(count, 442 + g.getFontMetrics().stringWidth(msg.get("hud.passengers")) * 0.8f + 6, 14.5f);

        int spare = game.career().spare();
        for (int i = 0; i < Math.min(spare, 4); i++) miniCab(g, 574 + i * 16, 15);
        if (spare > 4) {
            g.setFont(font(7, true));
            g.setColor(CHALK);
            g.drawString("+" + (spare - 4), 574 + 4 * 16 - 4, 14);
        }
        g.setFont(font(7, false));
        g.setColor(cab.gearOut() ? GREEN : GREY);
        String gear = cab.gearOut() ? "↓" : "↑";
        g.drawString(gear, 632, 14);
    }
    // --8<-- [end:status]

    private void label(Graphics2D g, String text, double x) {
        g.setFont(font(6.5f, true));
        g.setColor(GREY);
        g.drawString(text, (float) x, 14);
    }

    private static void miniCab(Graphics2D g, double x, double y) {
        AffineTransform saved = g.getTransform();
        g.translate(x - 7, y - 7);
        g.scale(0.42, 0.42);
        CabArt.body(g, 0, false);
        g.setTransform(saved);
    }

    /** The shift's name at its start, a crash's cause after a crash. */
    void banners(Graphics2D g, Game game, Event.Cause crash, double sinceCrash) {
        if (game.time() < BANNER_SECONDS) {
            int number = game.shift().number();
            fading(g, msg.format("play.shift", number, name(number)), 150, 20, GOLD, game.time());
            if (game.shift().windy()) fading(g, msg.get("play.windy"), 172, 11, CHALK, game.time());
            else if (game.shift().gravity() < Field.GRAVITY * 0.8) fading(g, msg.get("play.light"), 172, 11, CHALK, game.time());
        }
        if (crash != null && game.phase() == Game.Phase.CRASHED) {
            fading(g, msg.get("crash." + crash.name().toLowerCase(Locale.ROOT)), 190, 16, RED, sinceCrash);
        }
    }

    private void fading(Graphics2D g, String line, double y, float size, Color colour, double shown) {
        double left = BANNER_SECONDS - shown;
        int alpha = (int) Math.round(255 * Math.clamp(left / 0.5, 0, 1));
        g.setFont(font(size, true));
        double x = (Field.WIDTH - g.getFontMetrics().stringWidth(line)) / 2;
        g.setColor(new Color(0, 0, 0, alpha / 2));
        g.drawString(line, (float) x + 1.5f, (float) y + 1.5f);
        g.setColor(new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), alpha));
        g.drawString(line, (float) x, (float) y);
    }

    // --8<-- [start:bubble]
    /** The speech bubbles: over a passenger on a pad, or over the cab when they speak from inside it. */
    void bubbles(Graphics2D g, Game game, Effects effects) {
        g.setFont(font(8, true));
        for (Effects.Bubble bubble : effects.bubbles()) {
            Rider rider = bubble.rider();
            double tipX, tipY;
            boolean fromCab = !rider.visible();
            if (fromCab) {
                if (game.phase() == Game.Phase.CRASHED) continue;
                Body body = game.cab().body();
                tipX = body.centerX() + 2;
                tipY = body.y - 5;
            } else {
                tipX = rider.x() + 2;
                tipY = rider.pad().top(game.time()) - 24;
            }
            double age = effects.time() - bubble.born();
            double grow = Math.min(1, age / 0.12);
            bubble(g, bubble.text(), tipX, tipY, grow);
        }
        for (Effects.Money money : effects.money()) {
            double age = effects.time() - money.born();
            int alpha = (int) (255 * Math.clamp((Effects.MONEY_SECONDS - age) / 0.5, 0, 1));
            g.setFont(font(10, true));
            g.setColor(new Color(0, 0, 0, alpha / 2));
            g.drawString(money.text(), (float) money.x() - 14, (float) (money.y() - 8 - age * 20) + 1);
            g.setColor(new Color(GREEN.getRed(), GREEN.getGreen(), GREEN.getBlue(), alpha));
            g.drawString(money.text(), (float) money.x() - 15, (float) (money.y() - 9 - age * 20));
        }
    }

    private void bubble(Graphics2D g, String text, double tipX, double tipY, double grow) {
        FontMetrics fm = g.getFontMetrics();
        double w = fm.stringWidth(text) + 12, h = 15;
        double bx = Math.clamp(tipX - 12, 2, Field.WIDTH - w - 2);
        double by = tipY - h - 5;
        if (by < Field.CEILING + 2) by = Field.CEILING + 2;
        AffineTransform saved = g.getTransform();
        g.translate(tipX, tipY);
        g.scale(grow, grow);
        g.translate(-tipX, -tipY);
        g.setColor(new Color(0, 0, 0, 70));
        g.fill(new RoundRectangle2D.Double(bx + 1, by + 1, w, h, 8, 8));
        Path2D tail = new Path2D.Double();
        tail.moveTo(Math.clamp(tipX - 2, bx + 4, bx + w - 10), by + h - 1);
        tail.lineTo(tipX, tipY);
        tail.lineTo(Math.clamp(tipX + 5, bx + 8, bx + w - 4), by + h - 1);
        tail.closePath();
        g.setColor(BUBBLE);
        g.fill(new RoundRectangle2D.Double(bx, by, w, h, 8, 8));
        g.fill(tail);
        g.setColor(INK);
        g.drawString(text, (float) (bx + 6), (float) (by + 10.5));
        g.setTransform(saved);
    }
    // --8<-- [end:bubble]

    // ---- the screens around the play ---------------------------------------------------------------------

    void title(Graphics2D g, double time) {
        veil(g);
        g.setFont(font(54, true));
        String title = msg.get("window.title");
        double x = (Field.WIDTH - g.getFontMetrics().stringWidth(title)) / 2;
        g.setColor(new Color(0, 0, 0, 120));
        g.drawString(title, (float) x + 3, 133);
        g.setColor(GOLD);
        g.drawString(title, (float) x, 130);
        // a checkered band under the name, as on the cab
        for (int i = 0; i < 40; i++) {
            g.setColor(i % 2 == 0 ? new Color(0x1A1A1A) : new Color(0xFFF0C0));
            g.fill(new Rectangle2D.Double(Field.WIDTH / 2 - 120 + i * 6, 142, 6, 3));
            g.setColor(i % 2 == 1 ? new Color(0x1A1A1A) : new Color(0xFFF0C0));
            g.fill(new Rectangle2D.Double(Field.WIDTH / 2 - 120 + i * 6, 145, 6, 3));
        }
        text(g, msg.get("title.tagline"), 172, 12, false, CHALK);
        if ((time * 1.5) % 1 < 0.7) text(g, msg.get("title.start"), 240, 16, true, CHALK);
        text(g, msg.get("title.keys"), 290, 10, false, CHALK);
        text(g, msg.get("title.land"), 306, 10, false, CHALK);
        text(g, msg.get("title.screen"), 322, 10, false, GREY);
    }

    void pause(Graphics2D g) {
        veil(g);
        text(g, msg.get("pause.title"), 190, 36, true, CHALK);
        text(g, msg.get("pause.continue"), 230, 13, false, CHALK);
        text(g, msg.get("pause.leave"), 250, 11, false, GREY);
    }

    /** The end of a shift: what it brought in. */
    void cleared(Graphics2D g, ShiftResult result, long total, boolean last) {
        veil(g);
        text(g, msg.format("cleared.title", result.shift()), 110, 28, true, GOLD);
        text(g, msg.format("cleared.fares", dollars(result.fares())), 160, 15, false, CHALK);
        text(g, msg.format("cleared.tips", dollars(result.tips())), 184, 15, false, GREEN);
        text(g, msg.format("cleared.passengers", result.passengers()), 208, 13, false, CHALK);
        text(g, msg.format("cleared.time", clock(result.seconds())), 228, 13, false, CHALK);
        if (result.crashes() > 0) text(g, msg.format("cleared.crashes", result.crashes()), 248, 13, false, RED);
        text(g, msg.format("cleared.total", dollars(total)), 285, 17, true, GOLD);
        text(g, msg.get(last ? "cleared.last" : "cleared.next"), 340, 12, false, CHALK);
    }

    void over(Graphics2D g, GameResult result) {
        veil(g);
        text(g, msg.get(result.finished() ? "over.finished" : "over.lost"), 170, 30, true, result.finished() ? GOLD : CHALK);
        text(g, msg.format("over.money", dollars(result.cents()), result.shift()), 215, 15, false, CHALK);
        text(g, msg.get("over.continue"), 270, 12, false, GREY);
    }

    /** The name of shift {@code number} in the player's language. */
    String name(int number) {
        return msg.get("shift." + number);
    }

    /** Seconds as minutes and seconds: 83.4 is "1:23". */
    static String clock(double seconds) {
        int whole = (int) Math.floor(seconds);
        return String.format(Locale.ROOT, "%d:%02d", whole / 60, whole % 60);
    }

    void veil(Graphics2D g) {
        g.setColor(VEIL);
        g.fill(new Rectangle2D.Double(0, 0, Field.WIDTH, Field.HEIGHT));
    }

    /** A line of text centred across the screen, its baseline at {@code y}. */
    void text(Graphics2D g, String line, double y, float size, boolean strong, Color colour) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(font(size, strong));
        double x = (Field.WIDTH - g.getFontMetrics().stringWidth(line)) / 2;
        g.setColor(SHADOW);
        g.drawString(line, (float) x + 1, (float) y + 1);
        g.setColor(colour);
        g.drawString(line, (float) x, (float) y);
    }

    Font font(float size, boolean strong) {
        return (strong ? bold : plain).computeIfAbsent(size,
                s -> new Font(Font.SANS_SERIF, strong ? Font.BOLD : Font.PLAIN, 1).deriveFont(s));
    }
}
