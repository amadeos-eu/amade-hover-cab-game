package eu.amadeos.games.hovercab.bootstrap;

import eu.amadeos.commons.app.AmadeApplication;
import eu.amadeos.commons.app.AppInfo;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.input.Gamepads;
import eu.amadeos.games.hovercab.ui.game.HoverCabGame;
import eu.amadeos.games.hovercab.ui.main.HoverCabPresenter;
import eu.amadeos.games.hovercab.ui.main.HoverCabWindow;

public final class AmadeHoverCabApplication {
    public static final AppInfo APP = new AppInfo("amade-hover-cab-game", "Hover Cab", "hovercab");

    private AmadeHoverCabApplication() {
    }

    /** The version from the jar's manifest; "dev" when run from the IDE. */
    public static String version() {
        String version = AmadeHoverCabApplication.class.getPackage().getImplementationVersion();
        return version == null ? "dev" : version;
    }

    // --8<-- [start:main]
    public static void main(String[] args) {
        AmadeApplication.launch(APP, injector -> {
            Mixer mixer = injector.getInstance(Mixer.class);
            mixer.start();
            Gamepads gamepads = injector.getInstance(Gamepads.class);
            HoverCabWindow window = injector.getInstance(HoverCabWindow.class);
            HoverCabPresenter presenter = injector.getInstance(HoverCabPresenter.class);
            HoverCabGame game = injector.getInstance(HoverCabGame.class);
            window.bind(presenter);
            presenter.start();
            window.whenClosed(() -> {
                game.stop();
                gamepads.close();
                mixer.close();
                System.exit(0);
            });
            window.setLocationByPlatform(true);
            window.setVisible(true);
            game.start(window.canvas());
        }, new HoverCabModule());
    }
    // --8<-- [end:main]
}
