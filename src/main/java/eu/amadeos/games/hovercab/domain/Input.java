package eu.amadeos.games.hovercab.domain;

/**
 * What the pilot wants in one step, whatever it came from: keys, a gamepad, the autopilot or a test.
 *
 * @param side -1 the right jet pushes left, 1 the left jet pushes right, 0 neither
 * @param up   the main jet under the cab fires
 * @param down the small jet on the roof fires
 * @param gear the gear button went down in this step: the gear goes out or in
 */
public record Input(int side, boolean up, boolean down, boolean gear) {
    public static final Input NONE = new Input(0, false, false, false);
}
