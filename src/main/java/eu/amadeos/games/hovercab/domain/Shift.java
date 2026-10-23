package eu.amadeos.games.hovercab.domain;

import eu.amadeos.gameengine.collision.PixelMask;
import eu.amadeos.gameengine.data.TextData;

import java.awt.BasicStroke;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * One screen of the game, read from a text file in {@code shifts/}: its scenery, its pads, how strong
 * gravity and the wind are, and the passengers who will hail the cab, in order. The file is the level
 * designer's: one thing per line, a keyword and numbers, in screen units (the screen is 640 × 400).
 *
 * <pre>
 * name Harbour Heights
 * theme dusk                    # the sky and what lies far away: day dusk night cave canyon snow space factory storm
 * gravity 1                     # a factor on normal gravity
 * air 0.2                       # how much the air slows the cab, a part of its speed a second
 * wind 0 30 5                   # a steady push, gusts up to this much on top, a gust every so many seconds
 * sea 362                       # water below this line
 * gate 276 364                  # the exit in the ceiling, from x to x
 * start 2                       # the pad the cab stands on at the start and after a crash
 * block brick 0 150 116 212     # a solid box of a look: x y width height
 * poly rock 0 200 40 180 60 260 # a solid polygon: its corners
 * beam 452 244 508 276          # a solid strut from a point to a point
 * sign pink 158 84 76 26 NOODLES  # a neon sign hanging from the ceiling on chains
 * deco tank 0 108               # something drawn that is not solid
 * pad 1 14 146 92               # a pad: its number, x, the top of its deck, its width
 * fuel 334 270 70               # the fuel pad
 * lift 6 300 200 60 320 8       # a pad that moves: number x y width, down to y2, there and back in so many seconds
 * mover steel 200 100 20 40 0 80 4 0   # a moving block (or a van, a drone): look x y w h, by dx dy, in seconds, starting late by
 * riders 2>4 1>3 5>2 4>up       # the passengers in order, from pad to pad; the last one wants up
 * </pre>
 */
public final class Shift {
    public static final int COUNT = 12;
    public static final Set<String> THEMES = Set.of("day", "dusk", "night", "cave", "canyon", "snow", "space",
            "factory", "storm");
    public static final Set<String> LOOKS = Set.of("brick", "glass", "concrete", "steel", "rock", "ice", "hull",
            "wood", "sand", "plain");
    /** What a moving block may also look like: a flying van, a drone. */
    public static final Set<String> VEHICLES = Set.of("van", "drone");
    private static final Set<String> MOVER_LOOKS = union(LOOKS, VEHICLES);
    public static final Set<String> DECOS = Set.of("tank", "antenna", "tree", "pine", "lamp", "crane", "chimney",
            "dish", "pipe", "cactus", "crystal", "flag");
    /** The pad numbers a passenger can name; {@code UP} is the exit. */
    public static final int UP = 0;

    /** Something solid in the scenery. */
    public record Solid(Kind kind, String look, double[] numbers, String text) {
        public enum Kind { BLOCK, POLY, BEAM, SIGN }

        /** The outline that is solid. */
        public Shape shape() {
            double[] n = numbers;
            return switch (kind) {
                case BLOCK -> new Rectangle2D.Double(n[0], n[1], n[2], n[3]);
                case POLY -> {
                    Polygon polygon = new Polygon();
                    for (int i = 0; i + 1 < n.length; i += 2) polygon.addPoint((int) n[i], (int) n[i + 1]);
                    yield polygon;
                }
                case BEAM -> new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                        .createStrokedShape(new Line2D.Double(n[0], n[1], n[2], n[3]));
                case SIGN -> new RoundRectangle2D.Double(n[0], n[1], n[2], n[3], 6, 6);
            };
        }
    }

    public record Deco(String kind, double x, double y) {
    }

    /** A passenger: from which pad to which, {@link #UP} for the exit. */
    public record Fare(int from, int to) {
    }

    private final int number;
    private String name = "";
    private String theme = "day";
    private double gravity = 1;
    private double air = Field.AIR;
    private double wind, gust, gustSeconds = 5;
    private double sea = Double.NaN;
    private double gateFrom = 280, gateTo = 360;
    private int start = -1;
    private final List<Solid> solids = new ArrayList<>();
    private final List<Deco> decos = new ArrayList<>();
    private final List<Pad> pads = new ArrayList<>();
    private final List<Mover> movers = new ArrayList<>();
    private final List<Fare> fares = new ArrayList<>();
    private PixelMask mask;

    private Shift(int number) {
        this.number = number;
    }

    /** Shift {@code number}, 1 to {@link #COUNT}, from the game's files. */
    public static Shift load(int number) {
        return read(number, TextData.load(Shift.class, "/eu/amadeos/games/hovercab/shifts/shift" + number + ".txt"));
    }

    public static Shift parse(int number, String source, Reader reader) {
        return read(number, TextData.parse(source, reader));
    }

    // --8<-- [start:read]
    private static Shift read(int number, List<TextData.Line> lines) {
        Shift shift = new Shift(number);
        TextData.Line last = null;
        for (TextData.Line line : lines) {
            last = line;
            switch (line.keyword()) {
                case "name" -> shift.name = String.join(" ", line.words().subList(1, line.size()));
                case "theme" -> shift.theme = known(line, 1, THEMES);
                case "gravity" -> shift.gravity = line.number(1);
                case "air" -> shift.air = line.number(1);
                case "wind" -> {
                    shift.wind = line.number(1);
                    shift.gust = line.number(2);
                    shift.gustSeconds = line.number(3);
                }
                case "sea" -> shift.sea = line.number(1);
                case "gate" -> {
                    shift.gateFrom = line.number(1);
                    shift.gateTo = line.number(2);
                }
                case "start" -> shift.start = line.integer(1);
                case "block" -> shift.solids.add(new Solid(Solid.Kind.BLOCK, known(line, 1, LOOKS), numbers(line, 2, 4), null));
                case "poly" -> {
                    if (line.size() < 8 || line.size() % 2 != 0) throw line.error("a polygon needs a look and at least three corners");
                    shift.solids.add(new Solid(Solid.Kind.POLY, known(line, 1, LOOKS), numbers(line, 2, line.size() - 2), null));
                }
                case "beam" -> shift.solids.add(new Solid(Solid.Kind.BEAM, "steel", numbers(line, 1, 4), null));
                case "sign" -> shift.solids.add(new Solid(Solid.Kind.SIGN, line.word(1), numbers(line, 2, 4),
                        String.join(" ", line.words().subList(6, Math.max(6, line.size())))));
                case "deco" -> shift.decos.add(new Deco(known(line, 1, DECOS), line.number(2), line.number(3)));
                case "pad" -> shift.pads.add(Pad.fixed(padNumber(line), line.number(2), line.number(3), line.number(4)));
                case "fuel" -> shift.pads.add(Pad.fuelPad(line.number(1), line.number(2), line.number(3)));
                case "lift" -> shift.pads.add(new Pad(padNumber(line), line.number(2), line.number(3), line.number(4),
                        false, line.number(5), positive(line, 6)));
                case "mover" -> shift.movers.add(new Mover(known(line, 1, MOVER_LOOKS), line.number(2), line.number(3),
                        line.number(4), line.number(5), line.number(6), line.number(7), positive(line, 8),
                        line.size() > 9 ? line.number(9) : 0));
                case "riders" -> {
                    for (String word : line.words().subList(1, line.size())) shift.fares.add(fare(line, word));
                }
                default -> throw line.error("unknown keyword \"" + line.keyword() + '"');
            }
        }
        shift.check(last);
        return shift;
    }
    // --8<-- [end:read]

    private static String known(TextData.Line line, int index, Set<String> names) {
        String word = line.word(index);
        if (!names.contains(word)) throw line.error('"' + word + "\" is not one of " + new java.util.TreeSet<>(names));
        return word;
    }

    private static double[] numbers(TextData.Line line, int from, int count) {
        double[] numbers = new double[count];
        for (int i = 0; i < count; i++) numbers[i] = line.number(from + i);
        return numbers;
    }

    private static int padNumber(TextData.Line line) {
        int number = line.integer(1);
        if (number < 1 || number > 9) throw line.error("pads are numbered 1 to 9, not " + number);
        return number;
    }

    private static double positive(TextData.Line line, int index) {
        double value = line.number(index);
        if (value <= 0) throw line.error("the time must be more than 0");
        return value;
    }

    private static Fare fare(TextData.Line line, String word) {
        String[] parts = word.split(">");
        if (parts.length != 2) throw line.error('"' + word + "\" is not a fare like 2>4");
        try {
            int from = Integer.parseInt(parts[0]);
            int to = parts[1].equals("up") ? UP : Integer.parseInt(parts[1]);
            return new Fare(from, to);
        } catch (NumberFormatException e) {
            throw line.error('"' + word + "\" is not a fare like 2>4");
        }
    }

    /** The things a level designer can get wrong, each with the file's name. */
    private void check(TextData.Line last) {
        String where = last == null ? "shift" + number : last.source();
        Set<Integer> numbers = new HashSet<>();
        for (Pad pad : pads) {
            if (pad.number() > 0 && !numbers.add(pad.number())) throw new TextData.DataException(where + ": two pads " + pad.number());
        }
        if (pad(start).isEmpty()) throw new TextData.DataException(where + ": the start pad " + start + " is missing");
        if (fares.isEmpty()) throw new TextData.DataException(where + ": no riders");
        for (int i = 0; i < fares.size(); i++) {
            Fare fare = fares.get(i);
            boolean isLast = i == fares.size() - 1;
            if (pad(fare.from()).isEmpty()) throw new TextData.DataException(where + ": no pad " + fare.from() + " for a rider");
            if (fare.to() == UP != isLast) throw new TextData.DataException(where + ": the last rider, and only the last, wants up");
            if (fare.to() != UP && (pad(fare.to()).isEmpty() || fare.to() == fare.from())) {
                throw new TextData.DataException(where + ": a rider from " + fare.from() + " to " + fare.to() + " cannot go there");
            }
        }
        if (gateFrom < 0 || gateTo > Field.WIDTH || gateTo - gateFrom < Field.CAB_WIDTH + 16) {
            throw new TextData.DataException(where + ": the gate must be on the screen and wide enough for the cab");
        }
    }

    // --8<-- [start:mask]
    /**
     * Where the scenery is solid, pixel by pixel: the ceiling but for the gate, the blocks, polygons, beams
     * and signs, and the decks of the pads that do not move. Made once, the first time it is asked for.
     */
    public PixelMask mask() {
        if (mask == null) {
            mask = PixelMask.painted((int) Field.WIDTH, (int) Field.HEIGHT, g -> {
                g.fill(new Rectangle2D.Double(0, 0, gateFrom, Field.CEILING));
                g.fill(new Rectangle2D.Double(gateTo, 0, Field.WIDTH - gateTo, Field.CEILING));
                for (Solid solid : solids) g.fill(solid.shape());
                for (Pad pad : pads) {
                    if (!pad.isLift()) g.fill(new Rectangle2D.Double(pad.x(), pad.y(), pad.width(), Field.DECK));
                }
            });
        }
        return mask;
    }
    // --8<-- [end:mask]

    /** The wind's push at {@code time}: steady, plus gusts that come and go. */
    public double wind(double time) {
        if (gust == 0) return wind;
        double swell = Math.sin(2 * Math.PI * time / gustSeconds);
        double flutter = 0.7 + 0.3 * Math.sin(2 * Math.PI * time / (gustSeconds * 0.37));
        return wind + gust * Math.max(0, swell) * flutter;
    }

    public Optional<Pad> pad(int number) {
        for (Pad pad : pads) if (pad.number() == number && (number > 0 || pad.fuel())) return Optional.of(pad);
        return Optional.empty();
    }

    public int number() {
        return number;
    }

    public String name() {
        return name;
    }

    public String theme() {
        return theme;
    }

    public double gravity() {
        return gravity * Field.GRAVITY;
    }

    public double air() {
        return air;
    }

    /** The strongest the wind ever blows, either way. */
    public double strongestWind() {
        return Math.abs(wind) + Math.abs(gust);
    }

    public boolean windy() {
        return wind != 0 || gust != 0;
    }

    public boolean hasSea() {
        return !Double.isNaN(sea);
    }

    public double sea() {
        return sea;
    }

    public double gateFrom() {
        return gateFrom;
    }

    public double gateTo() {
        return gateTo;
    }

    public Pad startPad() {
        return pad(start).orElseThrow();
    }

    public List<Solid> solids() {
        return Collections.unmodifiableList(solids);
    }

    public List<Deco> decos() {
        return Collections.unmodifiableList(decos);
    }

    public List<Pad> pads() {
        return Collections.unmodifiableList(pads);
    }

    public List<Mover> movers() {
        return Collections.unmodifiableList(movers);
    }

    public List<Fare> fares() {
        return Collections.unmodifiableList(fares);
    }

    private static Set<String> union(Set<String> a, Set<String> b) {
        Set<String> all = new HashSet<>(a);
        all.addAll(b);
        return Set.copyOf(all);
    }
}
