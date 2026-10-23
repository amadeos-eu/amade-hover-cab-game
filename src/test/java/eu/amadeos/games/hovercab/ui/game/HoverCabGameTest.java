package eu.amadeos.games.hovercab.ui.game;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.input.Gamepads;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.games.hovercab.bootstrap.HoverCabModule;
import eu.amadeos.games.hovercab.domain.Career;
import eu.amadeos.games.hovercab.domain.Field;
import eu.amadeos.games.hovercab.domain.Game;
import eu.amadeos.games.hovercab.domain.GameResult;
import eu.amadeos.games.hovercab.repository.HoverCabSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The game without a window and without the loop thread: the test presses keys and calls the steps itself,
 * so everything happens in order on the test thread.
 */
class HoverCabGameTest {
    private static final double STEP = HoverCabGame.STEP;

    @TempDir
    Path dir;

    private final long[] now = {0};
    private final Keyboard keyboard = new Keyboard(() -> now[0]);
    private final List<GameResult> results = new ArrayList<>();
    private HoverCabGame game;

    @BeforeEach
    void create() {
        Messages msg = Messages.load(HoverCabModule.class, HoverCabModule.MESSAGES, Locale.ENGLISH);
        Mixer mixer = new Mixer();
        mixer.setMuted(true);
        game = new HoverCabGame(keyboard, Gamepads.none(), msg, new HoverCabSettings(dir.resolve("hovercab.properties")),
                mixer, Runnable::run);
        game.onGameOver(results::add);
    }

    private void tap(int key) {
        keyboard.pressed(key, now[0]);
        keyboard.released(key, now[0] + 1_000_000);
        steps(1);
    }

    private void steps(int n) {
        for (int i = 0; i < n; i++) {
            now[0] += (long) (STEP * 1e9);
            game.update(STEP);
        }
    }

    private void seconds(double seconds) {
        steps((int) Math.round(seconds * HoverCabGame.UPDATES_PER_SECOND));
    }

    private HoverCabGame.PlayScene play() {
        tap(KeyEvent.VK_ENTER);
        return assertInstanceOf(HoverCabGame.PlayScene.class, game.top());
    }

    @Test
    void enterStartsTheFirstShiftAndUpLifts() {
        assertInstanceOf(HoverCabGame.TitleScene.class, game.top());
        HoverCabGame.PlayScene play = play();
        assertEquals(1, play.game().shift().number());
        assertTrue(play.game().cab().landed());
        keyboard.pressed(KeyEvent.VK_UP, now[0]);
        seconds(0.5);
        assertFalse(play.game().cab().landed());
        assertTrue(play.game().cab().jetUp());
        keyboard.released(KeyEvent.VK_UP, now[0]);
    }

    @Test
    void spaceMovesTheGearInTheAir() {
        HoverCabGame.PlayScene play = play();
        keyboard.pressed(KeyEvent.VK_W, now[0]);
        seconds(0.3);
        keyboard.released(KeyEvent.VK_W, now[0]);
        tap(KeyEvent.VK_SPACE);
        seconds(0.5);
        assertEquals(0, play.game().cab().gear(), 1e-9);
    }

    @Test
    void pFreezesTheShiftAndEnterContinuesIt() {
        HoverCabGame.PlayScene play = play();
        tap(KeyEvent.VK_P);
        assertInstanceOf(HoverCabGame.PauseScene.class, game.top());
        double time = play.game().time();
        seconds(1);
        assertEquals(time, play.game().time());
        tap(KeyEvent.VK_ENTER);
        seconds(0.5);
        assertTrue(play.game().time() > time);
    }

    @Test
    void escapeInThePauseLeavesToTheTitle() {
        play();
        tap(KeyEvent.VK_P);
        tap(KeyEvent.VK_ESCAPE);
        assertInstanceOf(HoverCabGame.TitleScene.class, game.top());
    }

    @Test
    void aShiftFlownToTheEndShowsTheResultsAndLeadsToTheNext() {
        Career career = new Career();
        HoverCabGame.PlayScene play = game.play(1, career);
        var pilot = new eu.amadeos.games.hovercab.domain.Autopilot(play.game().shift());
        for (int i = 0; i < 120 * 300 && play.game().phase() == Game.Phase.PLAYING; i++) {
            play.step(STEP, pilot.steer(play.game()));
        }
        assertEquals(Game.Phase.CLEARED, play.game().phase());
        seconds(HoverCabGame.CLEARED_SECONDS + 1);
        assertInstanceOf(HoverCabGame.ClearedScene.class, game.top());
        tap(KeyEvent.VK_ENTER);
        HoverCabGame.PlayScene next = assertInstanceOf(HoverCabGame.PlayScene.class, game.top());
        assertEquals(2, next.game().shift().number());
        assertTrue(career.cents() > 0);
    }

    @Test
    void aCabThatFallsIsLostAndTheLastOneEndsTheGame() {
        HoverCabGame.PlayScene play = play();
        for (int cab = 0; cab <= Field.SPARE_CABS && results.isEmpty(); cab++) {
            keyboard.pressed(KeyEvent.VK_UP, now[0]);
            seconds(0.4);
            keyboard.released(KeyEvent.VK_UP, now[0]);
            tap(KeyEvent.VK_SPACE);                  // gear in: the next touch is a crash
            seconds(Field.CRASH_SECONDS + 3);
        }
        assertEquals(1, results.size());
        assertEquals(Game.Phase.OVER, play.game().phase());
        assertInstanceOf(HoverCabGame.OverScene.class, game.top());
        seconds(2);
        tap(KeyEvent.VK_ENTER);
        assertInstanceOf(HoverCabGame.TitleScene.class, game.top());
    }

    @Test
    void theDemoFliesThePassengers() {
        HoverCabGame.TitleScene title = assertInstanceOf(HoverCabGame.TitleScene.class, game.top());
        seconds(40);
        assertTrue(title.demo().carried() > 0, "the autopilot took someone");
    }

    @Test
    void everySceneDrawsWorldAndOverlay() {
        BufferedImage image = new BufferedImage((int) Field.WIDTH, (int) Field.HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        seconds(2);
        game.render(g, 0.5);
        game.renderOverlay(g, 0.5);
        play();
        seconds(1.5);
        game.render(g, 0.5);
        game.renderOverlay(g, 0.5);
        tap(KeyEvent.VK_P);
        game.render(g, 0.5);
        game.renderOverlay(g, 0.5);
        g.dispose();
        assertTrue(image.getRGB(10, 10) != 0, "something was drawn");
    }

    @Test
    void everyShiftIsDrawn() {
        BufferedImage image = new BufferedImage((int) Field.WIDTH, (int) Field.HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        for (int n = 1; n <= 12; n++) {
            game.play(n, new Career());
            seconds(1.2);
            game.render(g, 1);
            game.renderOverlay(g, 1);
        }
        g.dispose();
    }
}
