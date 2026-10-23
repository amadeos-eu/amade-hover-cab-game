package eu.amadeos.games.hovercab.ui.game;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.input.Gamepads;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.games.hovercab.bootstrap.HoverCabModule;
import eu.amadeos.games.hovercab.domain.Autopilot;
import eu.amadeos.games.hovercab.domain.Career;
import eu.amadeos.games.hovercab.domain.Field;
import eu.amadeos.games.hovercab.domain.Shift;
import eu.amadeos.games.hovercab.repository.HoverCabSettings;

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.util.Locale;

/**
 * How long a frame of each shift takes to draw, at two window sizes: {@code ./gradlew benchmark}. The
 * autopilot flies, so the jets smoke, the passengers talk and the weather moves. The frames are drawn into a
 * compatible image, as the canvas draws, but without waiting for the screen.
 */
public final class Benchmark {
    static final int WARM_UP = 60;
    static final int FRAMES = 300;

    public static void main(String[] args) throws Exception {
        Messages msg = Messages.load(HoverCabModule.class, HoverCabModule.MESSAGES, Locale.ENGLISH);
        Mixer mixer = new Mixer();
        mixer.setMuted(true);
        HoverCabGame game = new HoverCabGame(new Keyboard(), Gamepads.none(), msg,
                new HoverCabSettings(Files.createTempDirectory("hovercab").resolve("hovercab.properties")), mixer, Runnable::run);
        GraphicsConfiguration gc = GraphicsEnvironment.isHeadless() ? null
                : GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        System.out.println("ms per frame");
        for (int scale : new int[]{2, 3}) {
            for (int number = 1; number <= Shift.COUNT; number++) {
                HoverCabGame.PlayScene play = game.play(number, new Career());
                Autopilot pilot = new Autopilot(play.game().shift());
                int w = (int) Field.WIDTH * scale;
                int h = (int) Field.HEIGHT * scale;
                BufferedImage image = gc != null ? gc.createCompatibleImage(w, h) : new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                long total = 0;
                for (int i = 0; i < WARM_UP + FRAMES; i++) {
                    play.step(HoverCabGame.STEP, pilot.steer(play.game()));
                    play.step(HoverCabGame.STEP, pilot.steer(play.game()));
                    Graphics2D g = image.createGraphics();
                    g.scale(scale, scale);
                    long start = System.nanoTime();
                    game.render(g, 0.5);
                    game.renderOverlay(g, 0.5);
                    if (i >= WARM_UP) total += System.nanoTime() - start;
                    g.dispose();
                }
                System.out.printf(Locale.ROOT, "shift %2d at %4d x %4d   %6.2f%n", number, w, h, total / 1e6 / FRAMES);
            }
        }
        System.exit(0);
    }
}
