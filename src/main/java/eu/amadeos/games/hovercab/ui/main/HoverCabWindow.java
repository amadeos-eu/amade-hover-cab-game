package eu.amadeos.games.hovercab.ui.main;

import com.google.inject.Inject;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.i18n.Mnemonics;
import eu.amadeos.commons.swing.EdtExecutor;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.gameengine.score.HighScores;
import eu.amadeos.gameengine.score.PlayerName;
import eu.amadeos.gameengine.swing.FullScreen;
import eu.amadeos.gameengine.swing.GameCanvas;
import eu.amadeos.gameengine.swing.GameMenus;
import eu.amadeos.gameengine.swing.GameTexts;
import eu.amadeos.gameengine.swing.HighScoreDialog;
import eu.amadeos.gameengine.swing.RenderMode;
import eu.amadeos.games.hovercab.bootstrap.AmadeHoverCabApplication;
import eu.amadeos.games.hovercab.domain.Field;
import eu.amadeos.games.hovercab.ui.game.HoverCabGame;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Optional;

/**
 * The game window: a menu and the field, a {@link GameCanvas} the game loop draws on. The window can be
 * resized or switched to full screen with F11; the field scales with it, in the render mode chosen under
 * <i>Game → Graphics</i>.
 */
public final class HoverCabWindow extends JFrame implements HoverCabView {
    private final Messages msg;
    private final GameMenus menus = new GameMenus(GameTexts.load());
    private final HighScores scores;
    private final PlayerName playerName;
    private final GameCanvas canvas;
    private final FullScreen fullScreen;
    private final JCheckBoxMenuItem sound;
    private JMenu graphics;
    private HoverCabPresenter presenter;

    @Inject
    public HoverCabWindow(Messages msg, HighScores scores, PlayerName playerName, HoverCabGame game, Keyboard keyboard) {
        super(msg.get("window.title"));
        EdtExecutor.requireEdt();
        this.msg = msg;
        this.scores = scores;
        this.playerName = playerName;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        java.net.URL icon = getClass().getResource("/icons/amade-hover-cab-game.png");
        if (icon != null) setIconImage(new ImageIcon(icon).getImage());

        // --8<-- [start:canvas]
        // The canvas is a heavyweight AWT component: lightweight menus would open behind it.
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
        // Two layers: the world, and the overlay with the top bar, the speech bubbles and the texts, always at
        // the screen's full size. The screen is one fixed field: on a wide screen it keeps black bars.
        canvas = new GameCanvas((int) Field.WIDTH, (int) Field.HEIGHT, game::render, game::renderOverlay);
        keyboard.attach(canvas);
        // --8<-- [end:canvas]
        getContentPane().add(canvas, BorderLayout.CENTER);
        fullScreen = new FullScreen(this, canvas);
        sound = Mnemonics.apply(new JCheckBoxMenuItem(), msg.get("menu.game.sound"));
        sound.setName("game-sound");
        sound.addActionListener(e -> {
            presenter.setSound(sound.isSelected());
            canvas.requestFocusInWindow();
        });
        setJMenuBar(menuBar());
        // Another program took the focus: pause. Not for the moment F11 takes it while switching.
        fullScreen.whenDeactivated(() -> {
            if (presenter != null) presenter.focusLost();
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                canvas.requestFocusInWindow();
            }
        });
        pack();
    }

    /** Connects the window with the presenter; call once before {@link HoverCabPresenter#start()}. */
    public void bind(HoverCabPresenter presenter) {
        this.presenter = presenter;
    }

    public GameCanvas canvas() {
        return canvas;
    }

    public FullScreen fullScreen() {
        return fullScreen;
    }

    /** Runs the action when the window closes, but not when it only switches to or from full screen. */
    public void whenClosed(Runnable action) {
        fullScreen.whenClosed(action);
    }

    // --8<-- [start:menu]
    private JMenuBar menuBar() {
        JMenu game = menus.game();
        game.add(focusing(menus.newGame(() -> presenter.newGame())));
        // No shortcut: P is the game's own pause key.
        JMenuItem pause = Mnemonics.apply(new JMenuItem(), msg.get("menu.game.pause"));
        pause.setName("game-pause");
        pause.addActionListener(e -> presenter.pause());
        game.add(focusing(pause));
        game.addSeparator();
        game.add(sound);
        graphics = menus.graphics(RenderMode.DIRECT, mode -> {
            presenter.setRenderMode(mode);
            canvas.requestFocusInWindow();
        });
        game.add(graphics);
        game.add(focusing(menus.frameTimes(canvas::setShowFrameTimes)));
        game.add(focusing(menus.fullScreen(fullScreen::toggle)));
        game.addSeparator();
        game.add(menus.highScores(() -> presenter.showScores()));
        game.addSeparator();
        game.add(menus.exit(this::dispose));
        JMenu help = menus.help();
        help.add(menus.about(msg.get("window.title"), () -> presenter.showAbout()));
        JMenuBar bar = new JMenuBar();
        bar.add(game);
        bar.add(help);
        return bar;
    }
    // --8<-- [end:menu]

    /** After a menu command the keys belong to the game again. */
    private <T extends JMenuItem> T focusing(T item) {
        item.addActionListener(e -> canvas.requestFocusInWindow());
        return item;
    }

    // ---- HoverCabView ------------------------------------------------------------------------------

    @Override
    public void showOptions(boolean soundOn, RenderMode mode) {
        sound.setSelected(soundOn);
        for (int i = 0; i < graphics.getItemCount(); i++) {
            JMenuItem item = graphics.getItem(i);
            item.setSelected(("game-graphics-" + mode.id()).equals(item.getName()));
        }
    }

    @Override
    public void useRenderMode(RenderMode mode) {
        canvas.setRenderMode(mode);
    }

    @Override
    public Optional<String> askName(String message) {
        Optional<String> name = HighScoreDialog.askName(this, message, playerName);
        canvas.requestFocusInWindow();
        return name;
    }

    @Override
    public void showScores(int highlight) {
        HighScoreDialog.show(this, scores,
                List.of(new HighScoreDialog.Table(HoverCabPresenter.TABLE, msg.get("scores.table"))),
                cents -> HoverCabPresenter.dollars(cents), HoverCabPresenter.TABLE, highlight);
        canvas.requestFocusInWindow();
    }

    @Override
    public void showAbout(String gamepads) {
        // Swing's HTML only wraps lines with a fixed body width.
        JOptionPane.showMessageDialog(this,
                "<html><body style='width: 320px'><center><h2>"
                        + msg.get("window.title") + " " + AmadeHoverCabApplication.version() + "</h2>"
                        + "<p>" + msg.get("dialog.about.text") + "</p><br>"
                        + "<p>" + msg.get("dialog.about.rules") + "</p><br>"
                        + "<p>" + gamepads + "</p><br>"
                        + "<small>amade-hover-cab-game &bull; &copy; 2026 datazuul.com &bull; EUPL-1.2</small>"
                        + "</center></html>",
                Mnemonics.strip(menus.texts().format("menu.help.about", msg.get("window.title"))),
                JOptionPane.INFORMATION_MESSAGE);
        canvas.requestFocusInWindow();
    }
}
