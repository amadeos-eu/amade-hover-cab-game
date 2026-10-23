package eu.amadeos.games.hovercab.domain;

/** How a shift went: its number, the fares and tips earned in it in cents, the passengers carried and the cabs lost. */
public record ShiftResult(int shift, int fares, int tips, int passengers, int crashes, double seconds) {
}
