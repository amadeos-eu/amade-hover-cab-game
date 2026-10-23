package eu.amadeos.games.hovercab.domain;

/** How a game ended: the money earned in cents, the shift the last cab was lost in, and whether every shift was done. */
public record GameResult(long cents, int shift, boolean finished) {
}
