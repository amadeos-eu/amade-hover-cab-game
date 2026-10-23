package eu.amadeos.games.hovercab.ui.game;

import eu.amadeos.gameengine.physics.Body;
import eu.amadeos.gameengine.random.SeededRandom;
import eu.amadeos.gameengine.sprite.Sprite;
import eu.amadeos.games.hovercab.domain.Cab;
import eu.amadeos.games.hovercab.domain.Field;
import eu.amadeos.games.hovercab.domain.Game;
import eu.amadeos.games.hovercab.domain.Mover;
import eu.amadeos.games.hovercab.domain.Pad;
import eu.amadeos.games.hovercab.domain.Rider;
import eu.amadeos.games.hovercab.domain.Shift;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Draws a shift being flown, in logical coordinates: the scenery's picture, then what moves in it (lifts,
 * moving blocks, the gate, the passengers, the cab, the particles, the weather). Everything that does not
 * change is in the scenery's {@link Sprite}; what changes is little and quick to draw.
 */
final class Painter {
    private final Map<Integer, Sprite> scenery = new HashMap<>();
    private final Map<Mover, Sprite> movers = new HashMap<>();
    private final double[] drops = new double[240];

    Painter() {
        Random r = new Random(5);
        for (int i = 0; i < drops.length; i++) drops[i] = r.nextDouble();
    }

    // --8<-- [start:paint]
    void paint(Graphics2D g, Game game, Effects effects, double alpha) {
        Shift shift = game.shift();
        double time = game.time();
        double shake = effects.shake();
        AffineTransform saved = g.getTransform();
        if (shake > 0) {
            SeededRandom r = new SeededRandom((long) (effects.time() * 60));
            g.translate((r.nextDouble() - 0.5) * shake, (r.nextDouble() - 0.5) * shake);
        }
        scenery.computeIfAbsent(shift.number(), n -> Scenery.of(shift)).draw(g, 0, 0);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (Mover mover : shift.movers()) {
            movers.computeIfAbsent(mover, m -> Sprite.drawn(m.width(), m.height(), (mg, w, h) -> {
                mg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Scenery.mover(mg, new Mover(m.look(), 0, 0, w, h, 0, 0, 1, 0), 0);
            })).draw(g, mover.x(time), mover.y(time));
        }
        for (Pad pad : shift.pads()) if (pad.isLift()) Scenery.lift(g, pad, pad.top(time));
        Scenery.gate(g, shift, game.gateOpen(), time);
        target(g, game, time);
        Rider rider = game.rider();
        if (rider != null && rider.visible()) {
            boolean waving = rider.state() == Rider.State.WAITING;
            People.paint(g, People.kind(rider, shift.number()), rider.x(), rider.pad().top(time), rider.facingLeft(),
                    waving, !waving, rider.stateTime());
        }
        if (game.phase() != Game.Phase.CRASHED && game.phase() != Game.Phase.OVER) {
            Cab cab = game.cab();
            Body body = cab.body();
            boolean aboard = rider != null && rider.state() == Rider.State.RIDING;
            CabArt.paint(g, cab, body.x(alpha), body.y(alpha), aboard, (effects.time() * 37) % 1);
        }
        effects.render(g, alpha);
        weather(g, shift, time);
        g.setTransform(saved);
    }
    // --8<-- [end:paint]

    /** A small arrow bobbing over the pad the passenger aboard wants. */
    private static void target(Graphics2D g, Game game, double time) {
        Rider rider = game.rider();
        if (rider == null || rider.state() != Rider.State.RIDING || rider.to() == Shift.UP) return;
        Pad pad = game.shift().pad(rider.to()).orElseThrow();
        double x = pad.x() + 6, y = pad.top(time) - 30 - Math.abs(Math.sin(time * 4)) * 4;
        g.setColor(new Color(255, 230, 90, 220));
        g.fill(new Polygon(new int[]{(int) x - 4, (int) x + 4, (int) x}, new int[]{(int) y, (int) y, (int) y + 5}, 3));
    }

    // --8<-- [start:weather]
    /** Rain in a storm, snow in the mountains, streaks of wind where it blows. */
    private void weather(Graphics2D g, Shift shift, double time) {
        double wind = shift.wind(time);
        if (shift.theme().equals("storm")) {
            g.setColor(new Color(170, 190, 220, 110));
            g.setStroke(new BasicStroke(0.8f));
            double slant = wind * 0.08;
            for (int i = 0; i < drops.length; i++) {
                double x = (drops[i] * Field.WIDTH * 1.3 + time * slant * 20) % (Field.WIDTH * 1.3) - Field.WIDTH * 0.15;
                double y = (drops[(i * 7) % drops.length] * Field.HEIGHT + time * 380) % Field.HEIGHT;
                g.draw(new Line2D.Double(x, y, x + slant, y + 7));
            }
            g.setStroke(new BasicStroke(1));
            double flash = (time % 7.3);
            if (flash < 0.12) {
                g.setColor(new Color(255, 255, 255, (int) (90 * (1 - flash / 0.12))));
                g.fill(new Rectangle2D.Double(0, 0, Field.WIDTH, Field.HEIGHT));
            }
        } else if (shift.theme().equals("snow")) {
            g.setColor(new Color(255, 255, 255, 200));
            for (int i = 0; i < 120; i++) {
                double x = (drops[i] * Field.WIDTH + time * (wind * 0.4) + Math.sin(time + i) * 6) % Field.WIDTH;
                if (x < 0) x += Field.WIDTH;
                double y = (drops[(i * 7) % drops.length] * Field.HEIGHT + time * 30) % Field.HEIGHT;
                g.fill(new Rectangle2D.Double(x, y, 1.5, 1.5));
            }
        } else if (Math.abs(wind) > 1) {
            g.setColor(new Color(255, 255, 255, (int) Math.min(80, Math.abs(wind) * 1.2)));
            for (int i = 0; i < 24; i++) {
                double x = (drops[i] * Field.WIDTH * 1.5 + time * wind * 6) % (Field.WIDTH * 1.5);
                if (x < 0) x += Field.WIDTH * 1.5;
                x -= Field.WIDTH * 0.25;
                double y = Field.CEILING + drops[i + 40] * (Field.HEIGHT - Field.CEILING);
                g.fill(new Rectangle2D.Double(x, y, Math.min(40, Math.abs(wind) * 0.6), 0.8));
            }
        }
    }
    // --8<-- [end:weather]
}
