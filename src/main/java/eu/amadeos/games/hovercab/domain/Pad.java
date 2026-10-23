package eu.amadeos.games.hovercab.domain;

/**
 * A landing pad: its number on the sign (0 for the fuel pad, which has no number), where its deck lies and
 * how wide it is. A lift is a pad that moves up and down between {@code y} and {@code y2} and back, once in
 * {@code seconds}, slowing at both ends.
 */
public record Pad(int number, double x, double y, double width, boolean fuel, double y2, double seconds) {
    /** A pad that stays where it is. */
    public static Pad fixed(int number, double x, double y, double width) {
        return new Pad(number, x, y, width, false, y, 0);
    }

    public static Pad fuelPad(double x, double y, double width) {
        return new Pad(0, x, y, width, true, y, 0);
    }

    public boolean isLift() {
        return seconds > 0;
    }

    // --8<-- [start:lift]
    /** The top of the deck at {@code time} seconds into the shift. */
    public double top(double time) {
        if (!isLift()) return y;
        double along = 0.5 - 0.5 * Math.cos(2 * Math.PI * time / seconds);
        return y + (y2 - y) * along;
    }

    /** How fast the deck moves at {@code time}, units per second, down positive. */
    public double speed(double time) {
        if (!isLift()) return 0;
        return (y2 - y) * Math.PI / seconds * Math.sin(2 * Math.PI * time / seconds);
    }
    // --8<-- [end:lift]

    public double right() {
        return x + width;
    }

    public double centerX() {
        return x + width / 2;
    }

    /** The highest the deck ever is. */
    public double highest() {
        return Math.min(y, y2);
    }

    public double lowest() {
        return Math.max(y, y2);
    }
}
