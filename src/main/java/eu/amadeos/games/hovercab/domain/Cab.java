package eu.amadeos.games.hovercab.domain;

import eu.amadeos.gameengine.physics.Body;

/**
 * The cab: a {@link Body} that flies, its gear, its fuel, the pad it stands on (none in the air) and, for
 * the drawing, which way it faces and which jets fired in the last step.
 */
public final class Cab {
    final Body body = new Body(0, 0, Field.CAB_WIDTH, Field.CAB_HEIGHT);
    /** How far the gear is out, 0 to 1, and where it is going. */
    double gear = 1;
    boolean gearWanted = true;
    double fuel = Field.TANK;
    Pad pad;
    boolean facingLeft;
    int jetSide;
    boolean jetUp;
    boolean jetDown;
    boolean refuelling;

    /** Stands the cab on a pad at {@code time}, gear out, the tank as it is. */
    void standOn(Pad pad, double time) {
        body.place(pad.centerX() - Field.CAB_WIDTH / 2, pad.top(time) - Field.CAB_HEIGHT);
        this.pad = pad;
        gear = 1;
        gearWanted = true;
        jetSide = 0;
        jetUp = jetDown = refuelling = false;
    }

    public Body body() {
        return body;
    }

    /** How far the gear is out, 0 (in) to 1 (out). */
    public double gear() {
        return gear;
    }

    public boolean gearOut() {
        return gear >= 1;
    }

    public double fuel() {
        return fuel;
    }

    /** The pad the cab stands on; null while it flies. */
    public Pad pad() {
        return pad;
    }

    public boolean landed() {
        return pad != null;
    }

    public boolean facingLeft() {
        return facingLeft;
    }

    /** The side jet that fired in the last step: -1 the one pushing left, 1 right, 0 none. */
    public int jetSide() {
        return jetSide;
    }

    public boolean jetUp() {
        return jetUp;
    }

    public boolean jetDown() {
        return jetDown;
    }

    public boolean refuelling() {
        return refuelling;
    }

    /** The middle of the cab's door, where passengers get in and out. */
    public double doorX() {
        return body.centerX() + (facingLeft ? 3 : -3);
    }
}
