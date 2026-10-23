package eu.amadeos.games.hovercab.domain;

import eu.amadeos.gameengine.physics.Touchdown;

/** Something that happened in a step; the user interface decides what it looks and sounds like. */
// --8<-- [start:event]
public sealed interface Event {
    record GearMoved(boolean down) implements Event {
    }

    /** The cab came down on a pad; a HARD touchdown shakes it. */
    record Landed(double x, double y, Touchdown touchdown, Pad pad) implements Event {
    }

    record TookOff(double x, double y) implements Event {
    }

    /** A passenger speaks; {@code pad} is the pad named, for {@link Call#PAD}. */
    record Spoke(Rider rider, Call call, int pad) implements Event {
    }

    record Boarded(Rider rider) implements Event {
    }

    /** A passenger got out and paid, in cents. */
    record Paid(Rider rider, int fare, int tip, double x, double y) implements Event {
    }

    /** All fares of the shift are paid but the last one's, who wants up: the gate in the ceiling opens. */
    record GateOpened() implements Event {
    }

    /** The tank ran low: the gauge starts to blink. */
    record FuelLow() implements Event {
    }

    record BonusCab(int spare) implements Event {
    }

    record Crashed(double x, double y, Cause cause) implements Event {
    }

    /** A new cab on the start pad, after a crash. */
    record Respawned(double x, double y) implements Event {
    }

    record Cleared(ShiftResult result) implements Event {
    }

    record GameOver(GameResult result) implements Event {
    }

    /**
     * What the cab crashed into: a wall, the water, a pad too fast, a pad with the gear in, the closed gate;
     * or it stood with an empty tank away from fuel and was towed away.
     */
    enum Cause { HIT, WATER, TOO_FAST, NO_GEAR, GATE, EMPTY }
}
// --8<-- [end:event]
