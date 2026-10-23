package eu.amadeos.games.hovercab.domain;

/**
 * A solid block that moves to and fro: a piston, a sliding door, a crane's load. It goes from where it
 * stands by {@code dx}, {@code dy} and back once in {@code seconds}, slowing at both ends, and starts
 * {@code delay} seconds into its way.
 */
public record Mover(String look, double x, double y, double width, double height, double dx, double dy,
                    double seconds, double delay) {
    public double x(double time) {
        return x + dx * along(time);
    }

    public double y(double time) {
        return y + dy * along(time);
    }

    private double along(double time) {
        return 0.5 - 0.5 * Math.cos(2 * Math.PI * (time + delay) / seconds);
    }
}
