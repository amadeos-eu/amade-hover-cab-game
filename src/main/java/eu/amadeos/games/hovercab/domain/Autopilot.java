package eu.amadeos.games.hovercab.domain;

import eu.amadeos.gameengine.collision.PixelMask;
import eu.amadeos.gameengine.path.DistanceMap;
import eu.amadeos.gameengine.physics.Body;
import eu.amadeos.gameengine.tile.Cell;
import eu.amadeos.gameengine.tile.Direction;
import eu.amadeos.gameengine.tile.TileMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A pilot made of rules: it flies the cab on the title screen, and in the tests it proves that every shift
 * can be flown, with the same physics as the player, through the passengers to the gate.
 *
 * <p>It plans on a grid of cab positions four units apart. A position is open if the cab, three units
 * fatter all round, touches nothing there; a {@link DistanceMap} from the hover point over the pad it wants
 * gives, from any open position, the next step on a shortest way. It looks a few steps ahead along that way,
 * wants a speed towards that point, and fires each jet in the step when that brings the cab's speed closer
 * to the wanted one than not firing: on and off a hundred and twenty times a second, the jets average out
 * to just the push it needs. Over the pad it sinks slowly onto the deck.
 */
public final class Autopilot {
    static final int CELL = 4;
    /** How much fatter than the cab a position must be free. */
    static final int MARGIN = 3;
    /** The hover point lies so far above a deck, and the cab sinks onto it at this speed. */
    static final double HOVER = 12;
    static final double SINK = 20;
    static final double CRUISE = 110;
    /** It sinks no faster than this, so that it can still stop. */
    static final double FALL = 70;
    /** The braking it plans with: less than the jets can do, to have some to spare. */
    static final double BRAKE = 55;
    static final double LOOK_AHEAD = 4;
    /** How fast it takes a bend, and how far ahead it looks for one, in steps. */
    static final double TURN = 30;
    static final int BEND_LOOK = 40;
    /** Below this much fuel it goes to fill up first, if the shift has a fuel pad. */
    static final double REFUEL_BELOW = 50;

    private final Shift shift;
    private final int cols;
    private final int rows;
    private final boolean[] open;
    private final boolean[] gap;
    private final TileMap<Boolean> grid;
    private final Map<String, DistanceMap> maps = new HashMap<>();
    private final double brake;
    private boolean refuelling;

    public Autopilot(Shift shift) {
        this.shift = shift;
        // against a strong wind the side jets have less left to brake with
        brake = Math.min(BRAKE, (Field.SIDE_JET - shift.strongestWind()) * 0.8);
        cols = (int) ((Field.WIDTH - Field.CAB_WIDTH) / CELL) + 1;
        rows = (int) ((Field.HEIGHT - Field.CAB_HEIGHT) / CELL) + 1;
        // two more columns and rows of nothing all round: a distance map's steps over an edge come back on the far side
        grid = new TileMap<>(cols + 2, rows + 2, Boolean.FALSE);
        open = new boolean[cols * rows];
        gap = new boolean[cols * rows];
        PixelMask fat = fat();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int x = c * CELL, y = r * CELL;
                open[r * cols + c] = isOpen(fat, x, y);
                gap[r * cols + c] = y < Field.CEILING + MARGIN && x + Field.CAB_WIDTH > shift.gateFrom()
                        && x < shift.gateTo();
            }
        }
    }

    /** The cab's outline facing either way, gear out, grown by the margin. */
    private static PixelMask fat() {
        int w = (int) Field.CAB_WIDTH + 2 * MARGIN, h = (int) Field.CAB_HEIGHT + 2 * MARGIN;
        PixelMask fat = new PixelMask(w, h);
        for (boolean left : new boolean[]{false, true}) {
            PixelMask cab = CabShape.mask(left, true);
            for (int y = 0; y < cab.height(); y++) {
                for (int x = 0; x < cab.width(); x++) {
                    if (!cab.solid(x, y)) continue;
                    for (int dy = 0; dy <= 2 * MARGIN; dy++) for (int dx = 0; dx <= 2 * MARGIN; dx++) fat.set(x + dx, y + dy);
                }
            }
        }
        return fat;
    }

    private boolean isOpen(PixelMask fat, int x, int y) {
        if (shift.mask().overlaps(fat, x - MARGIN, y - MARGIN)) return false;
        if (shift.hasSea() && y + Field.CAB_HEIGHT > shift.sea() - 4) return false;
        for (Mover m : shift.movers()) {
            double left = Math.min(m.x(), m.x() + m.dx()), top = Math.min(m.y(), m.y() + m.dy());
            double right = Math.max(m.x(), m.x() + m.dx()) + m.width(), bottom = Math.max(m.y(), m.y() + m.dy()) + m.height();
            if (boxes(x, y, left, top, right, bottom)) return false;
        }
        for (Pad pad : shift.pads()) {
            if (pad.isLift() && boxes(x, y, pad.x(), pad.highest(), pad.right(), pad.lowest() + Field.DECK)) return false;
        }
        return true;
    }

    private static boolean boxes(double x, double y, double left, double top, double right, double bottom) {
        return x - MARGIN < right && left < x + Field.CAB_WIDTH + MARGIN
                && y - MARGIN < bottom && top < y + Field.CAB_HEIGHT + MARGIN;
    }

    // --8<-- [start:steer]
    /** What the autopilot does in this step of the game. */
    public Input steer(Game game) {
        if (game.phase() != Game.Phase.PLAYING) return Input.NONE;
        Cab cab = game.cab();
        if (!cab.gearWanted && !cab.landed()) return new Input(0, false, false, true);   // gear out, always
        Target target = target(game);
        if (cab.landed()) {
            if (target == null || target.pad() == cab.pad()) return Input.NONE;     // wait for the passenger
            return new Input(0, true, false, false);                                 // lift off
        }
        if (target == null) target = new Target(null, hoverCell(game.shift().startPad()));
        return fly(game, target);
    }
    // --8<-- [end:steer]

    /** Where to: the pad to land on, or the gate (pad null); null to stay. */
    private record Target(Pad pad, Cell cell) {
    }

    private Target target(Game game) {
        Cab cab = game.cab();
        Rider rider = game.rider();
        Pad fuel = shift.pads().stream().filter(Pad::fuel).findFirst().orElse(null);
        if (fuel != null) {
            if (cab.fuel() < REFUEL_BELOW && (rider == null || rider.state() != Rider.State.BOARDING)) refuelling = true;
            if (refuelling && cab.landed() && cab.pad() == fuel && cab.fuel() >= Field.TANK - 1) refuelling = false;
            if (refuelling) return new Target(fuel, hoverCell(fuel));
        }
        if (rider == null) return null;
        Pad pad = switch (rider.state()) {
            case WAITING, BOARDING -> rider.pad();
            case RIDING -> rider.to() == Shift.UP ? null : shift.pad(rider.to()).orElseThrow();
            default -> cab.pad();
        };
        if (rider.state() == Rider.State.RIDING && rider.to() == Shift.UP) {
            return game.gateOpen() ? new Target(null, gateCell()) : null;
        }
        if (pad == null) return null;
        return new Target(pad, hoverCell(pad));
    }

    // --8<-- [start:fly]
    private Input fly(Game game, Target target) {
        Body body = game.cab().body();
        Pad pad = target.pad();
        double wantX, wantVx, wantVy;
        double hoverX = target.cell().col() * CELL, hoverY = target.cell().row() * CELL;
        double deck = pad == null ? 0 : pad.top(game.time()) - Field.CAB_HEIGHT;
        boolean over = pad != null && Math.abs(body.x - hoverX) < 3 && body.y > hoverY - 3 && body.y < deck + 2;
        if (over && body.x + Field.LEG_LEFT >= pad.x() && body.x + Field.LEG_RIGHT <= pad.right()) {
            // over the pad: hold still sideways and sink onto the deck
            wantVx = (hoverX - body.x) * 3;
            wantVy = pad.speed(game.time()) + Math.min(SINK, Math.max(8, (deck - body.y) * 2));
            return jets(game, wantVx, wantVy);
        }
        if (pad == null && body.y < Field.CEILING + 2 * CELL && Math.abs(body.x - hoverX) < 6 && game.gateOpen()) {
            return jets(game, (hoverX - body.x) * 3, -CRUISE);                 // out through the gate
        }
        Cell from = nearestOpen(body.x, body.y, game.gateOpen());
        DistanceMap map = map(target.cell(), game.gateOpen());
        Cell to = from;
        for (int i = 0; i < LOOK_AHEAD && from != null; i++) {
            int distance = map.distance(to.col() + 1, to.row() + 1);
            if (distance == 0 || distance == DistanceMap.UNREACHABLE) break;
            Direction d = map.downhill(to.col() + 1, to.row() + 1);
            if (d == null) break;
            to = new Cell(to.col() + d.dx, to.row() + d.dy);
        }
        wantX = to == null ? body.x : to.col() * CELL;
        double wantY = to == null ? body.y - CELL : to.row() * CELL;
        double dx = wantX - body.x, dy = wantY - body.y;
        double length = Math.max(1e-6, Math.hypot(dx, dy));
        // as fast as the cab can still stop before the hover point, along the way there
        int steps = from == null ? 0 : map.distance(from.col() + 1, from.row() + 1);
        double left = steps == DistanceMap.UNREACHABLE ? length : steps * CELL;
        // slower where the wind pulls, since the side jets have less to spare against it
        double cruise = CRUISE - Math.abs(shift.wind(game.time())) * 0.8;
        double speed = Math.min(cruise, Math.min(Math.sqrt(2 * brake * left), length * 6));
        // and slow enough to take the next bend in the way
        int bend = bend(map, from);
        if (bend >= 0) speed = Math.min(speed, TURN + Math.sqrt(2 * brake * bend * CELL));
        wantVx = dx / length * speed;
        wantVy = Math.min(FALL, dy / length * speed);
        return jets(game, wantVx, wantVy);
    }

    /**
     * How many steps along the way from {@code from} the first bend is, where the heading over the next few
     * steps turns by more than sixty degrees; -1 if there is none ahead. A heading over a few steps, because
     * the steps themselves only go up, down, left or right, and a slant is a staircase of them.
     */
    private static int bend(DistanceMap map, Cell from) {
        if (from == null) return -1;
        List<Cell> way = new ArrayList<>();
        Cell at = from;
        way.add(at);
        while (way.size() < BEND_LOOK) {
            int distance = map.distance(at.col() + 1, at.row() + 1);
            if (distance == 0 || distance == DistanceMap.UNREACHABLE) break;
            Direction d = map.downhill(at.col() + 1, at.row() + 1);
            if (d == null) break;
            at = new Cell(at.col() + d.dx, at.row() + d.dy);
            way.add(at);
        }
        int w = 4;
        if (way.size() <= w) return -1;
        double hx = way.get(w).col() - from.col(), hy = way.get(w).row() - from.row();
        for (int i = 1; i + w < way.size(); i++) {
            double x = way.get(i + w).col() - way.get(i).col(), y = way.get(i + w).row() - way.get(i).row();
            if (hx * x + hy * y < 0.5 * Math.hypot(hx, hy) * Math.hypot(x, y)) return i;
        }
        return -1;
    }

    /**
     * Fires each jet if the speed it gives after this step is nearer the wanted one than without it: the
     * main jet against the roof jet against neither, the left against the right against neither.
     */
    private Input jets(Game game, double wantVx, double wantVy) {
        Body body = game.cab().body();
        double step = 1.0 / 120;
        double gravity = shift.gravity();
        double needY = (wantVy - body.vy) / step;
        double needX = (wantVx - body.vx) / step - shift.wind(game.time());
        double none = Math.abs(needY - gravity);
        double up = Math.abs(needY - (gravity - Field.MAIN_JET));
        double down = Math.abs(needY - (gravity + Field.DOWN_JET));
        boolean fireUp = up < none && up <= down;
        boolean fireDown = !fireUp && down < none;
        int side = 0;
        if (Math.abs(needX - Field.SIDE_JET) < Math.abs(needX)) side = 1;
        else if (Math.abs(needX + Field.SIDE_JET) < Math.abs(needX)) side = -1;
        return new Input(side, fireUp, fireDown, false);
    }
    // --8<-- [end:fly]

    /** The way the autopilot plans from where the cab is, cell by cell to its goal; for the handbook's picture. */
    List<Cell> way(Game game) {
        List<Cell> way = new ArrayList<>();
        Target target = target(game);
        if (target == null) return way;
        Cell at = nearestOpen(game.cab().body().x, game.cab().body().y, game.gateOpen());
        DistanceMap map = map(target.cell(), game.gateOpen());
        while (at != null && way.size() < cols * rows) {
            way.add(at);
            int distance = map.distance(at.col() + 1, at.row() + 1);
            if (distance == 0 || distance == DistanceMap.UNREACHABLE) break;
            Direction d = map.downhill(at.col() + 1, at.row() + 1);
            if (d == null) break;
            at = new Cell(at.col() + d.dx, at.row() + d.dy);
        }
        return way;
    }

    int cols() {
        return cols;
    }

    int rows() {
        return rows;
    }

    // ---- the grid ------------------------------------------------------------------------------------

    /** The cell over a pad from which the cab sinks onto it: above its highest deck, or the nearest open one above that. */
    Cell hoverCell(Pad pad) {
        int c = (int) Math.round((pad.centerX() - Field.CAB_WIDTH / 2) / CELL);
        int r = (int) Math.floor((pad.highest() - Field.CAB_HEIGHT - HOVER) / CELL);
        for (int up = 0; up < rows; up++) {
            for (int side : new int[]{0, -1, 1, -2, 2}) {
                if (open(c + side, r - up, false)) return new Cell(c + side, r - up);
            }
        }
        throw new IllegalStateException("no room over pad " + pad.number() + " of shift " + shift.number());
    }

    /** The cell in the middle of the gate, as high as the grid goes. */
    Cell gateCell() {
        int c = (int) Math.round(((shift.gateFrom() + shift.gateTo()) / 2 - Field.CAB_WIDTH / 2) / CELL);
        return new Cell(c, 0);
    }

    private DistanceMap map(Cell goal, boolean gateOpen) {
        String key = goal.col() + "," + goal.row() + "," + gateOpen;
        return maps.computeIfAbsent(key, k -> DistanceMap.search(grid,
                (col, row) -> open(col - 1, row - 1, gateOpen), new Cell(goal.col() + 1, goal.row() + 1)));
    }

    boolean open(int col, int row, boolean gateOpen) {
        if (col < 0 || row < 0 || col >= cols || row >= rows) return false;
        int i = row * cols + col;
        return open[i] && (gateOpen || !gap[i]);
    }

    /** The open cell nearest to a position: its own, or one around it, nearest first. */
    private Cell nearestOpen(double x, double y, boolean gateOpen) {
        int c = (int) Math.round(x / CELL), r = (int) Math.round(y / CELL);
        if (open(c, r, gateOpen)) return new Cell(c, r);
        List<Cell> ring = new ArrayList<>();
        for (int d = 1; d <= 6 && ring.isEmpty(); d++) {
            for (int dr = -d; dr <= d; dr++) {
                for (int dc = -d; dc <= d; dc++) {
                    if (Math.max(Math.abs(dr), Math.abs(dc)) == d && open(c + dc, r + dr, gateOpen)) ring.add(new Cell(c + dc, r + dr));
                }
            }
        }
        ring.sort((a, b) -> Double.compare(Math.hypot(a.col() - c, a.row() - r + 0.5), Math.hypot(b.col() - c, b.row() - r + 0.5)));
        return ring.isEmpty() ? null : ring.getFirst();
    }
}
