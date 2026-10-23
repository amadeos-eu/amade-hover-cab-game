package eu.amadeos.games.hovercab.domain;

import eu.amadeos.gameengine.data.TextData;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShiftTest {
    private static final String GOOD = """
            name Small
            theme night
            gate 280 360
            start 1
            pad 1 20 360 100
            pad 2 480 360 100
            riders 1>2 2>up
            """;

    private static Shift parse(String text) {
        return Shift.parse(1, "small.txt", new StringReader(text));
    }

    private static String error(String text) {
        return assertThrows(TextData.DataException.class, () -> parse(text)).getMessage();
    }

    @Test
    void everyShiftOfTheGameLoads() {
        for (int n = 1; n <= Shift.COUNT; n++) {
            Shift shift = Shift.load(n);
            assertEquals(n, shift.number());
            assertFalse(shift.name().isBlank());
            assertEquals(Shift.UP, shift.fares().getLast().to(), "shift " + n + " ends with a ride up");
            assertTrue(shift.pads().size() >= 4, "shift " + n);
        }
    }

    @Test
    void aSmallShiftIsRead() {
        Shift shift = parse(GOOD);
        assertEquals("Small", shift.name());
        assertEquals("night", shift.theme());
        assertEquals(2, shift.pads().size());
        assertEquals(1, shift.startPad().number());
        assertEquals(new Shift.Fare(1, 2), shift.fares().getFirst());
        assertFalse(shift.windy());
        assertFalse(shift.hasSea());
    }

    @Test
    void mistakesAreReportedWithTheFileAndLine() {
        assertTrue(error(GOOD + "tower 1 2\n").contains("unknown keyword \"tower\""));
        assertTrue(error(GOOD + "block marble 0 0 10 10\n").contains("\"marble\" is not one of"));
        assertTrue(error(GOOD.replace("pad 2 480", "pad 1 480")).contains("two pads 1"));
        assertTrue(error(GOOD.replace("riders 1>2 2>up", "riders 1>2")).contains("the last rider, and only the last, wants up"));
        assertTrue(error(GOOD.replace("riders 1>2 2>up", "riders 1>1 2>up")).contains("cannot go there"));
        assertTrue(error(GOOD.replace("riders 1>2", "riders 1-2")).contains("is not a fare like 2>4"));
        assertTrue(error(GOOD.replace("gate 280 360", "gate 280 300")).contains("wide enough"));
        assertTrue(error(GOOD.replace("start 1", "start 7")).contains("the start pad 7 is missing"));
        assertTrue(error(GOOD + "pad 12 0 0 50\n").contains("small.txt"));
    }

    @Test
    void theMaskHasTheCeilingButNotTheGate() {
        Shift shift = parse(GOOD);
        assertTrue(shift.mask().solid(100, (int) Field.CEILING - 2));
        assertFalse(shift.mask().solid(320, (int) Field.CEILING - 2));
        assertTrue(shift.mask().solid(50, 361), "a pad's deck");
        assertFalse(shift.mask().solid(300, 200));
    }

    @Test
    void gustsComeAndGo() {
        Shift shift = parse(GOOD + "wind 10 30 4\n");
        assertTrue(shift.windy());
        assertEquals(40, shift.strongestWind());
        double strongest = 0, weakest = 100;
        for (double t = 0; t < 8; t += 0.01) {
            strongest = Math.max(strongest, shift.wind(t));
            weakest = Math.min(weakest, shift.wind(t));
        }
        assertEquals(10, weakest, 1e-9);
        assertTrue(strongest > 30 && strongest <= 40);
    }
}
