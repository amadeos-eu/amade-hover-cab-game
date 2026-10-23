package eu.amadeos.games.hovercab.ui.main;

import com.google.inject.Inject;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.score.HighScores;
import eu.amadeos.gameengine.score.Score;
import eu.amadeos.gameengine.swing.RenderMode;
import eu.amadeos.games.hovercab.domain.GameResult;
import eu.amadeos.games.hovercab.repository.HoverCabSettings;
import eu.amadeos.games.hovercab.ui.game.Arena;

import java.time.Instant;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Everything around the shift, without Swing: the menu commands, the settings, pausing when the window
 * loses the focus, and the best earnings. All methods run on the Swing thread.
 */
public final class HoverCabPresenter {
    /** The one table of best earnings, in cents. */
    static final String TABLE = "earnings";

    private final HoverCabView view;
    private final Arena arena;
    private final Messages msg;
    private final HoverCabSettings settings;
    private final HighScores scores;
    private final Supplier<Instant> now;

    @Inject
    public HoverCabPresenter(HoverCabView view, Arena arena, Messages msg, HoverCabSettings settings, HighScores scores) {
        this(view, arena, msg, settings, scores, Instant::now);
    }

    HoverCabPresenter(HoverCabView view, Arena arena, Messages msg, HoverCabSettings settings, HighScores scores,
                      Supplier<Instant> now) {
        this.view = view;
        this.arena = arena;
        this.msg = msg;
        this.settings = settings;
        this.scores = scores;
        this.now = now;
    }

    public void start() {
        boolean sound = settings.sound();
        RenderMode mode = settings.renderMode();
        view.showOptions(sound, mode);
        view.useRenderMode(mode);
        arena.setSound(sound);
        arena.onGameOver(this::gameOver);
        arena.showTitle();
    }

    public void newGame() {
        arena.newGame();
    }

    public void pause() {
        arena.pause();
    }

    /** The window lost the focus: nobody is at the keys, so a running shift waits. */
    public void focusLost() {
        arena.pause();
    }

    public void setSound(boolean on) {
        settings.setSound(on);
        arena.setSound(on);
    }

    public void setRenderMode(RenderMode mode) {
        settings.setRenderMode(mode);
        view.useRenderMode(mode);
    }

    public void showScores() {
        view.showScores(-1);
    }

    public void showAbout() {
        view.showAbout(arena.gamepads());
    }

    /** Cents as the high score table shows them: 4850 is "$48.50". */
    static String dollars(long cents) {
        return String.format(Locale.ROOT, "$%d.%02d", cents / 100, cents % 100);
    }

    /** A game ended: good earnings enter the table. */
    void gameOver(GameResult result) {
        if (result.cents() <= 0 || !scores.qualifies(TABLE, result.cents())) return;
        view.askName(msg.format("scores.reached", dollars(result.cents()), result.shift())).ifPresent(name -> {
            int place = scores.add(TABLE, new Score(name, result.cents(), now.get()));
            view.showScores(place);
        });
    }
}
