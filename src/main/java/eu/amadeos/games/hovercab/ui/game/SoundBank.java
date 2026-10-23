package eu.amadeos.games.hovercab.ui.game;

import eu.amadeos.gameengine.audio.Sound;
import eu.amadeos.gameengine.audio.Sound.Wave;
import eu.amadeos.gameengine.audio.Tune;

/** The sounds of Hover Cab, made once at start from tones: no recordings. The voices are {@link Voices}. */
final class SoundBank {
    /** One puff of the main jet: low noise, played again and again while it fires. */
    final Sound jet = Sound.mix(Sound.tone(Wave.NOISE, 0, 0, 0.11, 0.05), Sound.tone(Wave.TRIANGLE, 70, 64, 0.11, 0.12));
    /** A puff of a side jet or the roof jet: higher and softer. */
    final Sound puff = Sound.tone(Wave.NOISE, 0, 0, 0.09, 0.025);
    final Sound gearOut = Sound.tone(Wave.SQUARE, 220, 330, 0.25, 0.04);
    final Sound gearIn = Sound.tone(Wave.SQUARE, 330, 220, 0.25, 0.04);
    final Sound land = Sound.mix(Sound.tone(Wave.TRIANGLE, 140, 60, 0.12, 0.3), Sound.tone(Wave.NOISE, 0, 0, 0.04, 0.05));
    final Sound hardLanding = Sound.mix(Sound.tone(Wave.SQUARE, 180, 70, 0.18, 0.08), Sound.tone(Wave.NOISE, 0, 0, 0.12, 0.12));
    final Sound crash = Sound.mix(Sound.tone(Wave.NOISE, 0, 0, 0.9, 0.22), Sound.tone(Wave.TRIANGLE, 120, 30, 0.8, 0.35));
    final Sound splash = Sound.mix(Sound.tone(Wave.NOISE, 0, 0, 0.5, 0.14), Sound.tone(Wave.TRIANGLE, 300, 80, 0.4, 0.2));
    /** The till: two high notes and a rattle. */
    final Sound pay = Sound.tone(Wave.SQUARE, 1568, 1568, 0.06, 0.05)
            .then(Sound.tone(Wave.SQUARE, 2093, 2093, 0.2, 0.05));
    final Sound fuel = Sound.tone(Wave.TRIANGLE, 500, 700, 0.08, 0.12);
    final Sound lowFuel = Sound.tone(Wave.SQUARE, 880, 880, 0.08, 0.06).then(Sound.tone(Wave.SQUARE, 0, 0, 0.06, 0))
            .then(Sound.tone(Wave.SQUARE, 880, 880, 0.08, 0.06));
    final Sound gate = Tune.voice(Wave.TRIANGLE, 0.25, "t180 o5 l16 c e g >c");
    final Sound respawn = Sound.tone(Wave.TRIANGLE, 200, 800, 0.3, 0.25);
    final Sound tick = Sound.tone(Wave.SQUARE, 880, 880, 0.03, 0.05);
}
