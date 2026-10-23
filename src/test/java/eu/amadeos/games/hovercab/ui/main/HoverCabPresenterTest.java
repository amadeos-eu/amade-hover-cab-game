package eu.amadeos.games.hovercab.ui.main;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.score.HighScores;
import eu.amadeos.gameengine.score.Ranking;
import eu.amadeos.gameengine.swing.RenderMode;
import eu.amadeos.games.hovercab.bootstrap.HoverCabModule;
import eu.amadeos.games.hovercab.domain.GameResult;
import eu.amadeos.games.hovercab.repository.HoverCabSettings;
import eu.amadeos.games.hovercab.ui.game.Arena;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HoverCabPresenterTest {
    @TempDir
    Path dir;

    private final List<String> log = new ArrayList<>();
    private Consumer<GameResult> gameOver;
    private HoverCabPresenter presenter;
    private HighScores scores;
    private HoverCabSettings settings;

    private final HoverCabView view = new HoverCabView() {
        @Override
        public void showOptions(boolean sound, RenderMode mode) {
            log.add("options " + sound + " " + mode);
        }

        @Override
        public void useRenderMode(RenderMode mode) {
            log.add("mode " + mode);
        }

        @Override
        public Optional<String> askName(String message) {
            log.add("ask " + message);
            return Optional.of("Ada");
        }

        @Override
        public void showScores(int highlight) {
            log.add("scores " + highlight);
        }

        @Override
        public void showAbout(String gamepads) {
            log.add("about " + gamepads);
        }
    };

    private final Arena arena = new Arena() {
        @Override
        public void showTitle() {
            log.add("title");
        }

        @Override
        public void newGame() {
            log.add("new");
        }

        @Override
        public void pause() {
            log.add("pause");
        }

        @Override
        public void setSound(boolean on) {
            log.add("sound " + on);
        }

        @Override
        public String gamepads() {
            return "Gamepads: none, 0 connected";
        }

        @Override
        public void onGameOver(Consumer<GameResult> listener) {
            gameOver = listener;
        }
    };

    @BeforeEach
    void create() {
        Messages msg = Messages.load(HoverCabModule.class, HoverCabModule.MESSAGES, Locale.ENGLISH);
        settings = new HoverCabSettings(dir.resolve("hovercab.properties"));
        scores = new HighScores(dir.resolve("scores"), Ranking.HIGHER_WINS);
        presenter = new HoverCabPresenter(view, arena, msg, settings, scores, () -> Instant.EPOCH);
        presenter.start();
    }

    @Test
    void theSettingsAreAppliedAtStartAndKept() {
        assertEquals(List.of("options true DIRECT", "mode DIRECT", "sound true", "title"), log);
        log.clear();
        presenter.setRenderMode(RenderMode.PIXEL);
        presenter.setSound(false);
        assertEquals(List.of("mode PIXEL", "sound false"), log);
        assertEquals(RenderMode.PIXEL, settings.renderMode());
    }

    @Test
    void goodEarningsEnterTheTable() {
        log.clear();
        gameOver.accept(new GameResult(4850, 3, false));
        assertEquals(List.of("ask $48.50 earned, up to shift 3.", "scores 0"), log);
        assertEquals("Ada", scores.list(HoverCabPresenter.TABLE).getFirst().name());
        assertEquals(4850, scores.list(HoverCabPresenter.TABLE).getFirst().value());
    }

    @Test
    void noMoneyNoEntry() {
        log.clear();
        gameOver.accept(new GameResult(0, 1, false));
        assertEquals(List.of(), log);
    }

    @Test
    void centsAreShownAsDollars() {
        assertEquals("$0.05", HoverCabPresenter.dollars(5));
        assertEquals("$123.40", HoverCabPresenter.dollars(12340));
    }

    @Test
    void losingTheFocusPausesTheShift() {
        log.clear();
        presenter.focusLost();
        presenter.newGame();
        presenter.showAbout();
        assertEquals(List.of("pause", "new", "about Gamepads: none, 0 connected"), log);
    }
}
