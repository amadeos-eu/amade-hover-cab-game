package eu.amadeos.games.hovercab.ui.game;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.audio.Sound;
import eu.amadeos.gameengine.audio.Speech;
import eu.amadeos.games.hovercab.domain.Call;
import eu.amadeos.games.hovercab.domain.Shift;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * What the passengers say, in words for the speech bubble and in sound. The words and the phonemes both come
 * from the message files, so a passenger in the German game calls "He, Taxi!" and sounds like it. Each
 * phrase is spoken by {@link Speech} in the voice of the passenger's kind and kept. Speaking a phrase takes
 * longer than a step of the game, so {@link #prepare} speaks a shift's phrases ahead, on a thread of its own.
 */
// --8<-- [start:voices]
final class Voices {
    private final Messages msg;
    private final Map<String, Sound> spoken = new ConcurrentHashMap<>();
    private final ExecutorService ahead = Executors.newSingleThreadExecutor(Thread.ofPlatform().daemon()
            .priority(Thread.MIN_PRIORITY).name("voices").factory());

    Voices(Messages msg) {
        this.msg = msg;
    }

    /** The words of a call, for the bubble: "Pad 3, please!" */
    String text(Call call, int pad) {
        String key = "say." + call.name().toLowerCase(Locale.ROOT);
        return call == Call.PAD ? msg.format(key, pad) : msg.get(key);
    }

    /** The phonemes of a call: "p ae1 d / th r iy1 , p l iy1 z !" */
    String phonemes(Call call, int pad) {
        String key = "voice." + call.name().toLowerCase(Locale.ROOT);
        return call == Call.PAD ? msg.format(key, msg.get("voice.number." + pad)) : msg.get(key);
    }

    /** Speaks, in the background, everything the passengers of a shift may say, in the order they come. */
    void prepare(Shift shift) {
        for (int i = 0; i < shift.fares().size(); i++) {
            int kind = People.kind(i, shift.number());
            int to = shift.fares().get(i).to();
            for (Call call : Call.values()) {
                if (call == Call.PAD && to == Shift.UP || call == Call.UP && to != Shift.UP) continue;
                ahead.execute(() -> sound(kind, call, to));
            }
        }
    }

    /** The call spoken by a passenger of {@code kind}; made now if it is not ready yet. */
    Sound sound(int kind, Call call, int pad) {
        return spoken.computeIfAbsent(key(kind, call, pad), k -> Speech.say(People.VOICES[kind], phonemes(call, pad), 0.55));
    }

    /** Whether the call is spoken already. */
    boolean ready(int kind, Call call, int pad) {
        return spoken.containsKey(key(kind, call, pad));
    }

    private static String key(int kind, Call call, int pad) {
        return kind + ":" + call + ":" + (call == Call.PAD ? pad : 0);
    }
}
// --8<-- [end:voices]
