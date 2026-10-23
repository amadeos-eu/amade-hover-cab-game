package eu.amadeos.games.hovercab.domain;

import java.util.Locale;

/** Flies shifts with the autopilot and tells how it went: for designing shifts. java ... Flights 1 3 */
public final class Flights {
    public static void main(String[] args) {
        for (String arg : args) {
            int number = Integer.parseInt(arg);
            Shift shift = Shift.load(number);
            Game game = new Game(shift, new Career());
            Autopilot pilot = new Autopilot(shift);
            double minFuel = Field.TANK;
            int steps = 0;
            while (game.phase() == Game.Phase.PLAYING || game.phase() == Game.Phase.CRASHED) {
                game.update(1 / 120.0, pilot.steer(game));
                steps++;
                minFuel = Math.min(minFuel, game.cab().fuel());
                for (Event e : game.events()) {
                    if (e instanceof Event.Crashed c) System.out.printf(Locale.ROOT, "  %6.1fs crash %s at %.0f,%.0f%n", game.time(), c.cause(), c.x(), c.y());
                    if (e instanceof Event.Paid p) System.out.printf(Locale.ROOT, "  %6.1fs paid %d + %d%n", game.time(), p.fare(), p.tip());
                    if (e instanceof Event.Landed l) System.out.printf(Locale.ROOT, "  %6.1fs landed %s on %d%n", game.time(), l.touchdown(), l.pad().number());
                }
                if (System.getenv("TRACE") != null && steps % 12 == 0 && game.time() < Double.parseDouble(System.getenv("TRACE")))
                    System.out.printf(Locale.ROOT, "    %5.1f x %.0f y %.0f vx %.0f vy %.0f wind %.0f fuel %.0f %s%n", game.time(), game.cab().body().x, game.cab().body().y,
                            game.cab().body().vx, game.cab().body().vy, shift.wind(game.time()), game.cab().fuel(), game.rider() == null ? "-" : game.rider().state() + " " + game.rider().from() + ">" + game.rider().to());
                if (System.getenv("LIFT") != null && Math.abs(game.time() - Double.parseDouble(System.getenv("LIFT"))) < 0.15)
                    for (Pad p : shift.pads()) if (p.isLift()) System.out.printf(Locale.ROOT, "      t %.3f lift %d top %.1f speed %.1f cab x %.1f bottom %.1f vy %.1f gear %.2f%n", game.time(), p.number(),
                            p.top(game.time()), p.speed(game.time()), game.cab().body().x, game.cab().body().bottom(), game.cab().body().vy, game.cab().gear());
                if (steps > 120 * 600) break;
            }
            System.out.printf(Locale.ROOT, "shift %d: %s after %.1f s, fares %d tips %d, min fuel %.0f%n", number, game.phase(),
                    game.time(), game.fares(), game.tips(), minFuel);
        }
    }
}
