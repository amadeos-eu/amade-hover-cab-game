package eu.amadeos.games.hovercab.ui.main;

import eu.amadeos.gameengine.swing.RenderMode;

import java.util.Optional;

/** What {@link HoverCabPresenter} needs from the window; tests replace it. The shift itself is drawn by the game. */
public interface HoverCabView {
    /** Shows the settings in the menu. */
    void showOptions(boolean sound, RenderMode mode);

    /** Switches the canvas to a render mode. */
    void useRenderMode(RenderMode mode);

    /** Asks for the player's name after a good result; empty if cancelled. */
    Optional<String> askName(String message);

    /** Shows the best results; {@code highlight} -1 for no new entry. */
    void showScores(int highlight);

    /** The About dialog, with a line on the gamepads. */
    void showAbout(String gamepads);
}
