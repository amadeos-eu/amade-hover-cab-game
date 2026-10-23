package eu.amadeos.games.hovercab.bootstrap;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import eu.amadeos.commons.app.AppInfo;
import eu.amadeos.commons.config.AppDirectories;
import eu.amadeos.commons.config.XdgDirectories;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.input.Gamepads;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.gameengine.score.HighScores;
import eu.amadeos.gameengine.score.PlayerName;
import eu.amadeos.gameengine.score.Ranking;
import eu.amadeos.games.hovercab.repository.HoverCabSettings;
import eu.amadeos.games.hovercab.ui.game.Arena;
import eu.amadeos.games.hovercab.ui.game.HoverCabGame;
import eu.amadeos.games.hovercab.ui.main.HoverCabPresenter;
import eu.amadeos.games.hovercab.ui.main.HoverCabView;
import eu.amadeos.games.hovercab.ui.main.HoverCabWindow;

import java.util.Locale;

/**
 * amade-hover-cab-game bindings. Theme, language and the XDG directories come from
 * {@link eu.amadeos.commons.app.CommonsModule}.
 */
public final class HoverCabModule extends AbstractModule {
    /** The texts, {@code messages.properties} and its translations; also the passengers' phrases as phonemes. */
    public static final String MESSAGES = "eu.amadeos.games.hovercab.messages";

    @Override
    protected void configure() {
        bind(HoverCabSettings.class).in(Scopes.SINGLETON);
        bind(Keyboard.class).in(Scopes.SINGLETON);
        bind(Mixer.class).in(Scopes.SINGLETON);
        bind(HoverCabGame.class).in(Scopes.SINGLETON);
        bind(Arena.class).to(HoverCabGame.class);
        bind(HoverCabWindow.class).in(Scopes.SINGLETON);
        bind(HoverCabView.class).to(HoverCabWindow.class);
        bind(HoverCabPresenter.class).in(Scopes.SINGLETON);
    }

    /** The best earnings in {@code ~/.local/share/amadeos/games/hovercab}. */
    @Provides
    @Singleton
    HighScores highScores(XdgDirectories xdg, AppInfo app) {
        return HighScores.of(xdg, app.configName(), Ranking.HIGHER_WINS);
    }

    @Provides
    @Singleton
    PlayerName playerName(AppDirectories directories) {
        return new PlayerName(directories.config(), System.getProperty("user.name"));
    }

    /** The gamepads of this computer, by the backend of its operating system. */
    @Provides
    @Singleton
    Gamepads gamepads() {
        return Gamepads.open();
    }

    /** The texts in the language {@code AmadeApplication} chose. */
    @Provides
    @Singleton
    Messages messages(Locale locale) {
        return Messages.load(HoverCabModule.class, MESSAGES, locale);
    }
}
