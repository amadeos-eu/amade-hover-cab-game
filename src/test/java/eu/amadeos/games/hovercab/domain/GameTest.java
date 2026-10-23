package eu.amadeos.games.hovercab.domain;

import eu.amadeos.gameengine.physics.Touchdown;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The rules, on a small shift made for the tests: a pad at each end, a wall, a fuel pad and a lift. */
class GameTest {
    private static final double STEP = 1.0 / 120;
    private static final String TEST_SHIFT = """
            name Test
            theme day
            gate 280 360
            start 1
            block concrete 0 360 640 40
            block brick 200 200 40 160
            pad 1 20 360 100
            fuel 300 360 60
            lift 3 400 300 60 150 4
            pad 2 480 360 100
            riders 1>2 2>up
            """;

    private final Career career = new Career();
    private final List<Event> events = new ArrayList<>();

    private static Shift shift(String text) {
        return Shift.parse(99, "test.txt", new StringReader(text));
    }

    private Game game() {
        return new Game(shift(TEST_SHIFT), career);
    }

    /** Runs the game for so many seconds with one input held, keeping every event. */
    private void run(Game game, double seconds, Input input) {
        for (int i = 0; i < Math.round(seconds / STEP); i++) {
            game.update(STEP, input);
            events.addAll(game.events());
        }
    }

    private void until(Game game, Rider.State state) {
        for (int i = 0; i < 120 * 20 && game.rider().state() != state; i++) run(game, STEP, Input.NONE);
        assertEquals(state, game.rider().state());
    }

    private <E extends Event> List<E> all(Class<E> type) {
        return events.stream().filter(type::isInstance).map(type::cast).toList();
    }

    private Event.Cause crashCause() {
        List<Event.Crashed> crashes = all(Event.Crashed.class);
        assertEquals(1, crashes.size(), "one crash: " + events);
        return crashes.getFirst().cause();
    }

    // --8<-- [start:test-fare]
    @Test
    void aPassengerBoardsOnTheirPadAndPaysOnTheirs() {
        Game game = game();
        until(game, Rider.State.RIDING);
        game.put(514, 340, 0, 20);                         // just over pad 2, sinking slowly
        run(game, 1, Input.NONE);
        Event.Landed landed = all(Event.Landed.class).getFirst();
        assertEquals(Touchdown.SOFT, landed.touchdown());
        Event.Paid paid = all(Event.Paid.class).getFirst();
        assertEquals(Game.fare(70, 360, 530, 360), paid.fare());
        assertEquals(660, paid.fare(), "two dollars and a dollar for every hundred units");
        assertTrue(paid.tip() > 400 && paid.tip() < Field.TIP_START, "the tip melted a little: " + paid.tip());
        assertEquals(paid.fare() + paid.tip(), career.cents());
    }
    // --8<-- [end:test-fare]

    @Test
    void aHardLandingCostsTipAndTheyComplain() {
        Game game = game();
        until(game, Rider.State.RIDING);
        game.put(514, 330, 0, 50);
        run(game, 1, Input.NONE);
        assertEquals(Touchdown.HARD, all(Event.Landed.class).getFirst().touchdown());
        assertTrue(all(Event.Spoke.class).stream().anyMatch(s -> s.call() == Call.OUCH));
        assertTrue(all(Event.Paid.class).getFirst().tip() < Field.TIP_START - Field.HARD_PENALTY);
    }

    @Test
    void comingDownTooFastIsACrash() {
        Game game = game();
        game.put(514, 300, 0, 100);
        run(game, 1, Input.NONE);
        assertEquals(Event.Cause.TOO_FAST, crashCause());
        assertEquals(Game.Phase.CRASHED, game.phase());
    }

    @Test
    void landingWithTheGearInIsACrash() {
        Game game = game();
        game.put(514, 300, 0, 0);
        run(game, STEP, new Input(0, false, false, true));        // gear in
        run(game, 1.5, Input.NONE);
        assertEquals(Event.Cause.NO_GEAR, crashCause());
    }

    @Test
    void touchingAWallIsACrash() {
        Game game = game();
        game.put(150, 250, 80, 0);
        run(game, 1, Input.NONE);
        assertEquals(Event.Cause.HIT, crashCause());
    }

    @Test
    void theClosedGateIsSolid() {
        Game game = game();
        game.put(304, 40, 0, -100);
        run(game, 0.2, Input.NONE);
        assertEquals(Event.Cause.GATE, crashCause());
    }

    @Test
    void aPassengerAboardAtACrashWaitsForTheNextCab() {
        Game game = game();
        until(game, Rider.State.RIDING);
        game.put(150, 250, 80, 0);
        run(game, 0.5, Input.NONE);
        assertEquals(Rider.State.COMING, game.rider().state());
        run(game, Field.CRASH_SECONDS, Input.NONE);
        assertEquals(Game.Phase.PLAYING, game.phase());
        assertEquals(Field.SPARE_CABS - 1, career.spare());
        assertEquals(1, game.cab().pad().number(), "back on the start pad");
        assertEquals(Field.TANK, game.cab().fuel());
    }

    @Test
    void theLastCabLostEndsTheGame() {
        Game game = game();
        career.spare = 0;
        career.cents = 1234;
        game.put(150, 250, 80, 0);
        run(game, Field.CRASH_SECONDS + 0.5, Input.NONE);
        assertEquals(Game.Phase.OVER, game.phase());
        GameResult result = all(Event.GameOver.class).getFirst().result();
        assertEquals(new GameResult(1234, 99, false), result);
    }

    // --8<-- [start:test-gate]
    @Test
    void theLastPassengerWantsUpAndTheGateOpens() {
        Game game = game();
        until(game, Rider.State.RIDING);
        game.put(514, 340, 0, 20);
        run(game, 1, Input.NONE);                          // the first one is home, the next one waits on pad 2
        until(game, Rider.State.RIDING);
        run(game, 0.6, Input.NONE);
        assertTrue(game.gateOpen());
        assertTrue(all(Event.Spoke.class).stream().anyMatch(s -> s.call() == Call.UP));
        game.put(304, 60, 0, -60);
        run(game, 1, new Input(0, true, false, false));   // up and out
        assertEquals(Game.Phase.CLEARED, game.phase());
        ShiftResult result = game.result();
        assertNotNull(result);
        assertEquals(2, result.passengers());
        assertEquals(0, result.crashes());
        assertEquals(2, all(Event.Paid.class).size());
    }
    // --8<-- [end:test-gate]

    @Test
    void theFuelPadFillsTheTankForMoney() {
        Game game = game();
        Pad fuel = game.shift().pad(0).orElseThrow();
        game.cab().standOn(fuel, 0);
        game.cab().fuel = 50;
        career.cents = 1000;
        run(game, 1, Input.NONE);
        assertEquals(50 + Field.REFUEL_RATE, game.cab().fuel(), 0.01);
        assertEquals(1000 - Math.round(Field.REFUEL_RATE * Field.FUEL_PRICE), career.cents(), 1);
        assertTrue(game.cab().refuelling());
    }

    @Test
    void anEmptyTankStrandsTheCab() {
        Game game = game();
        game.cab().fuel = 0;
        run(game, Game.STRANDED_SECONDS + 0.1, new Input(0, true, false, false));
        assertEquals(Event.Cause.EMPTY, crashCause());
    }

    @Test
    void theMainJetBurnsFuelAndLifts() {
        Game game = game();
        run(game, 0.5, new Input(0, true, false, false));
        assertFalse(game.cab().landed());
        assertTrue(game.cab().body().y < 344 - 5, "rose");
        assertTrue(game.cab().fuel() < Field.TANK);
        assertEquals(1, all(Event.TookOff.class).size());
    }

    @Test
    void aCabOnALiftRidesWithIt() {
        Game game = game();
        Pad lift = game.shift().pad(3).orElseThrow();
        game.cab().standOn(lift, 0);
        run(game, 2, Input.NONE);
        assertEquals(lift.top(game.time()) - Field.CAB_HEIGHT, game.cab().body().y, 1e-9);
        assertTrue(game.cab().body().y < 300 - Field.CAB_HEIGHT - 50, "went up");
        assertTrue(game.cab().landed());
    }

    @Test
    void theSeaSwallowsTheCab() {
        Game game = new Game(shift("""
                name Sea
                theme day
                gate 280 360
                start 1
                sea 360
                block concrete 0 360 120 40
                pad 1 20 360 100
                riders 1>up
                """), career);
        game.put(300, 330, 0, 30);
        run(game, 1, Input.NONE);
        assertEquals(Event.Cause.WATER, crashCause());
    }

    @Test
    void theWindPushesTheCab() {
        Game game = new Game(shift(TEST_SHIFT.replace("start 1", "start 1\nwind 40 0 5")), career);
        game.put(300, 100, 0, 0);
        run(game, 0.5, new Input(0, true, false, false));
        assertTrue(game.cab().body().vx > 15, "drifts right: " + game.cab().body().vx);
    }

    @Test
    void everyHundredDollarsEarnedBringsASpareCab() {
        assertFalse(career.earn(9_999));
        assertTrue(career.earn(1));
        assertEquals(Field.SPARE_CABS + 1, career.spare());
        career.pay(10_500.4);
        assertEquals(0, career.cents(), "fuel never takes more than the till");
        assertEquals(10_000, career.earned());
    }

    @Test
    void longerRidesPayMore() {
        assertEquals(Field.FARE_BASE, Game.fare(0, 0, 0, 0));
        assertTrue(Game.fare(0, 0, 600, 0) > Game.fare(0, 0, 300, 0));
    }
}
