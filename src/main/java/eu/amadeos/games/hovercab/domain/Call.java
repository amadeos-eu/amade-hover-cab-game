package eu.amadeos.games.hovercab.domain;

/** What a passenger says. The words, and the voice saying them, are the user interface's business. */
public enum Call {
    /** Waiting on a pad: "Hey, cab!" */
    HEY,
    /** Waiting a long time: "Taxi!" */
    TAXI,
    /** Aboard, naming the pad: "Pad 3, please!" */
    PAD,
    /** The last passenger of a shift: "Up, please!" */
    UP,
    /** A hard landing: "Ouch!" */
    OUCH,
    /** Arrived with some tip left: "Thanks!" */
    THANKS
}
