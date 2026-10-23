package eu.amadeos.games.hovercab.domain;

import eu.amadeos.gameengine.tile.Cell;

import java.util.List;

/** The autopilot's grid and plan, opened up for the handbook's pictures. */
public final class Plans {
    public static final int CELL = Autopilot.CELL;

    private Plans() {
    }

    public static int cols(Autopilot pilot) {
        return pilot.cols();
    }

    public static int rows(Autopilot pilot) {
        return pilot.rows();
    }

    public static boolean open(Autopilot pilot, int col, int row) {
        return pilot.open(col, row, false);
    }

    public static List<Cell> way(Autopilot pilot, Game game) {
        return pilot.way(game);
    }

    /** Puts the cab somewhere in the air, as the tests do. */
    public static void put(Game game, double x, double y, double vx, double vy) {
        game.put(x, y, vx, vy);
    }
}
