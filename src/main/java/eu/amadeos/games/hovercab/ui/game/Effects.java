package eu.amadeos.games.hovercab.ui.game;

import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.fx.Particles;
import eu.amadeos.gameengine.physics.Body;
import eu.amadeos.gameengine.physics.Touchdown;
import eu.amadeos.gameengine.random.SeededRandom;
import eu.amadeos.games.hovercab.domain.Cab;
import eu.amadeos.games.hovercab.domain.Call;
import eu.amadeos.games.hovercab.domain.Event;
import eu.amadeos.games.hovercab.domain.Field;
import eu.amadeos.games.hovercab.domain.Game;
import eu.amadeos.games.hovercab.domain.Rider;
import eu.amadeos.games.hovercab.domain.Shift;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/**
 * What the events of a step look and sound like: the exhaust of the jets, dust and sparks under a landing,
 * the fire of a crash, a splash, the money rising from a paid fare, the speech bubbles and the voices. The
 * rules know nothing of this.
 */
final class Effects {
    /** Exhaust, fire, smoke, water, sparks, white. */
    static final Color[] PALETTE = {new Color(0xFFD070), new Color(0xFF6A2A), new Color(0x6A6A78),
            new Color(0x8AC8FF), new Color(0xFFF6C0), new Color(0xF4F4F0)};
    static final int EXHAUST = 0, FIRE = 1, SMOKE = 2, WATER = 3, SPARK = 4, WHITE = 5;
    static final double BUBBLE_SECONDS = 2.2;
    static final double MONEY_SECONDS = 1.6;

    /** A speech bubble: who says it, and the words. */
    record Bubble(Rider rider, String text, double born) {
    }

    /** Money rising from where it was paid. */
    record Money(String text, double x, double y, double born) {
    }

    private final SoundBank sounds = new SoundBank();
    private final Music music = new Music();
    private final Mixer mixer;
    private final Voices voices;
    private final SeededRandom random = new SeededRandom(3);
    private final Particles particles = new Particles(600, PALETTE);
    private final List<Bubble> bubbles = new ArrayList<>();
    private final List<Money> money = new ArrayList<>();
    private double time;
    private double nextPuff;
    private double nextFuel;
    private double shake;

    Effects(Mixer mixer, Voices voices) {
        this.mixer = mixer;
        this.voices = voices;
    }

    double time() {
        return time;
    }

    /** A new shift, or the title: nothing left from before. */
    void clear() {
        particles.clear();
        bubbles.clear();
        money.clear();
        shake = 0;
        mixer.stopMusic();
    }

    /** The voices of a shift's passengers, made ahead in the background. */
    void prepare(Shift shift) {
        voices.prepare(shift);
    }

    void startTune() {
        mixer.playMusic(music.start, false);
    }

    void tick() {
        mixer.play(sounds.tick);
    }

    /** How far the picture shakes now, after a crash or a hard landing, in units. */
    double shake() {
        return shake;
    }

    // --8<-- [start:react]
    /** Shows and, if {@code audible}, plays what happened in the last step. */
    void react(Game game, boolean audible) {
        int shift = game.shift().number();
        for (Event event : game.events()) {
            switch (event) {
                case Event.GearMoved(boolean down) -> play(audible, down ? sounds.gearOut : sounds.gearIn);
                case Event.Landed(double x, double y, Touchdown touchdown, var pad) -> {
                    dust(x, y, touchdown == Touchdown.HARD ? 14 : 6);
                    if (touchdown == Touchdown.HARD) shake = 2.5;
                    play(audible, touchdown == Touchdown.HARD ? sounds.hardLanding : sounds.land);
                }
                case Event.TookOff(double x, double y) -> dust(x, y, 4);
                case Event.Spoke(Rider rider, Call call, int pad) -> {
                    bubbles.removeIf(b -> b.rider() == rider);
                    bubbles.add(new Bubble(rider, voices.text(call, pad), time));
                    if (audible) mixer.play(voices.sound(People.kind(rider, shift), call, pad));
                }
                case Event.Boarded boarded -> {
                }
                case Event.Paid(Rider rider, int fare, int tip, double x, double y) -> {
                    money.add(new Money(Hud.dollars(fare + tip), x, y, time));
                    play(audible, sounds.pay);
                }
                case Event.GateOpened opened -> play(audible, sounds.gate);
                case Event.FuelLow low -> play(audible, sounds.lowFuel);
                case Event.BonusCab bonus -> {
                    if (audible) mixer.playMusic(music.bonus, false);
                }
                case Event.Crashed(double x, double y, Event.Cause cause) -> {
                    if (cause == Event.Cause.WATER) {
                        burst(x, y + 6, 30, WATER, 110, -80);
                        play(audible, sounds.splash);
                    } else {
                        burst(x, y, 40, FIRE, 120, -30);
                        burst(x, y, 24, SMOKE, 60, -40);
                        burst(x, y, 16, SPARK, 160, -60);
                        play(audible, sounds.crash);
                    }
                    shake = 5;
                    bubbles.clear();
                }
                case Event.Respawned(double x, double y) -> {
                    burst(x, y - 8, 14, WHITE, 50, -20);
                    play(audible, sounds.respawn);
                }
                case Event.Cleared cleared -> {
                    if (audible) mixer.playMusic(music.cleared, false);
                }
                case Event.GameOver over -> {
                    if (audible) mixer.playMusic(music.over, false);
                }
            }
        }
    }
    // --8<-- [end:react]

    private void play(boolean audible, eu.amadeos.gameengine.audio.Sound sound) {
        if (audible) mixer.play(sound);
    }

    // --8<-- [start:exhaust]
    /** Smoke from the jets that fire, their hum, and the gurgle of fuel. Every step. */
    void update(double seconds, Game game, boolean audible) {
        time += seconds;
        shake = Math.max(0, shake - seconds * 8);
        particles.update(seconds, 20);
        bubbles.removeIf(b -> time - b.born() > BUBBLE_SECONDS);
        money.removeIf(m -> time - m.born() > MONEY_SECONDS);
        Cab cab = game.cab();
        if (game.phase() == Game.Phase.CRASHED) {
            if (random.nextInt(3) == 0) {
                Body body = cab.body();
                particles.spawn(body.centerX() + random.nextDouble() * 10 - 5, body.centerY(), random.nextDouble() * 10 - 5,
                        -30 - random.nextDouble() * 20, 0.8, 2 + random.nextDouble() * 2, SMOKE);
            }
            return;
        }
        if (game.phase() != Game.Phase.PLAYING) return;
        Body body = cab.body();
        if (cab.jetUp()) {
            particles.spawn(body.centerX() + random.nextDouble() * 4 - 2, body.y + Field.HULL_HEIGHT + 8,
                    body.vx * 0.3 + random.nextDouble() * 16 - 8, body.vy + 60 + random.nextDouble() * 40,
                    0.25 + random.nextDouble() * 0.15, 1 + random.nextDouble() * 1.5, random.nextInt(4) == 0 ? SMOKE : EXHAUST);
        }
        if (cab.jetSide() != 0) {
            double back = cab.jetSide() > 0 ? body.x : body.right();
            particles.spawn(back, body.y + 8, -cab.jetSide() * (50 + random.nextDouble() * 30) + body.vx * 0.3,
                    random.nextDouble() * 10 - 5, 0.2, 1 + random.nextDouble(), EXHAUST);
        }
        nextPuff -= seconds;
        if (nextPuff <= 0 && (cab.jetUp() || cab.jetSide() != 0 || cab.jetDown())) {
            play(audible, cab.jetUp() ? sounds.jet : sounds.puff);
            nextPuff = cab.jetUp() ? 0.1 : 0.09;
        }
        nextFuel -= seconds;
        if (cab.refuelling() && nextFuel <= 0) {
            play(audible, sounds.fuel);
            nextFuel = 0.3;
        }
    }
    // --8<-- [end:exhaust]

    private void dust(double x, double y, int count) {
        for (int i = 0; i < count; i++) {
            double side = i % 2 == 0 ? -1 : 1;
            particles.spawn(x + side * 10, y - 1, side * (20 + random.nextDouble() * 40), -5 - random.nextDouble() * 20,
                    0.3 + random.nextDouble() * 0.2, 1 + random.nextDouble() * 1.5, i % 3 == 0 ? SPARK : SMOKE);
        }
    }

    private void burst(double x, double y, int count, int colour, double speed, double lift) {
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double v = speed * (0.3 + random.nextDouble() * 0.7);
            particles.spawn(x, y, Math.cos(angle) * v, Math.sin(angle) * v + lift, 0.5 + random.nextDouble() * 0.6,
                    1.5 + random.nextDouble() * 2, colour);
        }
    }

    void render(Graphics2D g, double alpha) {
        particles.render(g, alpha);
    }

    List<Bubble> bubbles() {
        return bubbles;
    }

    List<Money> money() {
        return money;
    }

    int particles() {
        return particles.count();
    }
}
