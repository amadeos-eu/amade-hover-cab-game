package eu.amadeos.games.hovercab.domain;

/**
 * What lasts from shift to shift: the money in the till, the money earned in all (a spare cab for every
 * hundred dollars of it), and the spare cabs left. In cents.
 */
public final class Career {
    long cents;
    long earned;
    int spare = Field.SPARE_CABS;
    /** Parts of a cent of fuel not yet taken from the till. */
    private double owed;

    /** Takes in a fare and a tip; true if that earned a spare cab. */
    boolean earn(int amount) {
        long before = earned / Field.BONUS_CAB_EVERY;
        cents += amount;
        earned += amount;
        if (earned / Field.BONUS_CAB_EVERY > before) {
            spare++;
            return true;
        }
        return false;
    }

    /** Pays for fuel, as far as the till goes; a cab with an empty till gets its fuel on credit it never pays. */
    void pay(double amount) {
        owed += amount;
        long whole = (long) owed;
        owed -= whole;
        cents = Math.max(0, cents - whole);
    }

    public long cents() {
        return cents;
    }

    public long earned() {
        return earned;
    }

    public int spare() {
        return spare;
    }
}
