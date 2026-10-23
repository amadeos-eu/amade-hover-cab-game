package eu.amadeos.games.hovercab.domain;

import eu.amadeos.gameengine.collision.PixelMask;
import eu.amadeos.gameengine.physics.Body;
import eu.amadeos.gameengine.physics.Thrusters;
import eu.amadeos.gameengine.physics.Touchdown;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One shift being played. Plain Java without a clock: {@link #update} advances everything by a step, with
 * what the pilot wants in it, and the {@link #events()} tell what happened. No chance anywhere: the same
 * inputs always fly the same shift.
 *
 * <p>The cab flies against gravity, wind and the air; it may touch nothing but the deck of a pad, and that
 * only with the gear out, from above and slowly enough. The passengers come one after another, wait on
 * their pad, walk to the cab when it stands there, name where they want to go and pay when they get
 * there. The last one wants up: the gate in the ceiling opens, and flying out through it ends the shift.
 */
public final class Game {
    /** Flying; the wreck burning before the next cab; the shift done; no cabs left. */
    public enum Phase { PLAYING, CRASHED, CLEARED, OVER }

    /** How long a cab may stand with an empty tank, away from fuel, before it is towed away and counts as lost. */
    static final double STRANDED_SECONDS = 2.5;

    private final Shift shift;
    private final Career career;
    private final Cab cab = new Cab();
    private final Thrusters jets = new Thrusters(Field.MAIN_JET, Field.SIDE_JET, Field.DOWN_JET, Field.BURN);
    private final List<Rider> riders = new ArrayList<>();
    private final List<Event> events = new ArrayList<>();
    private int current;
    private double nextRider = Field.FIRST_RIDER;
    private boolean gateOpen;
    private Phase phase = Phase.PLAYING;
    private double phaseTime;
    private double time;
    private double stranded;
    private boolean fuelWarned;
    private int fares, tips, carried, crashes;
    private ShiftResult result;

    public Game(Shift shift, Career career) {
        this.shift = shift;
        this.career = career;
        for (int i = 0; i < shift.fares().size(); i++) riders.add(new Rider(i, shift.fares().get(i)));
        cab.standOn(shift.startPad(), 0);
    }

    // --8<-- [start:update]
    /** One step of {@code seconds} with what the pilot wants in it. */
    public void update(double seconds, Input input) {
        events.clear();
        time += seconds;
        phaseTime += seconds;
        switch (phase) {
            case PLAYING -> fly(seconds, input);
            case CRASHED -> wreck();
            case CLEARED, OVER -> cab.body.remember();
        }
        if (phase == Phase.PLAYING || phase == Phase.CRASHED) riders(seconds);
    }
    // --8<-- [end:update]

    // ---- the cab -------------------------------------------------------------------------------------

    private void fly(double seconds, Input input) {
        Body body = cab.body;
        body.remember();
        gear(seconds, input);
        cab.jetSide = 0;
        cab.jetUp = cab.jetDown = cab.refuelling = false;
        if (cab.landed() && !standing(seconds, input)) return;
        push(seconds, input);
        move(seconds);
    }

    /** The gear goes out or in over a third of a second; not while standing on it. */
    private void gear(double seconds, Input input) {
        if (input.gear() && !cab.landed()) {
            cab.gearWanted = !cab.gearWanted;
            events.add(new Event.GearMoved(cab.gearWanted));
        }
        double change = seconds / Field.GEAR_SECONDS;
        cab.gear = cab.gearWanted ? Math.min(1, cab.gear + change) : Math.max(0, cab.gear - change);
    }

    /**
     * On a pad: fuel flows on the fuel pad, the cab rides a lift up and down, and the main jet lifts it off.
     * False if the cab stays standing.
     */
    private boolean standing(double seconds, Input input) {
        Pad pad = cab.pad;
        Body body = cab.body;
        if (pad.fuel() && cab.fuel < Field.TANK) {
            double added = Math.min(Field.REFUEL_RATE * seconds, Field.TANK - cab.fuel);
            cab.fuel += added;
            career.pay(added * Field.FUEL_PRICE);
            cab.refuelling = true;
            if (cab.fuel > Field.LOW_FUEL) fuelWarned = false;
        }
        if (input.up() && cab.fuel > 0) {
            cab.pad = null;
            stranded = 0;
            events.add(new Event.TookOff(body.centerX(), body.bottom()));
            return true;
        }
        body.place(body.x, pad.top(time) - Field.CAB_HEIGHT);
        body.remember();
        if (cab.fuel <= 0 && !pad.fuel()) {
            stranded += seconds;
            if (stranded >= STRANDED_SECONDS) crash(Event.Cause.EMPTY);
        }
        if (phase == Phase.PLAYING && touchesMovers()) crash(Event.Cause.HIT);
        return false;
    }

    // --8<-- [start:push]
    /** The jets, the wind, gravity and the air: what changes the cab's speed in a step. */
    private void push(double seconds, Input input) {
        Body body = cab.body;
        int vertical = input.up() ? -1 : input.down() ? 1 : 0;
        double burned = jets.fire(body, input.side(), vertical, cab.fuel, seconds);
        cab.fuel = Math.max(0, cab.fuel - burned);
        if (burned > 0) {
            cab.jetSide = input.side();
            cab.jetUp = input.up();
            cab.jetDown = input.down() && !input.up();
        }
        if (input.side() != 0) cab.facingLeft = input.side() < 0;
        if (cab.fuel < Field.LOW_FUEL && !fuelWarned) {
            fuelWarned = true;
            events.add(new Event.FuelLow());
        }
        body.vx += shift.wind(time) * seconds;
        body.fall(shift.gravity(), Field.MAX_FALL, seconds);
        body.drag(shift.air() + Field.GEAR_AIR * cab.gear, seconds);
    }
    // --8<-- [end:push]

    // --8<-- [start:move]
    /**
     * Moves the cab by its speed and finds out what it met: the deck of a pad from above with the gear
     * out is a landing, the open gate is the way out, and everything else solid is a crash.
     */
    private void move(double seconds) {
        Body body = cab.body;
        double bottomBefore = body.bottom();
        body.x += body.vx * seconds;
        body.y += body.vy * seconds;
        if (body.x < 0 || body.right() > Field.WIDTH) {     // the edges of the screen stop the cab, gently
            body.x = Math.clamp(body.x, 0, Field.WIDTH - Field.CAB_WIDTH);
            body.vx = 0;
        }
        if (gateOpen && body.bottom() < Field.TOP) {
            clear();
            return;
        }
        if (cab.gearOut()) {
            for (Pad pad : shift.pads()) {
                double top = pad.top(time);
                double falling = body.vy - pad.speed(time);
                if (falling >= 0 && bottomBefore <= pad.top(time - seconds) + 0.5 && body.bottom() >= top - 1
                        && body.x + Field.LEG_LEFT >= pad.x() && body.x + Field.LEG_RIGHT <= pad.right()) {
                    touchdown(pad, top, falling);
                    return;
                }
            }
        }
        Event.Cause cause = collision();
        if (cause != null) crash(cause);
        else if (shift.hasSea() && body.bottom() > shift.sea() + 3) crash(Event.Cause.WATER);
        else if (body.y > Field.HEIGHT) crash(Event.Cause.HIT);
    }
    // --8<-- [end:move]

    // --8<-- [start:touchdown]
    private void touchdown(Pad pad, double top, double falling) {
        Body body = cab.body;
        Touchdown touchdown = Touchdown.of(body.vx, falling, Field.SOFT_LANDING, Field.HARD_LANDING);
        body.y = top - Field.CAB_HEIGHT;
        if (touchdown == Touchdown.CRASH) {
            crash(Event.Cause.TOO_FAST);
            return;
        }
        body.vx = 0;
        body.vy = 0;
        cab.pad = pad;
        events.add(new Event.Landed(body.centerX(), top, touchdown, pad));
        Rider rider = rider();
        if (rider == null || rider.state != Rider.State.RIDING) return;
        if (touchdown == Touchdown.HARD) {
            rider.tip = Math.max(0, rider.tip - Field.HARD_PENALTY);
            say(rider, Call.OUCH, 0);
        }
        if (pad.number() == rider.to() && rider.to() != Shift.UP) arrive(rider, pad);
        else if (rider.sinceCall > 2) say(rider, rider.to() == Shift.UP ? Call.UP : Call.PAD, rider.to());
    }
    // --8<-- [end:touchdown]

    // --8<-- [start:collision]
    /** What the cab touches where it is now, or null: the scenery, a lift, a moving block, the closed gate. */
    private Event.Cause collision() {
        Body body = cab.body;
        int x = (int) Math.round(body.x);
        int y = (int) Math.round(body.y);
        PixelMask mask = CabShape.mask(cab.facingLeft, cab.gearOut());
        Point hit = shift.mask().firstOverlap(mask, x, y);
        if (hit != null) return onTopOfDeck(hit) && !cab.gearOut() ? Event.Cause.NO_GEAR : Event.Cause.HIT;
        for (Pad pad : shift.pads()) {
            if (pad.isLift() && overlaps(mask, x, y, pad.x(), pad.top(time), pad.width(), Field.DECK)) {
                return cab.gearOut() ? Event.Cause.HIT : Event.Cause.NO_GEAR;
            }
        }
        if (touchesMovers()) return Event.Cause.HIT;
        if (!gateOpen && overlaps(mask, x, y, shift.gateFrom(), Field.TOP, shift.gateTo() - shift.gateFrom(),
                Field.CEILING - Field.TOP)) {
            return Event.Cause.GATE;
        }
        return null;
    }
    // --8<-- [end:collision]

    private boolean touchesMovers() {
        Body body = cab.body;
        int x = (int) Math.round(body.x);
        int y = (int) Math.round(body.y);
        PixelMask mask = CabShape.mask(cab.facingLeft, cab.gearOut());
        for (Mover mover : shift.movers()) {
            if (overlaps(mask, x, y, mover.x(time), mover.y(time), mover.width(), mover.height())) return true;
        }
        return false;
    }

    /** Whether the cab's mask at {@code x}, {@code y} has a solid pixel in the box. */
    private static boolean overlaps(PixelMask mask, int x, int y, double bx, double by, double bw, double bh) {
        // rounded like the cab's own position, so that a cab standing on a moving deck never touches it by a fraction
        int left = (int) Math.round(bx) - x;
        int top = (int) Math.round(by) - y;
        int right = (int) Math.round(bx + bw) - x;
        int bottom = (int) Math.round(by + bh) - y;
        return mask.anyIn(left, top, right - left, bottom - top);
    }

    /** A crash pixel in the top rows of a pad's deck: the cab came down on it with the gear in. */
    private boolean onTopOfDeck(Point hit) {
        for (Pad pad : shift.pads()) {
            if (!pad.isLift() && hit.x >= pad.x() && hit.x < pad.right() && hit.y >= pad.y() && hit.y < pad.y() + 2) {
                return true;
            }
        }
        return false;
    }

    private void crash(Event.Cause cause) {
        Body body = cab.body;
        phase = Phase.CRASHED;
        phaseTime = 0;
        crashes++;
        cab.pad = null;
        cab.jetSide = 0;
        cab.jetUp = cab.jetDown = cab.refuelling = false;
        events.add(new Event.Crashed(body.centerX(), body.centerY(), cause));
        // A passenger aboard or on the way in waits for the next cab, on their pad.
        Rider rider = rider();
        if (rider != null && (rider.state == Rider.State.RIDING || rider.state == Rider.State.BOARDING)) {
            rider.become(Rider.State.COMING);
            nextRider = Field.CRASH_SECONDS + Field.NEXT_RIDER;
            gateOpen = false;
        }
    }

    /** The wreck burns; then the next cab stands on the start pad, or the game is over. */
    private void wreck() {
        cab.body.remember();
        if (phaseTime < Field.CRASH_SECONDS) return;
        if (career.spare > 0) {
            career.spare--;
            cab.standOn(shift.startPad(), time);
            cab.fuel = Field.TANK;
            fuelWarned = false;
            stranded = 0;
            phase = Phase.PLAYING;
            phaseTime = 0;
            events.add(new Event.Respawned(cab.body.centerX(), cab.body.bottom()));
        } else {
            phase = Phase.OVER;
            phaseTime = 0;
            events.add(new Event.GameOver(new GameResult(career.cents, shift.number(), false)));
        }
    }

    // ---- the passengers --------------------------------------------------------------------------------

    // --8<-- [start:riders]
    private void riders(double seconds) {
        Rider rider = rider();
        if (rider == null) return;
        rider.stateTime += seconds;
        if (rider.state == Rider.State.RIDING) rider.tip = Math.max(0, rider.tip - Field.TIP_DROP * seconds);
        rider.sinceCall += seconds;
        switch (rider.state) {
            case COMING -> {
                nextRider -= seconds;
                if (nextRider <= 0 && phase == Phase.PLAYING) appear(rider);
            }
            case WAITING -> {
                if (cab.pad == rider.pad && phase == Phase.PLAYING) rider.become(Rider.State.BOARDING);
                else if (rider.sinceCall >= Field.CALL_AGAIN) say(rider, rider.tip() > 0 ? Call.TAXI : Call.HEY, 0);
            }
            case BOARDING -> {
                if (cab.pad != rider.pad) {
                    // the cab left without them: back to their spot
                    if (walk(rider, rider.spot, seconds)) rider.become(Rider.State.WAITING);
                } else if (walk(rider, cab.doorX(), seconds)) {
                    rider.become(Rider.State.RIDING);
                    rider.pad = null;
                    events.add(new Event.Boarded(rider));
                }
            }
            case RIDING -> {
                if (rider.stateTime - seconds < 0.5 && rider.stateTime >= 0.5) {
                    say(rider, rider.to() == Shift.UP ? Call.UP : Call.PAD, rider.to());
                    if (rider.to() == Shift.UP) {
                        gateOpen = true;
                        events.add(new Event.GateOpened());
                    }
                }
            }
            case LEAVING -> {
                double away = rider.x < cab.body.centerX() ? rider.pad.x() + 4 : rider.pad.right() - 4;
                walk(rider, away, seconds);
                if (rider.stateTime >= 1.0) {
                    rider.become(Rider.State.GONE);
                    current++;
                    nextRider = Field.NEXT_RIDER;
                }
            }
            case GONE -> {
            }
        }
    }
    // --8<-- [end:riders]

    /** A passenger turns up on their pad, at the end away from the cab, and calls. */
    private void appear(Rider rider) {
        Pad pad = shift.pad(rider.from()).orElseThrow();
        boolean left = cab.pad == pad ? cab.body.centerX() > pad.centerX() : rider.index() % 2 == 0;
        rider.pad = pad;
        rider.spot = left ? pad.x() + 9 : pad.right() - 9;
        rider.x = rider.spot;
        rider.facingLeft = !left;
        rider.tip = Field.TIP_START;
        rider.become(Rider.State.WAITING);
        say(rider, Call.HEY, 0);
    }

    /** Walks a passenger towards {@code x}; true when there. */
    private static boolean walk(Rider rider, double x, double seconds) {
        double step = Field.WALK * seconds;
        if (Math.abs(x - rider.x) <= step) {
            rider.x = x;
            return true;
        }
        rider.facingLeft = x < rider.x;
        rider.x += Math.signum(x - rider.x) * step;
        return false;
    }

    // --8<-- [start:arrive]
    /** At the pad they wanted: they get out and pay the fare, by the distance, and what is left of the tip. */
    private void arrive(Rider rider, Pad pad) {
        rider.pad = pad;
        rider.x = cab.doorX();
        rider.become(Rider.State.LEAVING);
        Pad from = shift.pad(rider.from()).orElseThrow();
        pay(rider, fare(from.centerX(), from.y(), pad.centerX(), pad.y()));
    }

    static int fare(double x1, double y1, double x2, double y2) {
        return Field.FARE_BASE + (int) Math.round(Math.hypot(x2 - x1, y2 - y1) / 100 * Field.FARE_PER_100);
    }
    // --8<-- [end:arrive]

    private void pay(Rider rider, int fare) {
        int tip = rider.tip();
        fares += fare;
        tips += tip;
        carried++;
        events.add(new Event.Paid(rider, fare, tip, cab.body.centerX(), cab.body.y));
        if (career.earn(fare + tip)) events.add(new Event.BonusCab(career.spare));
        if (tip > 0) say(rider, Call.THANKS, 0);
    }

    private void say(Rider rider, Call call, int pad) {
        rider.sinceCall = 0;
        events.add(new Event.Spoke(rider, call, pad));
    }

    /** Out through the gate with the last passenger: they pay, and the shift is done. */
    private void clear() {
        Rider rider = rider();
        if (rider != null && rider.state == Rider.State.RIDING) {
            Pad from = shift.pad(rider.from()).orElseThrow();
            pay(rider, fare(from.centerX(), from.y(), (shift.gateFrom() + shift.gateTo()) / 2, Field.TOP));
            rider.become(Rider.State.GONE);
            current++;
        }
        phase = Phase.CLEARED;
        phaseTime = 0;
        result = new ShiftResult(shift.number(), fares, tips, carried, crashes, time);
        events.add(new Event.Cleared(result));
    }

    // ---- what the drawing, the autopilot and the tests see ------------------------------------------------

    /** The passenger of now: waiting, riding or about to come; null when all are done. */
    public Rider rider() {
        return current < riders.size() ? riders.get(current) : null;
    }

    public List<Rider> riders() {
        return Collections.unmodifiableList(riders);
    }

    public Shift shift() {
        return shift;
    }

    public Career career() {
        return career;
    }

    public Cab cab() {
        return cab;
    }

    public boolean gateOpen() {
        return gateOpen;
    }

    public Phase phase() {
        return phase;
    }

    /** Seconds in the current phase. */
    public double phaseTime() {
        return phaseTime;
    }

    /** Seconds since the shift started. */
    public double time() {
        return time;
    }

    /** The fares and tips of this shift so far, in cents. */
    public int fares() {
        return fares;
    }

    public int tips() {
        return tips;
    }

    public int carried() {
        return carried;
    }

    /** How the shift went; null before it is done. */
    public ShiftResult result() {
        return result;
    }

    /** What happened in the last step. */
    public List<Event> events() {
        return Collections.unmodifiableList(events);
    }

    // ---- for tests -------------------------------------------------------------------------------------

    /** Puts the cab somewhere in the air at once, flying at {@code vx}, {@code vy}. */
    void put(double x, double y, double vx, double vy) {
        cab.pad = null;
        cab.body.place(x, y);
        cab.body.vx = vx;
        cab.body.vy = vy;
    }
}
