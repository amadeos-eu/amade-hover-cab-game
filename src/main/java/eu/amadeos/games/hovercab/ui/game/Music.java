package eu.amadeos.games.hovercab.ui.game;

import eu.amadeos.gameengine.audio.Sound;
import eu.amadeos.gameengine.audio.Sound.Wave;
import eu.amadeos.gameengine.audio.Tune;

/**
 * The tunes of Hover Cab, in MML: a short swing at the start of each shift, a fanfare at its end, a sad one
 * when the last cab is lost. While flying it stays quiet: the passengers must be heard.
 */
final class Music {
    final Sound start = Sound.mix(
            Tune.voice(Wave.SQUARE, 0.06, "t150 o5 l8 c e g a- g e c4 | d f a b- a f d4 | e g >c< g e2"),
            Tune.voice(Wave.TRIANGLE, 0.2, "t150 o3 l4 c g c g | d a d a | c g c2"));

    final Sound cleared = Sound.mix(
            Tune.voice(Wave.SQUARE, 0.07, "t170 o5 l8 g >c e g4 e g2"),
            Tune.voice(Wave.TRIANGLE, 0.22, "t170 o3 l8 c g >c4 < g >c2"));

    final Sound over = Sound.mix(
            Tune.voice(Wave.SQUARE, 0.06, "t110 o4 l4 g f e d | c2. r4"),
            Tune.voice(Wave.TRIANGLE, 0.2, "t110 o2 l2 g e | c1"));

    final Sound bonus = Tune.voice(Wave.SQUARE, 0.06, "t200 o5 l16 c e g >c< g >c e g");
}
