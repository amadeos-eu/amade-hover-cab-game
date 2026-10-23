package eu.amadeos.games.hovercab.domain;

/**
 * A passenger: who they are (which look and voice, by their place in the shift's list), where they wait and
 * where they want to go, and what they are doing. Only one passenger is about at a time.
 */
public final class Rider {
    /** Not yet there; waiting on the pad; walking to the cab; aboard; walking off; gone. */
    public enum State { COMING, WAITING, BOARDING, RIDING, LEAVING, GONE }

    private final int index;
    private final int from;
    private final int to;
    State state = State.COMING;
    /** Where the feet are, on the deck of the pad they stand on; the spot they wait at. */
    double x;
    double spot;
    Pad pad;
    /** The tip still to be had, in cents, and seconds since they last called. */
    double tip;
    double sinceCall;
    /** Seconds in the current state, for the drawing: waving, walking. */
    double stateTime;
    boolean facingLeft;

    Rider(int index, Shift.Fare fare) {
        this.index = index;
        this.from = fare.from();
        this.to = fare.to();
    }

    void become(State state) {
        this.state = state;
        stateTime = 0;
    }

    /** Their place in the shift's list, from 0: it chooses how they look and sound. */
    public int index() {
        return index;
    }

    public int from() {
        return from;
    }

    /** The pad they want to go to; {@link Shift#UP} for the exit in the ceiling. */
    public int to() {
        return to;
    }

    public State state() {
        return state;
    }

    public double x() {
        return x;
    }

    /** The pad they stand on, null while they ride. */
    public Pad pad() {
        return pad;
    }

    public int tip() {
        return (int) Math.round(tip);
    }

    public double stateTime() {
        return stateTime;
    }

    public boolean facingLeft() {
        return facingLeft;
    }

    /** Seen on a pad: waiting, or walking to or from the cab. */
    public boolean visible() {
        return state == State.WAITING || state == State.BOARDING || state == State.LEAVING;
    }
}
