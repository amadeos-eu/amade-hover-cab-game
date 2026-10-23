package eu.amadeos.games.hovercab.ui.game;

import com.google.inject.Inject;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.input.Controls;
import eu.amadeos.gameengine.input.Gamepad;
import eu.amadeos.gameengine.input.Gamepads;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.gameengine.loop.GameLoop;
import eu.amadeos.gameengine.scene.Scene;
import eu.amadeos.gameengine.scene.SceneStack;
import eu.amadeos.gameengine.swing.GameCanvas;
import eu.amadeos.games.hovercab.domain.Autopilot;
import eu.amadeos.games.hovercab.domain.Career;
import eu.amadeos.games.hovercab.domain.Event;
import eu.amadeos.games.hovercab.domain.Game;
import eu.amadeos.games.hovercab.domain.GameResult;
import eu.amadeos.games.hovercab.domain.Input;
import eu.amadeos.games.hovercab.domain.Shift;
import eu.amadeos.games.hovercab.repository.HoverCabSettings;

import javax.swing.SwingUtilities;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * The running game: the loop, the controls, the effects and the scenes (the title with the autopilot flying
 * the first shift, a shift being flown, the pause, the end of a shift and the end of the game). Everything
 * here runs on the game loop thread, except the {@link Arena} methods, which only post commands to it.
 */
public final class HoverCabGame implements Arena {
    enum Action { LEFT, RIGHT, UP, DOWN, GEAR, PAUSE, BACK, OK }

    static final int UPDATES_PER_SECOND = 120;
    static final int FRAMES_PER_SECOND = 60;
    static final double STEP = 1.0 / UPDATES_PER_SECOND;
    /** How long the cab is seen leaving through the gate before the results show. */
    static final double CLEARED_SECONDS = 1.2;

    private final Keyboard keyboard;
    private final Gamepads gamepads;
    private final Messages msg;
    private final Mixer mixer;
    private final Painter painter = new Painter();
    private final Hud hud;
    private final Effects effects;
    private final SceneStack scenes = new SceneStack();
    private final Controls<Action> controls;
    private final List<Shift> shifts = new ArrayList<>();
    private final Executor ui;
    private volatile Consumer<GameResult> onGameOver = result -> { };
    private GameLoop loop;

    @Inject
    public HoverCabGame(Keyboard keyboard, Gamepads gamepads, Messages msg, HoverCabSettings settings, Mixer mixer) {
        this(keyboard, gamepads, msg, settings, mixer, SwingUtilities::invokeLater);
    }

    HoverCabGame(Keyboard keyboard, Gamepads gamepads, Messages msg, HoverCabSettings settings, Mixer mixer, Executor ui) {
        this.keyboard = keyboard;
        this.gamepads = gamepads;
        this.msg = msg;
        this.mixer = mixer;
        this.ui = ui;
        this.hud = new Hud(msg);
        this.effects = new Effects(mixer, new Voices(msg));
        for (int i = 1; i <= Shift.COUNT; i++) shifts.add(Shift.load(i));
        // --8<-- [start:controls]
        controls = new Controls<>(Action.class, keyboard)
                .bind(Action.LEFT, settings.keys("left", KeyEvent.VK_LEFT, KeyEvent.VK_A))
                .bind(Action.RIGHT, settings.keys("right", KeyEvent.VK_RIGHT, KeyEvent.VK_D))
                .bind(Action.UP, settings.keys("up", KeyEvent.VK_UP, KeyEvent.VK_W))
                .bind(Action.DOWN, settings.keys("down", KeyEvent.VK_DOWN, KeyEvent.VK_S))
                .bind(Action.GEAR, settings.keys("gear", KeyEvent.VK_SPACE, KeyEvent.VK_G))
                .bind(Action.PAUSE, settings.keys("pause", KeyEvent.VK_P, KeyEvent.VK_ESCAPE))
                .bind(Action.BACK, KeyEvent.VK_ESCAPE)
                .bind(Action.OK, KeyEvent.VK_ENTER, KeyEvent.VK_SPACE);
        // One pilot, any gamepad: the cross steers, A is the main jet, X the gear.
        for (Gamepad pad : gamepads.pads()) {
            controls.bind(Action.LEFT, pad, Gamepad.Button.LEFT)
                    .bind(Action.RIGHT, pad, Gamepad.Button.RIGHT)
                    .bind(Action.UP, pad, Gamepad.Button.UP, Gamepad.Button.A)
                    .bind(Action.DOWN, pad, Gamepad.Button.DOWN, Gamepad.Button.B)
                    .bind(Action.GEAR, pad, Gamepad.Button.X, Gamepad.Button.Y)
                    .bind(Action.PAUSE, pad, Gamepad.Button.START)
                    .bind(Action.BACK, pad, Gamepad.Button.BACK)
                    .bind(Action.OK, pad, Gamepad.Button.A, Gamepad.Button.START);
        }
        // --8<-- [end:controls]
        scenes.push(new TitleScene());
    }

    /** Starts the loop; the canvas draws the scenes' worlds and then their overlays. */
    public void start(GameCanvas canvas) {
        loop = new GameLoop(UPDATES_PER_SECOND, FRAMES_PER_SECOND, this::update, canvas::render);
        loop.start();
    }

    public void stop() {
        if (loop != null) loop.stop();
    }

    void update(double seconds) {
        keyboard.poll();
        gamepads.poll();
        scenes.update(seconds);
    }

    public void render(Graphics2D g, double alpha) {
        scenes.render(g, alpha);
    }

    public void renderOverlay(Graphics2D g, double alpha) {
        scenes.renderOverlay(g, alpha);
    }

    // ---- Arena: called on the Swing thread, carried out on the loop thread -----------------------

    @Override
    public void showTitle() {
        run(() -> scenes.replace(new TitleScene()));
    }

    @Override
    public void newGame() {
        run(() -> scenes.replace(new PlayScene(new Career(), 1)));
    }

    @Override
    public void pause() {
        run(() -> {
            if (scenes.top() instanceof PlayScene) scenes.push(new PauseScene());
        });
    }

    @Override
    public void setSound(boolean on) {
        mixer.setMuted(!on);
    }

    @Override
    public String gamepads() {
        return msg.format("about.gamepads", gamepads.backend(), gamepads.connected());
    }

    @Override
    public void onGameOver(Consumer<GameResult> listener) {
        onGameOver = listener;
    }

    private void run(Runnable command) {
        if (loop != null && loop.running()) loop.post(command);
        else command.run();
    }

    // ---- for tests -------------------------------------------------------------------------------

    Scene top() {
        return scenes.top();
    }

    Effects effects() {
        return effects;
    }

    /** Flies shift {@code number} at once, without the title. */
    PlayScene play(int number, Career career) {
        PlayScene play = new PlayScene(career, number);
        scenes.replace(play);
        return play;
    }

    // ---- the scenes ------------------------------------------------------------------------------

    /** The autopilot flies the first shift, again and again, without a sound. */
    final class TitleScene implements Scene {
        private Game demo;
        private Autopilot pilot;
        private double time;

        TitleScene() {
            effects.clear();
            restart();
        }

        private void restart() {
            Shift shift = shifts.getFirst();
            demo = new Game(shift, new Career());
            pilot = new Autopilot(shift);
        }

        @Override
        public void update(double seconds) {
            time += seconds;
            if (controls.wasPressed(Action.OK)) {
                scenes.replace(new PlayScene(new Career(), 1));
                return;
            }
            demo.update(seconds, pilot.steer(demo));
            effects.react(demo, false);
            effects.update(seconds, demo, false);
            if (demo.phase() == Game.Phase.CLEARED && demo.phaseTime() > 1 || demo.phase() == Game.Phase.OVER) restart();
        }

        @Override
        public void render(Graphics2D g, double alpha) {
            painter.paint(g, demo, effects, alpha);
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            hud.title(g, time);
        }

        Game demo() {
            return demo;
        }
    }

    // --8<-- [start:play]
    /** A shift being flown. */
    final class PlayScene implements Scene {
        private final Career career;
        private final Game game;
        private Event.Cause crash;
        private double sinceCrash;

        PlayScene(Career career, int number) {
            this.career = career;
            this.game = new Game(shifts.get(number - 1), career);
            effects.clear();
            effects.prepare(game.shift());
            effects.startTune();
        }

        @Override
        public void update(double seconds) {
            if (game.phase() == Game.Phase.CLEARED && game.phaseTime() >= CLEARED_SECONDS) {
                scenes.replace(new ClearedScene(this));
                return;
            }
            if (controls.wasPressed(Action.PAUSE)) {
                scenes.push(new PauseScene());
                return;
            }
            Input input = new Input(controls.axis(Action.LEFT, Action.RIGHT), controls.isDown(Action.UP),
                    controls.isDown(Action.DOWN), controls.wasPressed(Action.GEAR));
            step(seconds, input);
        }

        void step(double seconds, Input input) {
            game.update(seconds, input);
            effects.react(game, true);
            effects.update(seconds, game, true);
            sinceCrash += seconds;
            for (Event event : game.events()) {
                if (event instanceof Event.Crashed(double x, double y, Event.Cause cause)) {
                    crash = cause;
                    sinceCrash = 0;
                }
                if (event instanceof Event.GameOver(GameResult result)) {
                    scenes.replace(new OverScene(this, result));
                    Consumer<GameResult> listener = onGameOver;
                    ui.execute(() -> listener.accept(result));
                }
            }
        }
        // --8<-- [end:play]

        @Override
        public void render(Graphics2D g, double alpha) {
            painter.paint(g, game, effects, alpha);
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            hud.bubbles(g, game, effects);
            hud.banners(g, game, crash, sinceCrash);
            hud.status(g, game);
        }

        Game game() {
            return game;
        }
    }

    /** Drawn over the frozen shift until P, Enter or Start; Esc leaves the game. */
    final class PauseScene implements Scene {
        @Override
        public void update(double seconds) {
            if (controls.wasPressed(Action.BACK)) scenes.replace(new TitleScene());
            else if (controls.wasPressed(Action.PAUSE) || controls.wasPressed(Action.OK)) scenes.pop();
        }

        @Override
        public void render(Graphics2D g, double alpha) {
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            hud.pause(g);
        }
    }

    /** The shift is done: what it brought in, then the next one, or the end after the last. */
    final class ClearedScene implements Scene {
        private final PlayScene play;
        private double shown;

        ClearedScene(PlayScene play) {
            this.play = play;
        }

        @Override
        public void update(double seconds) {
            play.step(seconds, Input.NONE);
            shown += seconds;
            if (shown < 0.8) return;          // a short wait, so a key meant for the cab does not skip the results
            int next = play.game.shift().number() + 1;
            if (controls.wasPressed(Action.OK)) {
                if (next <= shifts.size()) {
                    scenes.replace(new PlayScene(play.career, next));
                } else {
                    GameResult result = new GameResult(play.career.cents(), shifts.size(), true);
                    scenes.replace(new OverScene(play, result));
                    Consumer<GameResult> listener = onGameOver;
                    ui.execute(() -> listener.accept(result));
                }
            }
        }

        @Override
        public void render(Graphics2D g, double alpha) {
            play.render(g, alpha);
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            hud.status(g, play.game);
            hud.cleared(g, play.game.result(), play.career.cents(), play.game.shift().number() == shifts.size());
        }
    }

    /** The end: the money stays on the screen, Enter leads back to the title. */
    final class OverScene implements Scene {
        private final PlayScene play;
        private final GameResult result;
        private double shown;

        OverScene(PlayScene play, GameResult result) {
            this.play = play;
            this.result = result;
        }

        @Override
        public void update(double seconds) {
            play.game.update(seconds, Input.NONE);
            effects.update(seconds, play.game, true);
            shown += seconds;
            if (shown > 1.5 && controls.wasPressed(Action.OK)) scenes.replace(new TitleScene());
        }

        @Override
        public void render(Graphics2D g, double alpha) {
            play.render(g, alpha);
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            hud.status(g, play.game);
            hud.over(g, result);
        }

        GameResult result() {
            return result;
        }
    }
}
