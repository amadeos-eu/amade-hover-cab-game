package eu.amadeos.games.hovercab.ui.game;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.audio.Sound;
import eu.amadeos.gameengine.audio.Speech;
import eu.amadeos.gameengine.collision.PixelMask;
import eu.amadeos.gameengine.input.Gamepads;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.gameengine.tile.Cell;
import eu.amadeos.games.hovercab.bootstrap.HoverCabModule;
import eu.amadeos.games.hovercab.domain.Autopilot;
import eu.amadeos.games.hovercab.domain.Cab;
import eu.amadeos.games.hovercab.domain.CabShape;
import eu.amadeos.games.hovercab.domain.Career;
import eu.amadeos.games.hovercab.domain.Field;
import eu.amadeos.games.hovercab.domain.Game;
import eu.amadeos.games.hovercab.domain.Input;
import eu.amadeos.games.hovercab.domain.Plans;
import eu.amadeos.games.hovercab.domain.Rider;
import eu.amadeos.games.hovercab.repository.HoverCabSettings;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * The screenshots of chapter 28, drawn without a screen: the title, a fare in the harbour, what the
 * collisions see, the autopilot's plan, the sound of a call, the lift, the storm, the end of a shift and all
 * twelve shifts at a glance. {@code ./gradlew screenshots -Pshots=<folder>}.
 */
public final class Screenshots {
    private final long[] now = {0};
    private final Keyboard keyboard = new Keyboard(() -> now[0]);
    private final HoverCabGame game;

    private Screenshots() throws Exception {
        Messages msg = Messages.load(HoverCabModule.class, HoverCabModule.MESSAGES, Locale.ENGLISH);
        HoverCabSettings settings = new HoverCabSettings(Files.createTempDirectory("hovercab").resolve("hovercab.properties"));
        Mixer mixer = new Mixer();
        mixer.setMuted(true);
        game = new HoverCabGame(keyboard, Gamepads.none(), msg, settings, mixer, Runnable::run);
    }

    public static void main(String[] args) throws Exception {
        File dir = new File(args.length > 0 ? args[0] : "screenshots");
        dir.mkdirs();
        Screenshots shots = new Screenshots();
        shots.steps(9 * 120);
        shots.save(new File(dir, "title.png"));

        // Harbour Heights: a passenger has just got in and says where to.
        HoverCabGame.PlayScene harbour = shots.fly(3, g -> g.carried() == 1 && riding(g, 0.7));
        shots.save(new File(dir, "harbour.png"));
        shots.masks(new File(dir, "masks.png"), harbour.game());

        // The mine: the way the autopilot plans to the passenger's pad.
        HoverCabGame.PlayScene mine = shots.fly(5, g -> g.carried() == 2 && riding(g, 0.1));
        shots.plan(new File(dir, "plan.png"), mine.game());

        // The lift: the cab rides it up with a passenger.
        shots.fly(7, g -> g.carried() == 1 && g.rider().state() == Rider.State.BOARDING);
        shots.save(new File(dir, "lift.png"));

        // The storm: rain, gusts and the sea.
        shots.fly(11, g -> g.time() > 31);
        shots.save(new File(dir, "storm.png"));

        // A crash: down on a pad with the gear in.
        HoverCabGame.PlayScene crash = shots.fly(2, g -> g.time() > 2);
        Plans.put(crash.game(), 324, 200, 0, 30);
        crash.step(HoverCabGame.STEP, new Input(0, false, false, true));
        for (int i = 0; i < 120 * 4 && crash.game().phase() == Game.Phase.PLAYING; i++) {
            crash.step(HoverCabGame.STEP, Input.NONE);
        }
        shots.steps(40);
        shots.save(new File(dir, "crash.png"));

        // The end of a shift.
        shots.fly(1, g -> g.phase() != Game.Phase.PLAYING);
        shots.steps((int) ((HoverCabGame.CLEARED_SECONDS + 1) * 120));
        shots.save(new File(dir, "cleared.png"));

        voice(new File(dir, "voice.png"));
        shots.allShifts(new File(dir, "shifts.png"));
        System.exit(0);
    }

    private static boolean riding(Game game, double seconds) {
        Rider rider = game.rider();
        return rider != null && rider.state() == Rider.State.RIDING && rider.stateTime() >= seconds;
    }

    /** Shift {@code number}, flown by the autopilot until {@code until} holds, with all the effects. */
    private HoverCabGame.PlayScene fly(int number, java.util.function.Predicate<Game> until) {
        HoverCabGame.PlayScene play = game.play(number, new Career());
        Autopilot pilot = new Autopilot(play.game().shift());
        for (int i = 0; i < 120 * 300 && !until.test(play.game()); i++) {
            play.step(HoverCabGame.STEP, pilot.steer(play.game()));
        }
        return play;
    }

    /** The shift again, with what is solid for the collisions over it, and the cab's own pixels in red. */
    private void masks(File file, Game shown) throws Exception {
        write(file, g -> {
            game.render(g, 1);
            g.setColor(new Color(0, 0, 0, 120));
            g.fill(new Rectangle2D.Double(0, 0, Field.WIDTH, Field.HEIGHT));
            PixelMask mask = shown.shift().mask();
            g.setColor(new Color(110, 200, 255, 200));
            for (int y = 0; y < mask.height(); y++) {
                for (int x = 0; x < mask.width(); x++) if (mask.solid(x, y)) g.fill(new Rectangle2D.Double(x, y, 1, 1));
            }
            Cab cab = shown.cab();
            PixelMask own = CabShape.mask(cab.facingLeft(), cab.gearOut());
            int cx = (int) Math.round(cab.body().x), cy = (int) Math.round(cab.body().y);
            g.setColor(new Color(255, 80, 60));
            for (int y = 0; y < own.height(); y++) {
                for (int x = 0; x < own.width(); x++) if (own.solid(x, y)) g.fill(new Rectangle2D.Double(cx + x, cy + y, 1, 1));
            }
        });
    }

    /** The autopilot's grid in the mine: open cells as dots, and the way it plans as a line. */
    private void plan(File file, Game shown) throws Exception {
        Autopilot pilot = new Autopilot(shown.shift());
        List<Cell> way = Plans.way(pilot, shown);
        double half = Field.CAB_WIDTH / 2, middle = Field.CAB_HEIGHT / 2;
        write(file, g -> {
            game.render(g, 1);
            g.setColor(new Color(0, 0, 0, 110));
            g.fill(new Rectangle2D.Double(0, 0, Field.WIDTH, Field.HEIGHT));
            g.setColor(new Color(140, 230, 160, 150));
            for (int r = 0; r < Plans.rows(pilot); r++) {
                for (int c = 0; c < Plans.cols(pilot); c++) {
                    if (Plans.open(pilot, c, r) && (r + c) % 2 == 0) {
                        g.fill(new Ellipse2D.Double(c * Plans.CELL + half - 0.6, r * Plans.CELL + middle - 0.6, 1.2, 1.2));
                    }
                }
            }
            Path2D line = new Path2D.Double();
            for (int i = 0; i < way.size(); i++) {
                double x = way.get(i).col() * Plans.CELL + half, y = way.get(i).row() * Plans.CELL + middle;
                if (i == 0) line.moveTo(x, y);
                else line.lineTo(x, y);
            }
            g.setColor(new Color(255, 220, 90));
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(line);
            if (!way.isEmpty()) {
                Cell end = way.getLast();
                g.fill(new Ellipse2D.Double(end.col() * Plans.CELL + half - 3, end.row() * Plans.CELL + middle - 3, 6, 6));
            }
        });
    }

    /**
     * "Pad 3, please!" in the man's voice: a spectrogram, frequency upwards to 5000 Hz, time to the right.
     * The dark bands are the formants: they glide from vowel to vowel; the hiss of the s and z is high.
     */
    private static void voice(File file) throws Exception {
        String phrase = "p ae1 d / th r iy1 , p l iy1 z !";
        Sound sound = Speech.say(Speech.Voice.MAN, phrase);
        int window = 300, hop = 48, top = 5000;
        int frames = Math.max(1, (sound.length() - window) / hop);
        int bins = top * window / Sound.RATE;
        double[][] power = new double[frames][bins];
        double loudest = 1e-12;
        double[] hann = new double[window];
        for (int i = 0; i < window; i++) hann[i] = 0.5 - 0.5 * Math.cos(2 * Math.PI * i / (window - 1));
        for (int f = 0; f < frames; f++) {
            for (int k = 0; k < bins; k++) {
                double re = 0, im = 0, w = 2 * Math.PI * k / window;
                for (int i = 0; i < window; i++) {
                    double s = sound.sample(f * hop + i) * hann[i];
                    re += s * Math.cos(w * i);
                    im -= s * Math.sin(w * i);
                }
                power[f][k] = re * re + im * im;
                loudest = Math.max(loudest, power[f][k]);
            }
        }
        int width = 1280, height = 560, left = 70, bottom = 50;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0xF6F4EE));
        g.fillRect(0, 0, width, height);
        int plotW = width - left - 20, plotH = height - bottom - 20;
        for (int px = 0; px < plotW; px++) {
            int f = px * frames / plotW;
            for (int py = 0; py < plotH; py++) {
                int k = (plotH - 1 - py) * bins / plotH;
                double db = 10 * Math.log10(power[f][k] / loudest + 1e-12);
                float shade = (float) Math.clamp((db + 50) / 50, 0, 1);
                int c = (int) (246 - shade * 226);
                image.setRGB(left + px, 20 + py, new Color(c, c, Math.min(255, c + 12)).getRGB());
            }
        }
        g.setColor(new Color(0x30343E));
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        for (int hz = 0; hz <= top; hz += 1000) {
            int y = 20 + plotH - hz * plotH / top;
            g.drawLine(left - 6, y, left, y);
            g.drawString(hz + "", 8, y + 5);
        }
        double seconds = sound.seconds();
        for (double t = 0; t <= seconds; t += 0.25) {
            int x = left + (int) (t / seconds * plotW);
            g.drawLine(x, 20 + plotH, x, 26 + plotH);
            g.drawString(String.format(Locale.ROOT, "%.2f s", t), x - 18, 44 + plotH);
        }
        g.drawString("Hz", 8, 16);
        g.dispose();
        ImageIO.write(image, "png", file);
    }

    /** All twelve shifts at the start, half the size, four in a row. */
    private void allShifts(File file) throws Exception {
        int w = (int) Field.WIDTH, h = (int) Field.HEIGHT;
        BufferedImage sheet = new BufferedImage(w * 2, h * 3 / 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D sg = sheet.createGraphics();
        sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        for (int n = 1; n <= 12; n++) {
            fly(n, g -> g.time() > 6);
            BufferedImage one = picture(1, g -> {
                game.render(g, 1);
                game.renderOverlay(g, 1);
            });
            sg.drawImage(one, ((n - 1) % 4) * w / 2, ((n - 1) / 4) * h / 2, w / 2, h / 2, null);
        }
        sg.dispose();
        ImageIO.write(sheet, "png", file);
    }

    private void steps(int n) {
        for (int i = 0; i < n; i++) {
            now[0] += (long) (HoverCabGame.STEP * 1e9);
            game.update(HoverCabGame.STEP);
        }
    }

    /** World and overlay at twice the logical size. */
    private void save(File file) throws Exception {
        write(file, g -> {
            game.render(g, 1);
            game.renderOverlay(g, 1);
        });
    }

    private static void write(File file, Consumer<Graphics2D> draw) throws Exception {
        ImageIO.write(picture(2, draw), "png", file);
    }

    private static BufferedImage picture(int scale, Consumer<Graphics2D> draw) {
        BufferedImage image = new BufferedImage((int) Field.WIDTH * scale, (int) Field.HEIGHT * scale,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.scale(scale, scale);
        draw.accept(g);
        g.dispose();
        return image;
    }
}
