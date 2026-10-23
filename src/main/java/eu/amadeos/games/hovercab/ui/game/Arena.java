package eu.amadeos.games.hovercab.ui.game;

import eu.amadeos.games.hovercab.domain.GameResult;

import java.util.function.Consumer;

/**
 * What the presenter may ask of the running game. The game runs on its own thread; every method here is
 * safe to call from the Swing thread and takes effect at the start of the game's next step.
 */
public interface Arena {
    /** Back to the title screen, where the autopilot flies. */
    void showTitle();

    /** A new game from the first shift. */
    void newGame();

    /** Pauses a running shift; does nothing elsewhere. */
    void pause();

    void setSound(boolean on);

    /** Which gamepads the game sees, for the About dialog: "Gamepads: XInput, 1 connected". */
    String gamepads();

    /** Called on the Swing thread when a game ends. */
    void onGameOver(Consumer<GameResult> listener);
}
