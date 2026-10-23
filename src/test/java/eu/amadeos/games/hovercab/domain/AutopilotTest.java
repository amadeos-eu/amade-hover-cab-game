package eu.amadeos.games.hovercab.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every shift can be flown: the autopilot takes every passenger and leaves through the gate, without a crash. */
class AutopilotTest {
    // --8<-- [start:test-autopilot]
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12})
    void theAutopilotFliesTheShift(int number) {
        Shift shift = Shift.load(number);
        Game game = new Game(shift, new Career());
        Autopilot pilot = new Autopilot(shift);
        for (int i = 0; i < 120 * 400 && game.phase() == Game.Phase.PLAYING; i++) game.update(1 / 120.0, pilot.steer(game));
        assertEquals(Game.Phase.CLEARED, game.phase(), "shift " + number);
        assertEquals(0, game.result().crashes());
        assertEquals(shift.fares().size(), game.result().passengers());
        assertTrue(game.result().tips() > 0, "some tips");
    }
    // --8<-- [end:test-autopilot]
}
