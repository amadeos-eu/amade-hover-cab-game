package eu.amadeos.games.hovercab.ui.game;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.input.Gamepads;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.games.hovercab.bootstrap.HoverCabModule;
import eu.amadeos.games.hovercab.domain.Autopilot;
import eu.amadeos.games.hovercab.domain.Career;
import eu.amadeos.games.hovercab.domain.Game;
import eu.amadeos.games.hovercab.repository.HoverCabSettings;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.Locale;

/** For designing: java ... Shot shift seconds out.png — the autopilot flies a shift that long, then a picture. */
public final class Shot {
    public static void main(String[] args) throws Exception {
        int shift = Integer.parseInt(args[0]);
        double seconds = Double.parseDouble(args[1]);
        Messages msg = Messages.load(HoverCabModule.class, HoverCabModule.MESSAGES, Locale.ENGLISH);
        HoverCabGame game = new HoverCabGame(new Keyboard(() -> 0), Gamepads.none(), msg,
                new HoverCabSettings(Files.createTempDirectory("cab").resolve("cab.properties")), new Mixer(), Runnable::run);
        HoverCabGame.PlayScene play = game.play(shift, new Career());
        Autopilot pilot = new Autopilot(play.game().shift());
        for (int i = 0; i < seconds * 120; i++) play.step(1 / 120.0, pilot.steer(play.game()));
        save(game, new File(args[2]));
    }

    static void save(HoverCabGame game, File file) throws Exception {
        BufferedImage image = new BufferedImage(1280, 800, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.scale(2, 2);
        game.render(g, 1);
        game.renderOverlay(g, 1);
        g.dispose();
        ImageIO.write(image, "png", file);
    }
}
