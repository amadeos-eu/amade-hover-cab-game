package eu.amadeos.games.hovercab.ui.game;

import eu.amadeos.commons.i18n.Language;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.audio.Speech;
import eu.amadeos.games.hovercab.bootstrap.HoverCabModule;
import eu.amadeos.games.hovercab.domain.Call;
import eu.amadeos.games.hovercab.domain.Shift;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoicesTest {
    @Test
    void theWordsAndThePhonemesComeFromTheMessages() {
        Voices voices = new Voices(Messages.load(HoverCabModule.class, HoverCabModule.MESSAGES, Locale.ENGLISH));
        assertEquals("Pad 3, please!", voices.text(Call.PAD, 3));
        assertEquals("p ae1 d / th r iy1 , p l iy1 z !", voices.phonemes(Call.PAD, 3));
        assertEquals("Hey, cab!", voices.text(Call.HEY, 0));
    }

    /** Every phrase in every language is made of phonemes the synthesizer knows, and sounds. */
    @Test
    void everyPhraseCanBeSpokenInEveryLanguage() {
        for (Language language : Language.values()) {
            Voices voices = new Voices(Messages.load(HoverCabModule.class, HoverCabModule.MESSAGES, language.locale()));
            for (Call call : Call.values()) {
                int pads = call == Call.PAD ? 9 : 1;
                for (int pad = 1; pad <= pads; pad++) {
                    String phonemes = voices.phonemes(call, pad);
                    double seconds = Speech.say(Speech.Voice.MAN, phonemes).seconds();
                    assertTrue(seconds > 0.15 && seconds < 3, language + " " + call + " " + pad + ": " + seconds);
                }
            }
        }
    }

    /** A shift's phrases are spoken ahead, in the background, before its passengers say them. */
    @Test
    void aShiftsPhrasesAreSpokenAhead() throws InterruptedException {
        Voices voices = new Voices(Messages.load(HoverCabModule.class, HoverCabModule.MESSAGES, Locale.ENGLISH));
        Shift shift = Shift.load(1);
        voices.prepare(shift);
        Shift.Fare first = shift.fares().getFirst();
        int kind = People.kind(0, shift.number());
        for (int i = 0; i < 300 && !voices.ready(kind, Call.PAD, first.to()); i++) Thread.sleep(100);
        assertTrue(voices.ready(kind, Call.HEY, 0));
        assertTrue(voices.ready(kind, Call.PAD, first.to()));
        assertFalse(voices.ready(kind, Call.UP, 0), "only the last passenger wants up");
    }
}
