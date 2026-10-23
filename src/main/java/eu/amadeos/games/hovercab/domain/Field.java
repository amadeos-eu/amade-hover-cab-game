package eu.amadeos.games.hovercab.domain;

/**
 * The numbers of the game: sizes in logical units (the screen is 640 × 400), speeds in units per second,
 * accelerations in units per second per second, money in cents, times in seconds.
 */
public final class Field {
    public static final double WIDTH = 640;
    public static final double HEIGHT = 400;
    /** The top bar; below it the girders of the ceiling, with the exit gate. */
    public static final double TOP = 22;
    public static final double CEILING = 36;

    // --8<-- [start:physics]
    /** Gravity of a normal shift; a shift may weaken it, out in space. */
    public static final double GRAVITY = 110;
    public static final double MAX_FALL = 260;
    /** The jets: the one under the cab lifts it at 0.9 g, the side jets steer at about 1 g, the top jet presses down. */
    public static final double MAIN_JET = 1.9 * GRAVITY;
    public static final double SIDE_JET = 0.95 * GRAVITY;
    public static final double DOWN_JET = 0.6 * GRAVITY;
    /** Fuel the main jet burns in a second: a full tank lasts half a minute of full lift, a minute of hovering. */
    public static final double BURN = 3.2;
    public static final double TANK = 100;
    /** The air slows the cab by this part of its speed a second; the gear out adds to it. */
    public static final double AIR = 0.2;
    public static final double GEAR_AIR = 0.45;
    public static final double GEAR_SECONDS = 0.35;
    /** A touchdown up to this speed is soft, up to the next hard (the passenger complains), beyond it a crash. */
    public static final double SOFT_LANDING = 40;
    public static final double HARD_LANDING = 80;
    // --8<-- [end:physics]

    /** The cab: its box, the hull above the gear, and where the two legs stand. */
    public static final double CAB_WIDTH = 32;
    public static final double CAB_HEIGHT = 16;
    public static final double HULL_HEIGHT = 13;
    public static final double LEG_LEFT = 6;
    public static final double LEG_RIGHT = 26;
    /** A pad's deck is solid this deep under its top. */
    public static final double DECK = 5;

    // --8<-- [start:money]
    /** The tip is five dollars when a passenger gets in and melts by 20 cents a second of the ride. */
    public static final int TIP_START = 500;
    public static final int TIP_DROP = 20;
    /** A hard landing with a passenger aboard costs a dollar of the tip. */
    public static final int HARD_PENALTY = 100;
    /** The fare: two dollars, and one more for every 100 units between the pads. */
    public static final int FARE_BASE = 200;
    public static final int FARE_PER_100 = 100;
    /** A spare cab for every hundred dollars earned. */
    public static final int BONUS_CAB_EVERY = 10_000;
    public static final int SPARE_CABS = 3;
    /** Refuelling: units a second, and cents a unit. */
    public static final double REFUEL_RATE = 25;
    public static final double FUEL_PRICE = 3;
    // --8<-- [end:money]

    /** Passengers walk to and from the cab at this speed; they are this wide. */
    public static final double WALK = 32;
    public static final double RIDER_WIDTH = 6;
    /** The first passenger appears this long after the start, each next one this long after the last is gone. */
    public static final double FIRST_RIDER = 1.0;
    public static final double NEXT_RIDER = 1.5;
    /** A waiting passenger calls again after this long. */
    public static final double CALL_AGAIN = 7;
    /** After a crash the wreck burns this long before the next cab comes. */
    public static final double CRASH_SECONDS = 2.2;
    /** Below this much fuel the gauge blinks and beeps. */
    public static final double LOW_FUEL = 20;

    private Field() {
    }
}
